package com.finalproyecto.atleticoborgoon.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproyecto.atleticoborgoon.R;
import com.finalproyecto.atleticoborgoon.data.Repo;

import java.util.ArrayList;
import java.util.List;

public class EstadoCobroAdapter extends RecyclerView.Adapter<EstadoCobroAdapter.VH> {

    public interface OnAbonar {
        void onClick(Repo.EstadoCobroRow row);
    }

    private final List<Repo.EstadoCobroRow> data;
    private final OnAbonar listener;

    public EstadoCobroAdapter(List<Repo.EstadoCobroRow> d, OnAbonar l) {
        this.data = d; this.listener = l;
    }

    public void submit(ArrayList<Repo.EstadoCobroRow> list) {
        data.clear(); data.addAll(list); notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvJugador, tvImporte, tvPagado, tvPendiente;
        Button btnAbonar;
        VH(@NonNull View v) {
            super(v);
            tvJugador = v.findViewById(R.id.tvJugador);
            tvImporte = v.findViewById(R.id.tvImporteTotal);
            tvPagado = v.findViewById(R.id.tvPagado);
            tvPendiente = v.findViewById(R.id.tvPendiente);
            btnAbonar = v.findViewById(R.id.btnAbonar);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_estado_cobro, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Repo.EstadoCobroRow r = data.get(pos);
        h.tvJugador.setText(r.jugador);
        h.tvImporte.setText(String.format("Total: %.2f€", r.importeTotal));
        h.tvPagado.setText(String.format("Pagado: %.2f€", r.pagado));
        h.tvPendiente.setText(String.format("Pendiente: %.2f€", r.pendiente));
        h.btnAbonar.setEnabled(r.pendiente > 0.0001);
        h.btnAbonar.setOnClickListener(v -> listener.onClick(r));
    }

    @Override public int getItemCount() { return data.size(); }
}
