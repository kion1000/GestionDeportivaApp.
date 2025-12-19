package com.finalproyecto.atleticoborgoon.ui;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
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

public class EstadisticasJugadorActivity extends AppCompatActivity {

    public static final String EXTRA_JUGADOR_ID = "jugador_id";

    private int jugadorId = -1;
    private DBHelper dbHelper;
    private TextView tvHeader, tvSubHeader, tvTotales;
    private RecyclerView rv;
    private EstadisticaPartidoAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_estadisticas_jugador);

        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        jugadorId = getIntent().getIntExtra(EXTRA_JUGADOR_ID, -1);
        if (jugadorId <= 0) { Toast.makeText(this, "Jugador no válido", Toast.LENGTH_SHORT).show(); finish(); return; }

        dbHelper   = new DBHelper(this);
        tvHeader   = findViewById(safeId("tvHeaderJugador"));
        tvSubHeader= findViewById(safeId("tvSubHeader"));
        tvTotales  = findViewById(safeId("tvTotales"));
        rv         = findViewById(safeId("rvDetalle"));

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new EstadisticaPartidoAdapter();
            rv.setAdapter(adapter);
        } else {
            // Si no existe ese RecyclerView en el layout, no seguimos para evitar NPE
            Toast.makeText(this, "Falta rvDetalle en el layout", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarCabecera();
        cargarTotales();
        cargarDetalle();
    }

    private int safeId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }

    private void cargarCabecera() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT nombre, IFNULL(apellido,''), IFNULL(dorsal,0), IFNULL(posicion,'') " +
                        "FROM Jugadores WHERE id=?",
                new String[]{ String.valueOf(jugadorId) });
        try {
            if (c.moveToFirst()) {
                String nombre = (c.getString(0) + " " + c.getString(1)).trim();
                int dorsal     = c.getInt(2);
                String pos     = c.getString(3);

                if (tvHeader != null)   tvHeader.setText("#" + dorsal + " " + nombre);
                if (tvSubHeader != null) tvSubHeader.setText(pos == null ? "" : pos);
            }
        } finally { c.close(); }
    }

    private void cargarTotales() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int goles = 0, asist = 0, pj = 0;

        Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(goles),0), IFNULL(SUM(asistencias),0), COUNT(*) " +
                        "FROM Partidos WHERE jugador_id=?",
                new String[]{ String.valueOf(jugadorId) });
        try {
            if (c.moveToFirst()) {
                goles = c.getInt(0);
                asist = c.getInt(1);
                pj    = c.getInt(2);
            }
        } finally { c.close(); }

        int amar = 0, roja = 0;
        Cursor ct = db.rawQuery(
                "SELECT IFNULL(SUM(amarillas),0), IFNULL(SUM(rojas),0) FROM Partidos WHERE jugador_id=?",
                new String[]{ String.valueOf(jugadorId) });
        try {
            if (ct.moveToFirst()) { amar = ct.getInt(0); roja = ct.getInt(1); }
        } finally { ct.close(); }

        if (amar == 0 && roja == 0) {
            Cursor ctf = db.rawQuery(
                    "SELECT tarjetas, COUNT(*) FROM Partidos WHERE jugador_id=? AND tarjetas IS NOT NULL GROUP BY tarjetas",
                    new String[]{ String.valueOf(jugadorId) });
            try {
                while (ctf.moveToNext()) {
                    String t = ctf.getString(0);
                    int n = ctf.getInt(1);
                    if ("amarilla".equalsIgnoreCase(t)) amar += n;
                    else if ("roja".equalsIgnoreCase(t)) roja += n;
                }
            } finally { ctf.close(); }
        }

        String tarjetaText;
        if (amar == 0 && roja == 0) tarjetaText = "-";
        else if (amar > 0 && roja == 0) tarjetaText = "Amarillas: " + amar;
        else if (roja > 0 && amar == 0) tarjetaText = "Rojas: " + roja;
        else tarjetaText = "Amarillas: " + amar + " · Rojas: " + roja;

        if (tvTotales != null) {
            tvTotales.setText(String.format("Totales: G%d  A%d  T: %s   PJ: %d", goles, asist, tarjetaText, pj));
        }
    }

    private void cargarDetalle() {
        ArrayList<EstadisticaPartidoAdapter.Row> rows = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT p.goles, p.asistencias, " +
                        "       IFNULL(p.amarillas,0), IFNULL(p.rojas,0), " +
                        "       p.tarjetas, " +
                        "       p.fecha, e.fecha, r.nombre, e.goles_favor, e.goles_contra " +
                        "FROM Partidos p " +
                        "LEFT JOIN Encuentros e ON e.id = p.encuentro_id " +
                        "LEFT JOIN Equipos r    ON r.id = e.rival_id " +
                        "WHERE p.jugador_id=? " +
                        "ORDER BY COALESCE(e.fecha, p.fecha) DESC, p.id DESC",
                new String[]{ String.valueOf(jugadorId) });

        try {
            while (c.moveToNext()) {
                EstadisticaPartidoAdapter.Row r = new EstadisticaPartidoAdapter.Row();

                r.goles = c.getInt(0);
                r.asist = c.getInt(1);
                int amarillas = c.getInt(2);
                int rojas     = c.getInt(3);
                String tarjetasTexto = c.getString(4);

                String fechaPartidos  = c.getString(5);
                String fechaEncuentro = c.getString(6);
                r.fecha = (fechaEncuentro != null) ? fechaEncuentro : (fechaPartidos == null ? "" : fechaPartidos);
                r.rival = c.getString(7);
                r.gf    = c.isNull(8) ? null : c.getInt(8);
                r.gc    = c.isNull(9) ? null : c.getInt(9);

                StringBuilder sb = new StringBuilder();
                if (amarillas > 0) sb.append("A").append(amarillas);
                if (rojas > 0) {
                    if (sb.length() > 0) sb.append("+");
                    sb.append("R").append(rojas);
                }
                if (sb.length() == 0) {
                    if (tarjetasTexto != null) {
                        if ("amarilla".equalsIgnoreCase(tarjetasTexto)) sb.append("A1");
                        else if ("roja".equalsIgnoreCase(tarjetasTexto)) sb.append("R1");
                    }
                    if (sb.length() == 0) sb.append("-");
                }
                r.tarjetas = sb.toString();

                rows.add(r);
            }
        } finally { c.close(); }

        adapter.submit(rows);
    }
}
