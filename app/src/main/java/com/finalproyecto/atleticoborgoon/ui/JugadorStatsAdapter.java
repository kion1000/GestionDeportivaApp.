package com.finalproyecto.atleticoborgoon.ui;

import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproyecto.atleticoborgoon.R;

import java.util.ArrayList;
import java.util.List;

public class JugadorStatsAdapter extends RecyclerView.Adapter<JugadorStatsAdapter.VH> {

    public static class Row {
        public int jugadorId;
        public String nombre;
        public int goles;
        public int asist;
        public String tarjetas; // "ninguna","amarilla","roja"
    }

    private final List<Row> data;

    public JugadorStatsAdapter(List<Row> d) { this.data = d; }

    public void submit(ArrayList<Row> list) {
        data.clear(); data.addAll(list); notifyDataSetChanged();
    }

    public List<Row> getData() { return data; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNombre;
        EditText etGoles, etAsist;
        Spinner spTarjetas;
        VH(@NonNull View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombreJugador);
            etGoles = v.findViewById(R.id.etGoles);
            etAsist = v.findViewById(R.id.etAsist);
            spTarjetas = v.findViewById(R.id.spTarjetas);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_jugador_stats, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Row r = data.get(pos);
        h.tvNombre.setText(r.nombre);

        h.etGoles.setInputType(InputType.TYPE_CLASS_NUMBER);
        h.etAsist.setInputType(InputType.TYPE_CLASS_NUMBER);
        // límite razonable
        h.etGoles.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(2) });
        h.etAsist.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(2) });

        h.etGoles.setText(String.valueOf(r.goles));
        h.etAsist.setText(String.valueOf(r.asist));

        ArrayAdapter<String> ad = new ArrayAdapter<>(h.itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ninguna", "amarilla", "roja"});
        h.spTarjetas.setAdapter(ad);

        int idx = 0;
        if ("amarilla".equalsIgnoreCase(r.tarjetas)) idx = 1;
        else if ("roja".equalsIgnoreCase(r.tarjetas)) idx = 2;
        h.spTarjetas.setSelection(idx);

        // listeners que actualizan el modelo in-place
        h.etGoles.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) r.goles = parseIntSafe(h.etGoles.getText().toString());
        });
        h.etAsist.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) r.asist = parseIntSafe(h.etAsist.getText().toString());
        });

        h.spTarjetas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                r.tarjetas = position == 1 ? "amarilla" : position == 2 ? "roja" : "ninguna";
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });
    }

    private int parseIntSafe(String s) {
        try { return Math.max(0, Integer.parseInt(s.trim())); } catch (Exception e) { return 0; }
    }

    @Override public int getItemCount() { return data.size(); }
}
