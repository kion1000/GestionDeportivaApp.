package com.finalproyecto.atleticoborgoon;
import com.finalproyecto.atleticoborgoon.data.DBHelper;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

// ==== Edge-to-edge + insets ====
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.view.View;
// ===============================

public class MainActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private RecyclerView recycler;
    private JugadorAdapter adapter;
    private final ArrayList<Jugador> jugadores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---- EDGE-TO-EDGE + INSETS (Opción A) ----
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
        // ------------------------------------------

        dbHelper = new DBHelper(this);

        recycler = findViewById(R.id.recyclerJugadores);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnEncuentros).setOnClickListener(v ->
                startActivity(new Intent(this, com.finalproyecto.atleticoborgoon.ui.EncuentrosActivity.class))
        );

        findViewById(R.id.btnCobros).setOnClickListener(v ->
                startActivity(new Intent(this, com.finalproyecto.atleticoborgoon.ui.CobrosActivity.class))
        );

        adapter = new JugadorAdapter(jugadores, new JugadorAdapter.OnJugadorActionListener() {
            @Override
            public void onOpen(Jugador j) {
                Intent intent = new Intent(MainActivity.this, JugadorActivity.class);
                intent.putExtra("jugador_id", j.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(Jugador j, int position) {
                final int pos = position;
                final Jugador eliminado = j;
                adapter.removeAt(pos);

                Snackbar.make(recycler, "Jugador eliminado", Snackbar.LENGTH_LONG)
                        .setAction("DESHACER", v -> {
                            jugadores.add(pos, eliminado);
                            adapter.notifyItemInserted(pos);
                            recycler.scrollToPosition(pos);
                            Toast.makeText(MainActivity.this, "Acción deshecha", Toast.LENGTH_SHORT).show();
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                    borrarJugadorEnBD(eliminado.getId());
                                }
                            }
                        })
                        .show();
            }
        });
        recycler.setAdapter(adapter);

        findViewById(R.id.btnAddJugador).setOnClickListener(v ->
                startActivity(new Intent(this, EditJugadorActivity.class))
        );

        seedIfEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarJugadores();
    }

    private void seedIfEmpty() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM Jugadores", null);
        try {
            if (c.moveToFirst() && c.getInt(0) == 0) {
                SQLiteDatabase wdb = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("nombre", "Jonay");
                cv.put("apellido", "Armas");
                cv.put("dorsal", 25);
                cv.put("posicion", "Medio");
                cv.put("foto", "");
                wdb.insert("Jugadores", null, cv);
            }
        } finally {
            c.close();
        }
    }

    private void cargarJugadores() {
        jugadores.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, nombre, apellido, dorsal, posicion, foto FROM Jugadores ORDER BY dorsal ASC",
                null);
        try {
            while (c.moveToNext()) {
                Jugador j = new Jugador();
                j.setId(c.getInt(0));
                j.setNombre(c.getString(1));
                j.setApellido(c.getString(2));
                j.setDorsal(c.getInt(3));
                j.setPosicion(c.getString(4));
                j.setFoto(c.getString(5));

                // Totales Partidos
                Cursor c1 = db.rawQuery(
                        "SELECT IFNULL(SUM(goles),0), IFNULL(SUM(asistencias),0) " +
                                "FROM Partidos WHERE jugador_id=?",
                        new String[]{ String.valueOf(j.getId()) });
                try {
                    if (c1.moveToFirst()) {
                        j.setTotalGoles(c1.getInt(0));
                        j.setTotalAsistencias(c1.getInt(1));
                    }
                } finally {
                    c1.close();
                }

                // Total Cuotas €
                Cursor c2 = db.rawQuery(
                        "SELECT IFNULL(SUM(cantidad),0) FROM Cuotas WHERE jugador_id=?",
                        new String[]{ String.valueOf(j.getId()) });
                try {
                    if (c2.moveToFirst()) {
                        j.setTotalCuotas(c2.getDouble(0));
                    }
                } finally {
                    c2.close();
                }

                jugadores.add(j);
            }
        } finally {
            c.close();
        }

        adapter.notifyDataSetChanged();
    }

    /** Borrado real en BD: si tienes ON DELETE CASCADE basta con borrar Jugadores. */
    private void borrarJugadorEnBD(int jugadorId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Si tu DBHelper TIENE ON DELETE CASCADE en Partidos/Cuotas, con esta línea es suficiente:
        // db.delete("Jugadores", "id=?", new String[]{ String.valueOf(jugadorId) });

        // A prueba de balas aunque no haya CASCADE:
        db.delete("Partidos", "jugador_id=?", new String[]{ String.valueOf(jugadorId) });
        db.delete("Cuotas", "jugador_id=?", new String[]{ String.valueOf(jugadorId) });
        db.delete("Jugadores", "id=?", new String[]{ String.valueOf(jugadorId) });
    }
}
