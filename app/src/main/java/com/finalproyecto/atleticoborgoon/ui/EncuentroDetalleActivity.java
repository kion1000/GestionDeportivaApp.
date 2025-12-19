package com.finalproyecto.atleticoborgoon.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproyecto.atleticoborgoon.R;
import com.finalproyecto.atleticoborgoon.data.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EncuentroDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_ENCUENTRO_ID = "encuentro_id";

    private int encuentroId = -1;
    private DBHelper dbHelper;

    private TextView tvHeaderLinea1, tvHeaderLinea2;
    private Spinner spGol, spAsist, spTarj, spTipoTarj;
    private RecyclerView rv;
    private EventoAdapter adapter;

    private static class JugadorItem {
        int id;
        String nombre;
        @Override public String toString() { return nombre; }
    }

    private ArrayList<JugadorItem> jugadores;

    // marcador del encuentro (para validar límites)
    private int golesFavor = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_encuentro_detalle);

        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        encuentroId = getIntent().getIntExtra(EXTRA_ENCUENTRO_ID, -1);
        if (encuentroId <= 0) {
            Toast.makeText(this, "Encuentro no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DBHelper(this);

        tvHeaderLinea1 = findViewById(R.id.tvHeaderLinea1);
        tvHeaderLinea2 = findViewById(R.id.tvHeaderLinea2);

        spGol = findViewById(R.id.spJugadorGol);
        spAsist = findViewById(R.id.spJugadorAsist);
        spTarj = findViewById(R.id.spJugadorTarjeta);
        spTipoTarj = findViewById(R.id.spTipoTarjeta);

        rv = findViewById(R.id.rvEventos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventoAdapter(pos -> adapter.removeAt(pos));
        rv.setAdapter(adapter);

        // combos
        cargarCabeceraYMarcador();
        cargarJugadoresEnSpinners();
        spTipoTarj.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Amarilla", "Roja"}
        ));

        // botones
        findViewById(R.id.btnAddGol).setOnClickListener(v -> addGol());
        findViewById(R.id.btnAddAsist).setOnClickListener(v -> addAsist());
        findViewById(R.id.btnAddTarjeta).setOnClickListener(v -> addTarjeta());

        Button btnGuardar = findViewById(R.id.btnGuardarStats);
        btnGuardar.setOnClickListener(v -> guardarTodo());

        // cargar stats previas
        cargarEventosGuardados();
    }

    private void cargarCabeceraYMarcador() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.fecha, r.nombre, e.goles_favor, e.goles_contra, IFNULL(e.competicion,''), IFNULL(e.lugar,'') " +
                        "FROM Encuentros e JOIN Equipos r ON r.id=e.rival_id WHERE e.id=?",
                new String[]{ String.valueOf(encuentroId) }
        );
        try {
            if (c.moveToFirst()) {
                String fecha = c.getString(0);
                String rival = c.getString(1);
                golesFavor = c.getInt(2);
                int gc = c.getInt(3);
                String comp = c.getString(4);
                String lugar = c.getString(5);

                tvHeaderLinea1.setText(fecha + " · vs. " + rival);

                String extra = "";
                if (!comp.isEmpty()) extra += comp;
                if (!lugar.isEmpty()) extra += (extra.isEmpty() ? "" : " · ") + lugar;

                tvHeaderLinea2.setText("Marcador: " + golesFavor + "-" + gc + (extra.isEmpty() ? "" : " · " + extra));
            }
        } finally {
            c.close();
        }
    }

    private void cargarJugadoresEnSpinners() {
        jugadores = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, nombre, IFNULL(apellido,'') FROM Jugadores ORDER BY dorsal ASC, nombre",
                null
        );
        try {
            while (c.moveToNext()) {
                JugadorItem j = new JugadorItem();
                j.id = c.getInt(0);
                j.nombre = c.getString(1) + " " + c.getString(2);
                jugadores.add(j);
            }
        } finally {
            c.close();
        }

        ArrayAdapter<JugadorItem> ad =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, jugadores);

        spGol.setAdapter(ad);
        spAsist.setAdapter(ad);
        spTarj.setAdapter(ad);
    }

    private JugadorItem getSel(Spinner sp) {
        int p = sp.getSelectedItemPosition();
        if (p < 0 || p >= jugadores.size()) return null;
        return jugadores.get(p);
    }

    // ===== Validaciones rápidas =====

    private int contar(EventoAdapter.Evento.Tipo tipo) {
        int n = 0;
        for (EventoAdapter.Evento e : adapter.getData()) {
            if (e.tipo == tipo) n++;
        }
        return n;
    }

    private int contarAmarillasDeJugador(int jugadorId) {
        int n = 0;
        for (EventoAdapter.Evento e : adapter.getData()) {
            if (e.jugadorId == jugadorId && e.tipo == EventoAdapter.Evento.Tipo.TARJ_AMARILLA) n++;
        }
        return n;
    }

    private boolean tieneRoja(int jugadorId) {
        for (EventoAdapter.Evento e : adapter.getData()) {
            if (e.jugadorId == jugadorId && e.tipo == EventoAdapter.Evento.Tipo.TARJ_ROJA) return true;
        }
        return false;
    }

    private void addGol() {
        if (golesFavor <= 0) {
            Toast.makeText(this, "Este encuentro tiene 0 goles a favor.", Toast.LENGTH_SHORT).show();
            return;
        }

        int golesActuales = contar(EventoAdapter.Evento.Tipo.GOL);
        if (golesActuales + 1 > golesFavor) {
            Toast.makeText(this, "No puedes añadir más goles que el marcador (" + golesFavor + ").", Toast.LENGTH_SHORT).show();
            return;
        }

        JugadorItem j = getSel(spGol);
        if (j == null) return;

        adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.GOL, j.id, j.nombre));
    }

    private void addAsist() {
        if (golesFavor <= 0) {
            Toast.makeText(this, "Este encuentro tiene 0 goles a favor, no hay asistencias.", Toast.LENGTH_SHORT).show();
            return;
        }

        int asistActuales = contar(EventoAdapter.Evento.Tipo.ASIST);
        if (asistActuales + 1 > golesFavor) {
            Toast.makeText(this, "No puedes añadir más asistencias que goles (" + golesFavor + ").", Toast.LENGTH_SHORT).show();
            return;
        }

        JugadorItem j = getSel(spAsist);
        if (j == null) return;

        adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.ASIST, j.id, j.nombre));
    }

    private void addTarjeta() {
        JugadorItem j = getSel(spTarj);
        if (j == null) return;

        String t = (String) spTipoTarj.getSelectedItem();
        boolean amarilla = "Amarilla".equalsIgnoreCase(t);

        if (amarilla) {
            if (tieneRoja(j.id)) {
                Toast.makeText(this, "El jugador ya tiene roja.", Toast.LENGTH_SHORT).show();
                return;
            }

            int y = contarAmarillasDeJugador(j.id);
            if (y >= 2) {
                Toast.makeText(this, "Máximo 2 amarillas por jugador.", Toast.LENGTH_SHORT).show();
                return;
            }

            adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.TARJ_AMARILLA, j.id, j.nombre));
        } else {
            if (tieneRoja(j.id)) {
                Toast.makeText(this, "Máximo 1 roja por jugador.", Toast.LENGTH_SHORT).show();
                return;
            }

            adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.TARJ_ROJA, j.id, j.nombre));
        }
    }

    // ===== Guardado final =====

    private void guardarTodo() {

        Map<Integer, int[]> acc = new HashMap<>();          // jugadorId -> [goles, asist]
        Map<Integer, Integer> amarillas = new HashMap<>(); // jugadorId -> count
        Map<Integer, Boolean> roja = new HashMap<>();      // jugadorId -> true/false

        int totalGoles = 0, totalAsist = 0;

        for (EventoAdapter.Evento e : adapter.getData()) {
            switch (e.tipo) {
                case GOL:
                    totalGoles++;
                    acc.computeIfAbsent(e.jugadorId, k -> new int[]{0, 0})[0]++;
                    break;
                case ASIST:
                    totalAsist++;
                    acc.computeIfAbsent(e.jugadorId, k -> new int[]{0, 0})[1]++;
                    break;
                case TARJ_AMARILLA:
                    amarillas.put(e.jugadorId, amarillas.getOrDefault(e.jugadorId, 0) + 1);
                    break;
                case TARJ_ROJA:
                    roja.put(e.jugadorId, true);
                    break;
            }
        }

        // Validaciones de marcador
        if (totalGoles > golesFavor) {
            Toast.makeText(this, "Tienes " + totalGoles + " goles y el marcador es " + golesFavor + ". Reduce goles.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (totalAsist > golesFavor) {
            Toast.makeText(this, "Tienes " + totalAsist + " asistencias y el marcador es " + golesFavor + ". Reduce asistencias.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Validaciones por jugador de tarjetas
        for (Map.Entry<Integer, Integer> e : amarillas.entrySet()) {
            if (e.getValue() > 2) {
                Toast.makeText(this, "Jugador " + getNombre(e.getKey()) + ": máximo 2 amarillas.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Escribir en BD
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("Partidos", "encuentro_id=?", new String[]{ String.valueOf(encuentroId) });

            // UNION de todos los jugadores con cualquier evento
            Set<Integer> ids = new HashSet<>();
            ids.addAll(acc.keySet());
            ids.addAll(amarillas.keySet());
            ids.addAll(roja.keySet());

            for (Integer jugadorId : ids) {

                int[] ga = acc.getOrDefault(jugadorId, new int[]{0, 0});
                int goles = ga[0];
                int asist = ga[1];

                int y = amarillas.getOrDefault(jugadorId, 0);
                boolean r = roja.getOrDefault(jugadorId, false);

                String tarj = "ninguna";
                if (r) tarj = "roja";
                else if (y > 0) tarj = "amarilla";

                ContentValues cv = new ContentValues();
                cv.put("jugador_id", jugadorId);
                cv.put("goles", goles);
                cv.put("asistencias", asist);
                cv.put("tarjetas", tarj);
                cv.put("amarillas", y);
                cv.put("rojas", r ? 1 : 0);
                cv.put("fecha", (String) null);
                cv.put("encuentro_id", encuentroId);

                db.insert("Partidos", null, cv);
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, "Estadísticas guardadas", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception ex) {
            Toast.makeText(this, "Error guardando: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }
    }

    private String getNombre(int jugadorId) {
        for (JugadorItem j : jugadores) {
            if (j.id == jugadorId) return j.nombre;
        }
        return "ID " + jugadorId;
    }

    private void cargarEventosGuardados() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT jugador_id, IFNULL(goles,0), IFNULL(asistencias,0), " +
                        "IFNULL(amarillas,0), IFNULL(rojas,0) " +
                        "FROM Partidos WHERE encuentro_id=?",
                new String[]{ String.valueOf(encuentroId) }
        );
        try {
            while (c.moveToNext()) {
                int jId = c.getInt(0);
                int g   = c.getInt(1);
                int a   = c.getInt(2);
                int y   = c.getInt(3);
                int r   = c.getInt(4);

                String nombre = getNombre(jId);

                for (int i = 0; i < g; i++)
                    adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.GOL, jId, nombre));

                for (int i = 0; i < a; i++)
                    adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.ASIST, jId, nombre));

                for (int i = 0; i < y; i++)
                    adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.TARJ_AMARILLA, jId, nombre));

                if (r > 0)
                    adapter.add(new EventoAdapter.Evento(EventoAdapter.Evento.Tipo.TARJ_ROJA, jId, nombre));
            }
        } finally {
            c.close();
        }
    }
}
