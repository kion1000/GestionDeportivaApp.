package com.finalproyecto.atleticoborgoon;
import com.finalproyecto.atleticoborgoon.data.DBHelper;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

// ==== Edge-to-edge + insets ====
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
// ===============================

public class CuotasActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private int jugadorId;
    private ListView listView;
    private ArrayAdapter<Cuota> adapter;
    private final ArrayList<Cuota> datos = new ArrayList<>();
    private Cuota cuotaSeleccionada;

    // Soporte para Cobros (conceptos)
    private static class CobroItem {
        int id;
        String label; // "Seguro (2025/26) — 50.0€"
        String nombre;
        @Override public String toString() { return label; }
    }
    private boolean modoCobros = false; // true si hay tabla Cobros
    private ArrayList<CobroItem> cacheCobros = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---- EDGE-TO-EDGE + INSETS (Opción A) ----
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cuotas);
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
        // ------------------------------------------

        dbHelper  = new DBHelper(this);
        jugadorId = getIntent().getIntExtra("jugador_id", -1);
        listView  = findViewById(R.id.listCuotas);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, datos);
        listView.setAdapter(adapter);

        // Detecta si hay tabla Cobros → modo conceptos
        modoCobros = existeTabla("Cobros");
        if (modoCobros) cargarCobrosCache();

        cargar();

        findViewById(R.id.btnAddCuota).setOnClickListener(v -> dialogAñadir());

        // Long-press: Editar / Borrar
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            cuotaSeleccionada = datos.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Acción")
                    .setItems(new String[]{"Editar", "Borrar"}, (d, which) -> {
                        if (which == 0) dialogEditar(cuotaSeleccionada);  // Editar
                        else confirmarBorrado(cuotaSeleccionada);         // Borrar
                    })
                    .show();
            return true;
        });
    }

    /** Recarga la lista desde SQLite */
    private void cargar() {
        datos.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT q.id, q.jugador_id, " +
                        "       COALESCE(q.tipo, c.nombre) AS tipo_mostrar, " + // <- evita null
                        "       q.cantidad, q.fecha " +
                        "FROM Cuotas q " +
                        "LEFT JOIN Cobros c ON c.id = q.cobro_id " +
                        "WHERE q.jugador_id = ? " +
                        "ORDER BY " +
                        "  CASE WHEN q.fecha IS NULL OR q.fecha='' THEN 1 ELSE 0 END, " + // fechas vacías al final
                        "  q.fecha DESC, q.id DESC";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(jugadorId) });
        try {
            while (c.moveToNext()) {
                Cuota q = new Cuota();
                q.id        = c.getInt(0);
                q.jugadorId = c.getInt(1);
                q.tipo      = c.getString(2);     // tipo_mostrar (nunca null si hay cobro_id)
                q.cantidad  = c.getDouble(3);
                q.fecha     = c.getString(4);
                datos.add(q);
            }
        } finally {
            c.close();
        }

        adapter.notifyDataSetChanged();
    }


    /** Diálogo para añadir nueva cuota */
    private void dialogAñadir() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_cuota, null);
        Spinner spTipo      = view.findViewById(R.id.spTipo);
        EditText etCantidad = view.findViewById(R.id.etCantidad);
        EditText etFecha    = view.findViewById(R.id.etFecha);

        if (modoCobros && !cacheCobros.isEmpty()) {
            ArrayAdapter<CobroItem> ad = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, cacheCobros);
            spTipo.setAdapter(ad);
        } else {
            spTipo.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"Seguro", "1ª Cuota", "2ª Cuota", "3ª Cuota"}));
        }

        etFecha.setOnClickListener(v -> pickFecha(etFecha));

        new AlertDialog.Builder(this)
                .setTitle("Añadir cuota")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String fecha   = etFecha.getText().toString().trim();
                    double cantidad= parseDoubleSafe(etCantidad.getText().toString());

                    if (fecha.isEmpty()) {
                        Toast.makeText(this, "Selecciona fecha", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("jugador_id", jugadorId);
                    cv.put("cantidad", cantidad);
                    cv.put("fecha", fecha);

                    if (modoCobros && spTipo.getSelectedItem() instanceof CobroItem) {
                        CobroItem sel = (CobroItem) spTipo.getSelectedItem();
                        cv.put("cobro_id", sel.id);
                        // opcional: mantenemos 'tipo' por compatibilidad con pantallas antiguas
                        cv.put("tipo", sel.nombre);
                    } else {
                        String tipo = spTipo.getSelectedItem().toString();
                        cv.put("tipo", tipo);
                        // cobro_id se queda null en modo antiguo
                    }

                    db.insert("Cuotas", null, cv);
                    cargar();
                    Toast.makeText(CuotasActivity.this, "Cuota añadida", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** DatePicker para el campo fecha */
    private void pickFecha(EditText target) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, y, m, d) -> target.setText(String.format("%04d-%02d-%02d", y, m + 1, d)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    /** Parseo seguro de double con coma/punto */
    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); }
        catch (Exception e) { return 0.0; }
    }

    /** Diálogo para editar cuota (reutiliza layout de añadir) */
    private void dialogEditar(Cuota q) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_cuota, null);
        Spinner spTipo      = view.findViewById(R.id.spTipo);
        EditText etCantidad = view.findViewById(R.id.etCantidad);
        EditText etFecha    = view.findViewById(R.id.etFecha);

        if (modoCobros && !cacheCobros.isEmpty()) {
            ArrayAdapter<CobroItem> ad = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, cacheCobros);
            spTipo.setAdapter(ad);
            // Selección inicial por nombre (compatibilidad) o por índice si tuvieras q.cobroId
            if (q.tipo != null) {
                for (int i = 0; i < cacheCobros.size(); i++) {
                    if (cacheCobros.get(i).nombre.equalsIgnoreCase(q.tipo)) {
                        spTipo.setSelection(i); break;
                    }
                }
            }
        } else {
            String[] tipos = new String[]{"Seguro", "1ª Cuota", "2ª Cuota", "3ª Cuota"};
            spTipo.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, tipos));

            int idx = 0;
            for (int i = 0; i < tipos.length; i++) {
                if (q.tipo != null && tipos[i].equalsIgnoreCase(q.tipo)) { idx = i; break; }
            }
            spTipo.setSelection(idx);
        }

        etCantidad.setText(String.valueOf(q.cantidad));
        etFecha.setText(q.fecha);
        etFecha.setOnClickListener(v -> pickFecha(etFecha));

        new AlertDialog.Builder(this)
                .setTitle("Editar cuota")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String fecha    = etFecha.getText().toString().trim();
                    double cantidad = parseDoubleSafe(etCantidad.getText().toString());

                    if (fecha.isEmpty()) {
                        Toast.makeText(this, "Selecciona fecha", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("cantidad", cantidad);
                    cv.put("fecha", fecha);

                    if (modoCobros && spTipo.getSelectedItem() instanceof CobroItem) {
                        CobroItem sel = (CobroItem) spTipo.getSelectedItem();
                        cv.put("cobro_id", sel.id);
                        cv.put("tipo", sel.nombre); // compatibilidad
                    } else {
                        String tipo = spTipo.getSelectedItem().toString();
                        cv.put("tipo", tipo);
                        cv.putNull("cobro_id");
                    }

                    db.update("Cuotas", cv, "id=?", new String[]{ String.valueOf(q.id) });
                    cargar();
                    Toast.makeText(CuotasActivity.this, "Cuota actualizada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Confirmación y borrado de la cuota */
    private void confirmarBorrado(Cuota q) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar cuota")
                .setMessage("¿Seguro que quieres borrar esta cuota?")
                .setPositiveButton("Sí", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("Cuotas", "id=?", new String[]{ String.valueOf(q.id) });
                    cargar();
                    Toast.makeText(CuotasActivity.this, "Cuota eliminada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // ===== Helpers Cobros =====

    private boolean existeTabla(String nombre) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type IN ('table','view') AND name=?",
                new String[]{ nombre });
        try { return c.moveToFirst(); } finally { c.close(); }
    }

    private void cargarCobrosCache() {
        cacheCobros.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, nombre, temporada, importe_total FROM Cobros ORDER BY temporada DESC, id DESC",
                null);
        try {
            while (c.moveToNext()) {
                CobroItem it = new CobroItem();
                it.id = c.getInt(0);
                it.nombre = c.getString(1);
                String temp = c.getString(2);
                double imp = c.getDouble(3);
                it.label = it.nombre + " (" + (temp != null ? temp : "") + ") — " + imp + "€";
                cacheCobros.add(it);
            }
        } finally {
            c.close();
        }
        // Si no hay cobros definidos, seguimos en modo antiguo aunque exista la tabla
        if (cacheCobros.isEmpty()) modoCobros = false;
    }
}
