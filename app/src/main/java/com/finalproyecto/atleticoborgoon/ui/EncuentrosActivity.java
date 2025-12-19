package com.finalproyecto.atleticoborgoon.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.finalproyecto.atleticoborgoon.data.Repo;

import java.util.ArrayList;

public class EncuentrosActivity extends AppCompatActivity {

    private Repo repo;
    private RecyclerView rv;
    private EncuentrosAdapter adapter;
    private Spinner spRivales;
    private EditText etFecha, etGF, etGC, etComp, etLugar, etNotas;
    private ArrayAdapter<Repo.Equipo> rivalesAdapter;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_encuentros);

        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        repo = new Repo(this);

        spRivales = findViewById(R.id.spRival);
        etFecha   = findViewById(R.id.etFecha);
        etGF      = findViewById(R.id.etGF);
        etGC      = findViewById(R.id.etGC);
        etComp    = findViewById(R.id.etCompeticion);
        etLugar   = findViewById(R.id.etLugar);
        etNotas   = findViewById(R.id.etNotas);

        rv = findViewById(R.id.rvEncuentros);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Click -> abrir detalle; Long press -> confirmar borrado
        adapter = new EncuentrosAdapter(
                new ArrayList<>(),
                e -> {
                    Intent i = new Intent(this, EncuentroDetalleActivity.class);
                    i.putExtra(EncuentroDetalleActivity.EXTRA_ENCUENTRO_ID, e.id);
                    startActivity(i);
                },
                (e, pos) -> confirmarBorradoEncuentro(e)
        );
        rv.setAdapter(adapter);

        findViewById(R.id.btnAddRival).setOnClickListener(v -> onAddRival());
        findViewById(R.id.btnGuardarEncuentro).setOnClickListener(v -> onGuardar());

        cargarRivales();
        cargarLista();

        // Pista de UX (opcional)
        Toast.makeText(this, "Mantén pulsado un encuentro para borrarlo", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onResume() {
        super.onResume();
        cargarLista(); // por si volvemos del detalle con cambios
    }

    private void cargarRivales() {
        ArrayList<Repo.Equipo> equipos = repo.listEquipos();
        rivalesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, equipos);
        rivalesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRivales.setAdapter(rivalesAdapter);
    }

    private void cargarLista() {
        adapter.submit(repo.listEncuentros());
    }

    private void onAddRival() {
        EditText input = new EditText(this);
        input.setHint("Nombre del equipo");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo rival")
                .setView(input)
                .setPositiveButton("Guardar", (d, w) -> {
                    String nombre = input.getText().toString().trim();
                    if (nombre.isEmpty()) return;
                    repo.addEquipoIfNotExists(nombre);
                    cargarRivales();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void onGuardar() {
        Repo.Equipo sel = (Repo.Equipo) spRivales.getSelectedItem();
        if (sel == null) { Toast.makeText(this, "Elige un rival", Toast.LENGTH_SHORT).show(); return; }
        String fecha = etFecha.getText().toString().trim();
        if (fecha.isEmpty()) { Toast.makeText(this, "Fecha (YYYY-MM-DD)", Toast.LENGTH_SHORT).show(); return; }
        int gf = parseInt(etGF.getText().toString());
        int gc = parseInt(etGC.getText().toString());
        String comp  = etComp.getText().toString().trim();
        String lugar = etLugar.getText().toString().trim();
        String notas = etNotas.getText().toString().trim();

        repo.upsertEncuentro(null, fecha, sel.id, gf, gc, comp, lugar, notas);
        Toast.makeText(this, "Encuentro guardado", Toast.LENGTH_SHORT).show();
        etNotas.setText(""); etGF.setText(""); etGC.setText("");
        cargarLista();
    }

    private void confirmarBorradoEncuentro(Repo.Encuentro e) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Borrar encuentro")
                .setMessage("¿Eliminar el encuentro del " + (e.fecha == null ? "" : e.fecha) +
                        (e.rivalNombre == null ? "" : " vs. " + e.rivalNombre) +
                        "?\nSe borrarán también las estadísticas de los jugadores.")
                .setPositiveButton("Borrar", (d, w) -> {
                    boolean ok = repo.deleteEncuentro(e.id); // necesita el método en Repo
                    if (ok) {
                        Toast.makeText(this, "Encuentro eliminado", Toast.LENGTH_SHORT).show();
                        cargarLista();
                    } else {
                        Toast.makeText(this, "No se pudo borrar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
