package com.finalproyecto.atleticoborgoon.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproyecto.atleticoborgoon.R;

import java.util.ArrayList;

public class EstadisticaPartidoAdapter extends RecyclerView.Adapter<EstadisticaPartidoAdapter.VH> {

    public static class Row {
        public String fecha;        // "YYYY-MM-DD" o ""
        public String rival;        // "Rival" o ""
        public Integer gf, gc;      // marcador del equipo, si hay encuentro
        public int goles, asist;
        public String tarjetas;     // "ninguna/amarilla/roja"
    }

    private final ArrayList<Row> data = new ArrayList<>();
    public void submit(ArrayList<Row> rows) { data.clear(); data.addAll(rows); notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvFechaRival, tvMarcador, tvGoles, tvAsist, tvTarjetas;
        VH(@NonNull View v) {
            super(v);
            tvFechaRival = v.findViewById(R.id.tvFechaRival);
            tvMarcador   = v.findViewById(R.id.tvMarcador);
            tvGoles      = v.findViewById(R.id.tvGoles);
            tvAsist      = v.findViewById(R.id.tvAsist);
            tvTarjetas   = v.findViewById(R.id.tvTarjetas);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_estadistica_partido, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Row r = data.get(pos);
        String izq = (r.fecha == null ? "" : r.fecha);
        if (r.rival != null && !r.rival.isEmpty()) izq += (izq.isEmpty() ? "" : " · ") + r.rival;
        h.tvFechaRival.setText(izq);

        String marc = (r.gf == null || r.gc == null) ? "-" : (r.gf + "-" + r.gc);
        h.tvMarcador.setText(marc);

        h.tvGoles.setText(String.valueOf(r.goles));
        h.tvAsist.setText(String.valueOf(r.asist));
        String t = r.tarjetas == null ? "-" : r.tarjetas;
        h.tvTarjetas.setText("ninguna".equalsIgnoreCase(t) ? "-" : t);
    }

    @Override public int getItemCount() { return data.size(); }
}
