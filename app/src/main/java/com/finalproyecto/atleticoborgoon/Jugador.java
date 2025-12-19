package com.finalproyecto.atleticoborgoon;

public class Jugador {
    private int id;
    private String nombre;
    private String apellido;
    private int dorsal;
    private String posicion;
    private String foto;

    // Campos calculados para el listado
    private int totalGoles;
    private int totalAsistencias;
    private double totalCuotas;

    public Jugador() {}

    // Getters / Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public int getDorsal() { return dorsal; }
    public void setDorsal(int dorsal) { this.dorsal = dorsal; }
    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public int getTotalGoles() { return totalGoles; }
    public void setTotalGoles(int totalGoles) { this.totalGoles = totalGoles; }
    public int getTotalAsistencias() { return totalAsistencias; }
    public void setTotalAsistencias(int totalAsistencias) { this.totalAsistencias = totalAsistencias; }
    public double getTotalCuotas() { return totalCuotas; }
    public void setTotalCuotas(double totalCuotas) { this.totalCuotas = totalCuotas; }
}
