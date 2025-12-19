package com.finalproyecto.atleticoborgoon;

import com.finalproyecto.atleticoborgoon.data.DBHelper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

// ==== Edge-to-edge + insets ====
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.view.View;
// ===============================

import java.text.Normalizer;
import java.util.Locale;

public class JugadorActivity extends AppCompatActivity {

    DBHelper dbHelper;
    TextView tvNombre, tvDorsal, tvPosicion, tvTotales, tvPartidosJugados, tvCuotasResumen;
    ImageView ivFoto;
    Button btnPartidos, btnCuotas, btnEditar, btnVerEstadisticas;
    int jugadorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---- EDGE-TO-EDGE + INSETS ----
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_jugador);
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
        // --------------------------------

        dbHelper = new DBHelper(this);

        tvNombre          = findViewById(safeId("tvNombre"));
        tvDorsal          = findViewById(safeId("tvDorsal"));
        tvPosicion        = findViewById(safeId("tvPosicion"));
        tvTotales         = findViewById(safeId("tvTotales"));
        tvPartidosJugados = findViewById(safeId("tvPartidosJugados"));
        tvCuotasResumen   = findViewById(safeId("tvCuotasResumen"));
        ivFoto            = findViewById(safeId("ivFoto"));
        btnPartidos       = findViewById(safeId("btnPartidos"));
        btnCuotas         = findViewById(safeId("btnCuotas"));
        btnEditar         = findViewById(safeId("btnEditar"));
        btnVerEstadisticas= findViewById(safeId("btnVerEstadisticas"));

        jugadorId = getIntent().getIntExtra("jugador_id", -1);
        if (jugadorId == -1) {
            Toast.makeText(this, "Jugador no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (btnPartidos != null) {
            btnPartidos.setOnClickListener(v -> {
                Intent i = new Intent(this, PartidosActivity.class);
                i.putExtra("jugador_id", jugadorId);
                startActivity(i);
            });
        }

        if (btnCuotas != null) {
            btnCuotas.setOnClickListener(v -> {
                Intent i = new Intent(this, CuotasActivity.class);
                i.putExtra("jugador_id", jugadorId);
                startActivity(i);
            });
        }

        if (btnEditar != null) {
            btnEditar.setOnClickListener(v -> {
                Intent i = new Intent(this, EditJugadorActivity.class);
                i.putExtra("jugador_id", jugadorId);
                startActivity(i);
            });
        }

        if (btnVerEstadisticas != null) {
            btnVerEstadisticas.setOnClickListener(v -> {
                Intent i = new Intent(this, com.finalproyecto.atleticoborgoon.ui.EstadisticasJugadorActivity.class);
                i.putExtra(com.finalproyecto.atleticoborgoon.ui.EstadisticasJugadorActivity.EXTRA_JUGADOR_ID, jugadorId);
                startActivity(i);
            });
        }

        cargarJugador();
        cargarTotales();
        cargarPartidosJugados();
        cargarCuotasPorTipo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarJugador();
        cargarTotales();
        cargarPartidosJugados();
        cargarCuotasPorTipo();
    }

    private int safeId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }

    private void cargarJugador() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT nombre, apellido, dorsal, posicion, foto FROM Jugadores WHERE id = ?",
                new String[]{ String.valueOf(jugadorId) }
        );
        try {
            if (c.moveToFirst()) {
                String nombre   = c.getString(0);
                String apellido = c.getString(1);
                int dorsal      = c.getInt(2);
                String posicion = c.getString(3);
                String foto     = c.getString(4);

                if (tvNombre != null)   tvNombre.setText(getString(R.string.nombre_completo_fmt, nombre, apellido));
                if (tvDorsal != null)   tvDorsal.setText(getString(R.string.dorsal_fmt, dorsal));
                if (tvPosicion != null) tvPosicion.setText(getString(R.string.posicion_fmt, posicion));

                if (ivFoto != null) {
                    if (foto != null && !foto.isEmpty()) {
                        Picasso.get().load(Uri.parse(foto)).fit().centerCrop().into(ivFoto);
                    } else {
                        ivFoto.setImageResource(R.drawable.ic_jugador);
                    }
                }
            }
        } finally {
            c.close();
        }
    }

    private void cargarTotales() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int totalGoles = 0, totalAsist = 0;
        double totalEuros = 0.0;

        Cursor c1 = db.rawQuery(
                "SELECT IFNULL(SUM(goles),0), IFNULL(SUM(asistencias),0) FROM Partidos WHERE jugador_id=?",
                new String[]{ String.valueOf(jugadorId) });
        try {
            if (c1.moveToFirst()) { totalGoles = c1.getInt(0); totalAsist = c1.getInt(1); }
        } finally {
            c1.close();
        }

        Cursor c2 = db.rawQuery(
                "SELECT IFNULL(SUM(cantidad),0) FROM Cuotas WHERE jugador_id=?",
                new String[]{ String.valueOf(jugadorId) });
        try {
            if (c2.moveToFirst()) totalEuros = c2.getDouble(0);
        } finally {
            c2.close();
        }

        if (tvTotales != null) {
            tvTotales.setText(String.format("Totales: G%d  A%d   %.2f€", totalGoles, totalAsist, totalEuros));
        }
    }

    private void cargarPartidosJugados() {
        if (tvPartidosJugados == null) return;
        int count = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM Partidos WHERE jugador_id=?",
                new String[]{ String.valueOf(jugadorId) });
        try {
            if (c.moveToFirst()) count = c.getInt(0);
        } finally {
            c.close();
        }
        tvPartidosJugados.setText("Partidos jugados: " + count);
    }

    /**
     * Resumen de cuotas por tipo:
     * - Si existe 'vw_estado_cobro', usa esa vista (nuevo modelo).
     * - Si no, agrupa por 'tipo' en Cuotas (modo antiguo).
     */
    private void cargarCuotasPorTipo() {
        if (tvCuotasResumen == null) return;

        double seguro = 0, c1 = 0, c2 = 0, c3 = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        boolean usadoNuevo = false;
        Cursor test = null;
        try {
            test = db.rawQuery("SELECT name FROM sqlite_master WHERE type='view' AND name='vw_estado_cobro'", null);
            if (test.moveToFirst()) {
                Cursor c = db.rawQuery(
                        "SELECT nombre, IFNULL(pagado,0) FROM vw_estado_cobro WHERE jugador_id=?",
                        new String[]{ String.valueOf(jugadorId) });
                try {
                    while (c.moveToNext()) {
                        String nombre = c.getString(0);
                        double pagado = c.getDouble(1);
                        String bucket = bucketCobro(nombre);
                        if (bucket == null) continue;
                        switch (bucket) {
                            case "seguro": seguro = pagado; break;
                            case "c1":     c1 = pagado; break;
                            case "c2":     c2 = pagado; break;
                            case "c3":     c3 = pagado; break;
                        }
                    }
                    usadoNuevo = true;
                } finally { c.close(); }
            }
        } finally { if (test != null) test.close(); }

        if (!usadoNuevo) {
            Cursor c = db.rawQuery(
                    "SELECT COALESCE(q.tipo, cb.nombre) AS tipo_mostrar, IFNULL(SUM(q.cantidad),0) " +
                            "FROM Cuotas q LEFT JOIN Cobros cb ON cb.id = q.cobro_id " +
                            "WHERE q.jugador_id=? GROUP BY tipo_mostrar",
                    new String[]{ String.valueOf(jugadorId) });
            try {
                while (c.moveToNext()) {
                    String nombre = c.getString(0);
                    double suma = c.getDouble(1);
                    String bucket = bucketCobro(nombre);
                    if (bucket == null) continue;
                    switch (bucket) {
                        case "seguro": seguro = suma; break;
                        case "c1":     c1 = suma; break;
                        case "c2":     c2 = suma; break;
                        case "c3":     c3 = suma; break;
                    }
                }
            } finally { c.close(); }
        }

        tvCuotasResumen.setText(String.format(
                "Cuotas — Seguro: %.2f€ | 1ª: %.2f€ | 2ª: %.2f€ | 3ª: %.2f€",
                seguro, c1, c2, c3
        ));
    }

    /** Normaliza nombres de cobro y los mapea a: "seguro", "c1", "c2", "c3". */
    private String bucketCobro(String raw) {
        if (raw == null) return null;

        // 1) quitar acentos y bajar a minúsculas
        String s = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        // 2) normalizar símbolos ordinales y puntuación rara
        s = s.replace('º', 'o')
                .replace('°', 'o')
                .replace('ª', 'a')
                .replaceAll("[^a-z0-9 ]", " ");

        // espacios compactos
        s = s.replaceAll("\\s+", " ").trim();

        // 3) bucket "seguro"
        if (s.contains("seguro")) return "seguro";

        // 4) variantes de "cuota N"
        boolean contieneCuota = s.contains("cuota");
        boolean cuota1 = contieneCuota && (s.contains(" 1 ") || s.endsWith(" 1") || s.contains("1a") || s.contains("1o")
                || s.contains("1er") || s.contains("primer") || s.contains("cuota1"));
        boolean cuota2 = contieneCuota && (s.contains(" 2 ") || s.endsWith(" 2") || s.contains("2a") || s.contains("2o")
                || s.contains("2do") || s.contains("segunda") || s.contains("cuota2"));
        boolean cuota3 = contieneCuota && (s.contains(" 3 ") || s.endsWith(" 3") || s.contains("3a") || s.contains("3o")
                || s.contains("3er") || s.contains("tercera") || s.contains("cuota3"));

        if (cuota1) return "c1";
        if (cuota2) return "c2";
        if (cuota3) return "c3";

        // 5) fallback laxo
        if (contieneCuota) {
            if (s.startsWith("1")) return "c1";
            if (s.startsWith("2")) return "c2";
            if (s.startsWith("3")) return "c3";
        }
        return null;
    }
}


