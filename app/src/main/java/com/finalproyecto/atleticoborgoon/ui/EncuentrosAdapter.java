package com.finalproyecto.atleticoborgoon.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproyecto.atleticoborgoon.R;
import com.finalproyecto.atleticoborgoon.data.Repo;

import java.util.ArrayList;
import java.util.List;

public class EncuentrosAdapter extends RecyclerView.Adapter<EncuentrosAdapter.VH> {

    /** Click corto (abrir/editar) */
    public interface OnClick {
        void onClick(Repo.Encuentro e);
    }

    /** Long press (borrar/acciones) */
    public interface OnLongClick {
        void onLongClick(Repo.Encuentro e, int position);
    }

    private final List<Repo.Encuentro> data;
    private final OnClick onClick;
    private final OnLongClick onLongClick;

    /** Constructor completo */
    public EncuentrosAdapter(List<Repo.Encuentro> d, OnClick onClick, OnLongClick onLongClick) {
        this.data = d != null ? d : new ArrayList<>();
        this.onClick = onClick;
        this.onLongClick = onLongClick;
        setHasStableIds(true);
    }

    /** Constructor antiguo (solo click) para compatibilidad */
    public EncuentrosAdapter(List<Repo.Encuentro> d, OnClick onClick) {
        this(d, onClick, null);
    }

    /** Constructor muy antiguo (sin listeners) */
    public EncuentrosAdapter(List<Repo.Encuentro> d) {
        this(d, null, null);
    }

    public void submit(List<Repo.Encuentro> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public Repo.Encuentro getItem(int position) {
        return (position >= 0 && position < data.size()) ? data.get(position) : null;
    }

    public void removeAt(int position) {
        if (position >= 0 && position < data.size()) {
            data.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLinea1, tvLinea2;
        VH(@NonNull View v) {
            super(v);
            tvLinea1 = v.findViewById(R.id.tvLinea1);
            tvLinea2 = v.findViewById(R.id.tvLinea2);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_encuentro, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Repo.Encuentro e = data.get(pos);

        // Null-safe
        String fecha = e.fecha != null ? e.fecha : "";
        String rival = e.rivalNombre != null ? e.rivalNombre : "";
        String comp  = e.competicion != null ? e.competicion : "";
        String lugar = e.lugar != null ? e.lugar : "";
        String notas = (e.notas != null && !e.notas.isEmpty()) ? " · " + e.notas : "";

        h.tvLinea1.setText(fecha + " — " + rival +
                (comp.isEmpty() && lugar.isEmpty() ? "" : " (" + comp + (lugar.isEmpty() ? "" : ", " + lugar) + ")"));

        h.tvLinea2.setText("Marcador: " + e.golesFavor + " - " + e.golesContra + notas);

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(e);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (onLongClick != null) onLongClick.onLongClick(e, h.getBindingAdapterPosition());
            return true; // consumimos el evento
        });
    }

    @Override public int getItemCount() { return data.size(); }

    @Override public long getItemId(int position) {
        Repo.Encuentro e = getItem(position);
        return e != null ? e.id : RecyclerView.NO_ID;
    }
}
