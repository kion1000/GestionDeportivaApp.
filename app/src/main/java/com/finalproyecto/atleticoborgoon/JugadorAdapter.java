package com.finalproyecto.atleticoborgoon;
import com.finalproyecto.atleticoborgoon.data.DBHelper;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class JugadorAdapter extends RecyclerView.Adapter<JugadorAdapter.ViewHolder> {

    public interface OnJugadorActionListener {
        void onOpen(Jugador j);
        void onDelete(Jugador j, int position);
    }

    private final ArrayList<Jugador> data;
    private final OnJugadorActionListener listener;

    public JugadorAdapter(ArrayList<Jugador> data, OnJugadorActionListener listener) {
        this.data = data;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNombre, tvPosicion, tvTotales;
        ImageButton btnEliminar;

        public ViewHolder(View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFotoJugador);
            tvNombre = itemView.findViewById(R.id.tvNombreJugador);
            tvPosicion = itemView.findViewById(R.id.tvPosicion);
            tvTotales = itemView.findViewById(R.id.tvTotalesJugador);
            btnEliminar = itemView.findViewById(R.id.btnEliminarJugador);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_jugador, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        Jugador j = data.get(position);
        h.tvNombre.setText("#" + j.getDorsal() + " " + j.getNombre() + " " + j.getApellido());
        h.tvPosicion.setText(j.getPosicion());
        h.tvTotales.setText(String.format("G%d  A%d   %.2f€",
                j.getTotalGoles(), j.getTotalAsistencias(), j.getTotalCuotas()));

        // --- Carga robusta de imagen (file://, ruta local, content://, http/https) ---
        String foto = j.getFoto();
        if (foto != null && !foto.trim().isEmpty()) {
            try {
                String f = foto.trim();
                if (f.startsWith("file://")) {
                    // Ruta con esquema file://
                    Picasso.get()
                            .load(Uri.parse(f))
                            .fit().centerCrop()
                            .placeholder(R.drawable.ic_jugador)
                            .error(R.drawable.ic_jugador)
                            .into(h.ivFoto);
                } else if (f.startsWith("/")) {
                    // Ruta absoluta local sin esquema
                    Picasso.get()
                            .load(new File(f))
                            .fit().centerCrop()
                            .placeholder(R.drawable.ic_jugador)
                            .error(R.drawable.ic_jugador)
                            .into(h.ivFoto);
                } else {
                    // content:// o http(s)://
                    Picasso.get()
                            .load(Uri.parse(f))
                            .fit().centerCrop()
                            .placeholder(R.drawable.ic_jugador)
                            .error(R.drawable.ic_jugador)
                            .into(h.ivFoto);
                }
            } catch (Exception e) {
                h.ivFoto.setImageResource(R.drawable.ic_jugador);
            }
        } else {
            h.ivFoto.setImageResource(R.drawable.ic_jugador);
        }
        // ------------------------------------------------------------------------------

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(j);
        });

        if (h.btnEliminar != null) {
            h.btnEliminar.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(j, h.getBindingAdapterPosition());
            });
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    public void removeAt(int position) {
        if (position >= 0 && position < data.size()) {
            data.remove(position);
            notifyItemRemoved(position);
        }
    }
}
