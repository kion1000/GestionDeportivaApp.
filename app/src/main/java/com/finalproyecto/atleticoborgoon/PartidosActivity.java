package com.finalproyecto.atleticoborgoon;
import com.finalproyecto.atleticoborgoon.data.DBHelper;



import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// Edge-to-edge + insets
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * NOTAS de integración:
 * 1) Asegúrate de que la tabla Partidos tiene la columna 'encuentro_id INTEGER'.
 * 2) En el layout dialog_add_partido.xml debe existir un Spinner con id 'spEncuentro'.
 * 3) Añade en tu clase modelo 'Partido' el campo 'int encuentroId'. (Opcional: 'String rivalNombre')
 */
public class PartidosActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private int jugadorId;
    private ListView listView;
    private ArrayAdapter<Partido> adapter;
    private ArrayList<Partido> datos = new ArrayList<>();
    private Partido partidoSeleccionado;

    // Cache de encuentros para el spinner
    private static class EncuentroItem {
        int id;
        String label; // p.ej. "2025-10-05 · vs. San José · 2-1"
        @Override public String toString() { return label; }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ==== EDGE-TO-EDGE + INSETS ====
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_partidos);
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
        // ================================

        dbHelper = new DBHelper(this);

        jugadorId = getIntent().getIntExtra("jugador_id", -1);
        listView = findViewById(R.id.listPartidos);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, datos);
        listView.setAdapter(adapter);

        cargar();

        findViewById(R.id.btnAddPartido).setOnClickListener(v -> dialogAñadir());

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            partidoSeleccionado = datos.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Acción")
                    .setItems(new String[]{"Editar", "Borrar"}, (d, which) -> {
                        if (which == 0) dialogEditar(partidoSeleccionado);
                        else confirmarBorrado(partidoSeleccionado);
                    })
                    .show();
            return true;
        });
    }

    private void cargar() {
        datos.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Intentamos traer también encuentro_id y, si quieres, nombre del rival
        // LEFT JOIN para no romper si aún no hay enlace.
        Cursor c = db.rawQuery(
                "SELECT p.id, p.jugador_id, p.goles, p.asistencias, p.tarjetas, p.fecha, p.encuentro_id, " +
                        "       e.fecha AS e_fecha, r.nombre AS rival, e.goles_favor, e.goles_contra " +
                        "FROM Partidos p " +
                        "LEFT JOIN Encuentros e ON e.id = p.encuentro_id " +
                        "LEFT JOIN Equipos r ON r.id = e.rival_id " +
                        "WHERE p.jugador_id=? " +
                        "ORDER BY COALESCE(e.fecha, p.fecha) DESC, p.id DESC",
                new String[]{ String.valueOf(jugadorId) });

        while (c.moveToNext()) {
            Partido p = new Partido();
            p.id = c.getInt(0);
            p.jugadorId = c.getInt(1);
            p.goles = c.getInt(2);
            p.asistencias = c.getInt(3);
            p.tarjetas = c.getString(4);
            p.fecha = c.getString(5);            // fecha antigua (seguimos mostrándola por compatibilidad)
            p.encuentroId = c.isNull(6) ? 0 : c.getInt(6); // NUEVO

            // Opcional: si tu Partido tiene campo rivalNombre/descripcion, puedes formatear algo legible.
            // String rival = c.getString(8);
            // int gf = c.getInt(9), gc = c.getInt(10);
            // p.rivalNombre = rival; // si lo agregas al modelo

            datos.add(p);
        }
        c.close();
        adapter.notifyDataSetChanged();
    }

    private void dialogAñadir() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_partido, null);
        EditText etGoles = view.findViewById(R.id.etGoles);
        EditText etAsist = view.findViewById(R.id.etAsist);
        Spinner spTarjetas = view.findViewById(R.id.spTarjetas);
        EditText etFecha = view.findViewById(R.id.etFecha);

        // === NUEVO: Spinner de Encuentro ===
        Spinner spEncuentro = view.findViewById(R.id.spEncuentro);
        ArrayList<EncuentroItem> encuentros = cargarEncuentros();

        if (encuentros.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("No hay encuentros")
                    .setMessage("Primero crea un encuentro del equipo (rival y fecha).")
                    .setPositiveButton("Crear encuentro", (d, w) -> {
                        startActivity(new Intent(this, com.finalproyecto.atleticoborgoon.ui.EncuentrosActivity.class));
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        ArrayAdapter<EncuentroItem> encAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, encuentros);
        spEncuentro.setAdapter(encAdapter);

        spTarjetas.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ninguna", "amarilla", "roja"}));

        etFecha.setOnClickListener(v -> pickFecha(etFecha));

        new AlertDialog.Builder(this)
                .setTitle("Añadir partido (estadística del jugador)")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String fecha = etFecha.getText().toString().trim();
                    int goles = parseIntSafe(etGoles.getText().toString());
                    int asist = parseIntSafe(etAsist.getText().toString());
                    String tarjetas = spTarjetas.getSelectedItem().toString();

                    EncuentroItem sel = (EncuentroItem) spEncuentro.getSelectedItem();
                    if (sel == null) {
                        Toast.makeText(this, "Selecciona un encuentro", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    goles = Math.max(0, goles);
                    asist = Math.max(0, asist);

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("jugador_id", jugadorId);
                    cv.put("goles", goles);
                    cv.put("asistencias", asist);
                    cv.put("tarjetas", tarjetas);
                    cv.put("fecha", fecha.isEmpty() ? null : fecha); // opcional
                    cv.put("encuentro_id", sel.id);                  // enlace clave
                    db.insert("Partidos", null, cv);

                    cargar();
                    Toast.makeText(PartidosActivity.this, "Estadística añadida", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private void dialogEditar(Partido p) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_partido, null);
        EditText etGoles = view.findViewById(R.id.etGoles);
        EditText etAsist = view.findViewById(R.id.etAsist);
        Spinner spTarjetas = view.findViewById(R.id.spTarjetas);
        EditText etFecha = view.findViewById(R.id.etFecha);

        // === NUEVO: Spinner de Encuentro ===
        Spinner spEncuentro = view.findViewById(R.id.spEncuentro);
        ArrayList<EncuentroItem> encuentros = cargarEncuentros();
        ArrayAdapter<EncuentroItem> encAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, encuentros);
        spEncuentro.setAdapter(encAdapter);

        // Cargar datos actuales
        etGoles.setText(String.valueOf(p.goles));
        etAsist.setText(String.valueOf(p.asistencias));
        etFecha.setText(p.fecha != null ? p.fecha : "");

        spTarjetas.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ninguna", "amarilla", "roja"}));

        // Selección inicial de tarjetas
        for (int i = 0; i < spTarjetas.getCount(); i++) {
            if (spTarjetas.getItemAtPosition(i).toString().equalsIgnoreCase(p.tarjetas)) {
                spTarjetas.setSelection(i);
                break;
            }
        }

        // Selección inicial de encuentro
        if (p.encuentroId > 0) {
            for (int i = 0; i < encuentros.size(); i++) {
                if (encuentros.get(i).id == p.encuentroId) {
                    spEncuentro.setSelection(i);
                    break;
                }
            }
        }

        etFecha.setOnClickListener(v -> pickFecha(etFecha));

        new AlertDialog.Builder(this)
                .setTitle("Editar estadística")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String fecha = etFecha.getText().toString().trim();
                    int goles = parseIntSafe(etGoles.getText().toString());
                    int asist = parseIntSafe(etAsist.getText().toString());
                    String tarjetas = spTarjetas.getSelectedItem().toString();

                    EncuentroItem sel = (EncuentroItem) spEncuentro.getSelectedItem();
                    if (sel == null) {
                        Toast.makeText(this, "Selecciona un encuentro", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    goles = Math.max(0, goles);
                    asist = Math.max(0, asist);

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("goles", goles);
                    cv.put("asistencias", asist);
                    cv.put("tarjetas", tarjetas);
                    cv.put("fecha", fecha.isEmpty() ? null : fecha);
                    cv.put("encuentro_id", sel.id);
                    db.update("Partidos", cv, "id=?", new String[]{ String.valueOf(p.id) });

                    cargar();
                    Toast.makeText(PartidosActivity.this, "Estadística actualizada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarBorrado(Partido p) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar estadística")
                .setMessage("¿Seguro que quieres borrar esta estadística?")
                .setPositiveButton("Sí", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("Partidos", "id=?", new String[]{ String.valueOf(p.id) });
                    cargar();
                    Toast.makeText(PartidosActivity.this, "Eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // ==== HELPERS ====

    private ArrayList<EncuentroItem> cargarEncuentros() {
        ArrayList<EncuentroItem> out = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.id, e.fecha, r.nombre, e.goles_favor, e.goles_contra " +
                        "FROM Encuentros e JOIN Equipos r ON r.id=e.rival_id " +
                        "ORDER BY e.fecha DESC, e.id DESC", null);
        try {
            while (c.moveToNext()) {
                EncuentroItem it = new EncuentroItem();
                it.id = c.getInt(0);
                String fecha = c.getString(1);
                String rival = c.getString(2);
                int gf = c.getInt(3), gc = c.getInt(4);
                it.label = fecha + " · vs. " + rival + " · " + gf + "-" + gc;
                out.add(it);
            }
        } finally { c.close(); }
        return out;
    }

    private void pickFecha(EditText target) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, y, m, d) -> target.setText(String.format("%04d-%02d-%02d", y, m + 1, d)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
