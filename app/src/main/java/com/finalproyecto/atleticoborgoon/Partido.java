package com.finalproyecto.atleticoborgoon;
public class Partido {
    public int id;
    public int jugadorId;
    public int goles;
    public int asistencias;
    public String tarjetas;
    public String fecha;
    public int encuentroId;    // <-- NUEVO
    // public String rivalNombre; // (opcional) si quieres mostrarlo
    @Override public String toString() {
        // Muestra algo útil en la lista:
        return (fecha != null ? fecha + " · " : "") + "G:" + goles + " A:" + asistencias + " (" + tarjetas + ")";
    }
}
