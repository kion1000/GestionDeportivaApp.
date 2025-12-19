package com.finalproyecto.atleticoborgoon.ui;

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
import com.finalproyecto.atleticoborgoon.data.DBHelper;
import com.finalproyecto.atleticoborgoon.data.Repo;

import java.util.ArrayList;

public class CobrosActivity extends AppCompatActivity {

    private Repo repo;
    private Spinner spCobro;
    private RecyclerView rv;
    private EstadoCobroAdapter adapter;

    private ArrayList<Repo.EstadoCobroRow> current;
    private ArrayAdapter<String> cobrosAdapter;
    private ArrayList<Integer> cobrosIds; // paralelo al spinner

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cobros);
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        repo = new Repo(this);
        spCobro = findViewById(R.id.spCobro);
        rv = findViewById(R.id.rvEstadoCobro);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EstadoCobroAdapter(new ArrayList<>(), this::showAbonoDialog);
        rv.setAdapter(adapter);

        findViewById(R.id.btnNuevoCobro).setOnClickListener(v -> nuevoCobroDialog());
        findViewById(R.id.btnRefrescar).setOnClickListener(v -> cargarTabla());
        findViewById(R.id.btnEliminarCobro).setOnClickListener(v -> eliminarCobroSeleccionado());


        // Al cambiar de cobro en el spinner, refresca la tabla
        spCobro.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                cargarTabla();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { /* no-op */ }
        });

        cargarCobrosSpinner(); // esto llamará a cargarTabla() si hay elementos
    }

    @Override protected void onResume() {
        super.onResume();
        // Por si regresamos de otra pantalla con datos nuevos
        int prev = spCobro.getSelectedItemPosition();
        cargarCobrosSpinner();
        if (prev >= 0 && prev < (cobrosIds != null ? cobrosIds.size() : 0)) {
            spCobro.setSelection(prev);
        }
        cargarTabla();
    }

    private void cargarCobrosSpinner() {
        cobrosIds = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        var db = new DBHelper(this).getReadableDatabase();
        var c = db.rawQuery("SELECT id, nombre, temporada, importe_total FROM Cobros ORDER BY temporada DESC, id DESC", null);
        try {
            while (c.moveToNext()) {
                cobrosIds.add(c.getInt(0));
                String nombre = c.getString(1);
                String temp = c.getString(2);
                double imp = c.getDouble(3);
                String label = nombre + " (" + (temp == null ? "" : temp) + ") — " + imp + "€";
                labels.add(label);
            }
        } finally { c.close(); }

        cobrosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        cobrosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCobro.setAdapter(cobrosAdapter);

        if (labels.isEmpty()) {
            adapter.submit(new ArrayList<>());
            Toast.makeText(this, "No hay cobros definidos. Crea uno con “+ Cobro”.", Toast.LENGTH_LONG).show();
        }
    }

    private void cargarTabla() {
        Integer cobroId = getCobroIdSeleccionado();
        if (cobroId == null) {
            adapter.submit(new ArrayList<>());
            return;
        }
        current = repo.estadoCobroPorConcepto(cobroId);
        adapter.submit(current);
    }

    private Integer getCobroIdSeleccionado() {
        if (cobrosIds == null || cobrosIds.isEmpty()) return null;
        int pos = spCobro.getSelectedItemPosition();
        if (pos < 0 || pos >= cobrosIds.size()) return null;
        return cobrosIds.get(pos);
    }

    private void nuevoCobroDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_nuevo_cobro, null, false);
        EditText etNombre = view.findViewById(R.id.etNombreCobro);
        EditText etTemp = view.findViewById(R.id.etTemporada);
        EditText etImporte = view.findViewById(R.id.etImporteTotal);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo cobro")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String temp = etTemp.getText().toString().trim();
                    double imp = parseDouble(etImporte.getText().toString());
                    if (nombre.isEmpty() || temp.isEmpty() || imp <= 0) {
                        Toast.makeText(this, "Completa nombre/temporada/importe", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int id = repo.upsertCobro(null, nombre, temp, imp);
                    cargarCobrosSpinner();
                    // Seleccionar el recién creado si lo encontramos en la lista
                    int idx = cobrosIds.indexOf(id);
                    if (idx >= 0) spCobro.setSelection(idx);
                    cargarTabla();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAbonoDialog(Repo.EstadoCobroRow row) {
        if (row.pendiente <= 0) {
            Toast.makeText(this, "Este jugador ya está al día en este cobro.", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Importe (pendiente: " + String.format("%.2f", row.pendiente) + "€)");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Abono de " + row.jugador)
                .setView(input)
                .setPositiveButton("Guardar", (d, w) -> {
                    double imp = parseDouble(input.getText().toString());
                    if (imp <= 0) {
                        Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Capar al pendiente
                    imp = Math.min(imp, row.pendiente);

                    String hoy = java.time.LocalDate.now().toString();
                    boolean ok = repo.insertarAbono(row.jugadorId, row.cobroId, imp, hoy);
                    if (ok) {
                        Toast.makeText(this, "Abono registrado", Toast.LENGTH_SHORT).show();
                        cargarTabla();
                    } else {
                        Toast.makeText(this, "Error guardando", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); }
        catch (Exception e) { return 0.0; }
    }

    private void eliminarCobroSeleccionado() {
        Integer cobroId = getCobroIdSeleccionado();
        if (cobroId == null) {
            Toast.makeText(this, "No hay cobro seleccionado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Info: nº de abonos y total
        var db = new com.finalproyecto.atleticoborgoon.data.DBHelper(this).getReadableDatabase();
        int numAbonos = 0; double totalAbonado = 0.0;
        var c = db.rawQuery("SELECT COUNT(*), IFNULL(SUM(cantidad),0) FROM Cuotas WHERE cobro_id=?",
                new String[]{ String.valueOf(cobroId) });
        try {
            if (c.moveToFirst()) { numAbonos = c.getInt(0); totalAbonado = c.getDouble(1); }
        } finally { c.close(); }

        String msg = "Vas a eliminar este cobro.\n\n" +
                "Abonos vinculados: " + numAbonos + "\n" +
                "Total abonado: " + String.format("%.2f€", totalAbonado) + "\n\n" +
                "Se borrarán el concepto y todos sus abonos. ¿Continuar?";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar cobro")
                .setMessage(msg)
                .setPositiveButton("Eliminar", (d, w) -> {
                    borrarCobroEnBD(cobroId);
                    cargarCobrosSpinner();
                    cargarTabla();
                    Toast.makeText(this, "Cobro eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrarCobroEnBD(int cobroId) {
        var helper = new com.finalproyecto.atleticoborgoon.data.DBHelper(this);
        var db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("Cuotas", "cobro_id=?", new String[]{ String.valueOf(cobroId) });
            db.delete("Cobros", "id=?", new String[]{ String.valueOf(cobroId) });
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

}
