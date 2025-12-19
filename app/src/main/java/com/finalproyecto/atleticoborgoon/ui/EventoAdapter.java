package com.finalproyecto.atleticoborgoon.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproyecto.atleticoborgoon.R;

import java.util.ArrayList;
import java.util.List;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.VH> {

    public static class Evento {
        public enum Tipo { GOL, ASIST, TARJ_AMARILLA, TARJ_ROJA }
        public Tipo tipo;
        public int jugadorId;
        public String jugadorNombre;

        public Evento(Tipo t, int jId, String jNom) {
            this.tipo = t; this.jugadorId = jId; this.jugadorNombre = jNom;
        }
    }

    public interface OnRemove { void onRemove(int position); }

    private final List<Evento> data = new ArrayList<>();
    private final OnRemove onRemove;

    public EventoAdapter(OnRemove onRemove) { this.onRemove = onRemove; }

    public void submit(List<Evento> list) { data.clear(); data.addAll(list); notifyDataSetChanged(); }
    public void add(Evento e) { data.add(e); notifyItemInserted(data.size()-1); }
    public void removeAt(int pos) { if (pos>=0 && pos<data.size()) { data.remove(pos); notifyItemRemoved(pos); } }
    public List<Evento> getData() { return data; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTipo, tvJugador; Button btnBorrar;
        VH(@NonNull View v) {
            super(v);
            tvTipo = v.findViewById(R.id.tvTipo);
            tvJugador = v.findViewById(R.id.tvJugador);
            btnBorrar = v.findViewById(R.id.btnBorrar);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_evento, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Evento e = data.get(pos);
        String tipo = e.tipo == Evento.Tipo.GOL ? "Gol" :
                e.tipo == Evento.Tipo.ASIST ? "Asistencia" :
                        e.tipo == Evento.Tipo.TARJ_AMARILLA ? "Amarilla" : "Roja";
        h.tvTipo.setText(tipo);
        h.tvJugador.setText(e.jugadorNombre);
        h.btnBorrar.setOnClickListener(v -> { if (onRemove != null) onRemove.onRemove(h.getBindingAdapterPosition()); });
    }

    @Override public int getItemCount() { return data.size(); }
}
