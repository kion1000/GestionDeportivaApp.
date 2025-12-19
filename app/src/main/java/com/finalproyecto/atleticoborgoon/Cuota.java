package com.finalproyecto.atleticoborgoon;

public class Cuota {
    public int id;
    public int jugadorId;
    public String tipo;   // "Seguro", "1ª Cuota", "2ª Cuota", "3ª Cuota"
    public double cantidad;
    public String fecha;  // "YYYY-MM-DD"

    @Override public String toString() {
        return fecha + "  •  " + tipo + "  •  " + String.format("%.2f€", cantidad);
    }
}
