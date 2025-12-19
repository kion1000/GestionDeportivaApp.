package com.finalproyecto.atleticoborgoon.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class Repo {
    private final DBHelper helper;

    public Repo(Context ctx) { helper = new DBHelper(ctx); }

    // ======== MODELOS SENCILLOS ========
    public static class Equipo {
        public int id; public String nombre;
        @Override public String toString() { return nombre; }
    }

    public static class Encuentro {
        public int id;
        public String fecha; // 'YYYY-MM-DD'
        public int rivalId;
        public String rivalNombre;
        public int golesFavor, golesContra;
        public String competicion, lugar, notas;
    }

    public static class EstadoCobroRow {
        public int jugadorId; public String jugador;
        public int cobroId; public String cobroNombre; public String temporada;
        public double importeTotal, pagado, pendiente;
    }

    // ======== EQUIPOS ========
    public int addEquipoIfNotExists(String nombre) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre.trim());
        long id = -1;
        try {
            id = db.insertOrThrow("Equipos", null, cv);
        } catch (Exception e) {
            // ya existía => recuperamos id
            Cursor c = db.rawQuery("SELECT id FROM Equipos WHERE nombre=?", new String[]{nombre.trim()});
            try { if (c.moveToFirst()) id = c.getInt(0); } finally { c.close(); }
        }
        return (int) id;
    }

    public ArrayList<Equipo> listEquipos() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, nombre FROM Equipos ORDER BY nombre", null);
        ArrayList<Equipo> res = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                Equipo e = new Equipo();
                e.id = c.getInt(0); e.nombre = c.getString(1);
                res.add(e);
            }
        } finally { c.close(); }
        return res;
    }

    // ======== ENCUENTROS (partidos del equipo) ========
    public int upsertEncuentro(Integer id, String fecha, int rivalId, int gf, int gc, String competicion, String lugar, String notas) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("fecha", fecha);
        cv.put("rival_id", rivalId);
        cv.put("goles_favor", gf);
        cv.put("goles_contra", gc);
        cv.put("competicion", competicion);
        cv.put("lugar", lugar);
        cv.put("notas", notas);
        if (id == null) {
            long newId = db.insert("Encuentros", null, cv);
            return (int) newId;
        } else {
            db.update("Encuentros", cv, "id=?", new String[]{String.valueOf(id)});
            return id;
        }
    }

    public ArrayList<Encuentro> listEncuentros() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.id, e.fecha, e.rival_id, r.nombre, e.goles_favor, e.goles_contra, e.competicion, e.lugar, e.notas " +
                        "FROM Encuentros e JOIN Equipos r ON r.id=e.rival_id " +
                        "ORDER BY e.fecha DESC, e.id DESC", null);
        ArrayList<Encuentro> res = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                Encuentro en = new Encuentro();
                en.id = c.getInt(0); en.fecha = c.getString(1);
                en.rivalId = c.getInt(2); en.rivalNombre = c.getString(3);
                en.golesFavor = c.getInt(4); en.golesContra = c.getInt(5);
                en.competicion = c.getString(6); en.lugar = c.getString(7);
                en.notas = c.getString(8);
                res.add(en);
            }
        } finally { c.close(); }
        return res;
    }

    // ======== PARTIDOS (estadística por jugador enlazada a encuentro) ========
    public int addEstadisticaJugador(int jugadorId, int encuentroId, int goles, int asistencias, int tarjetas) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("jugador_id", jugadorId);
        cv.put("encuentro_id", encuentroId);
        cv.put("goles", goles);
        cv.put("asistencias", asistencias);
        cv.put("tarjetas", tarjetas);
        cv.put("fecha", (String) null); // opcional, ya tenemos fecha en Encuentros
        long id = db.insert("Partidos", null, cv);
        return (int) id;
    }

    // ======== COBROS (conceptos de temporada) ========
    public int upsertCobro(Integer id, String nombre, String temporada, double importeTotal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre.trim());
        cv.put("temporada", temporada);
        cv.put("importe_total", importeTotal);
        if (id == null) {
            long newId = db.insertWithOnConflict("Cobros", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            if (newId == -1) {
                Cursor c = db.rawQuery("SELECT id FROM Cobros WHERE nombre=? AND temporada=?", new String[]{nombre.trim(), temporada});
                try { if (c.moveToFirst()) return c.getInt(0); } finally { c.close(); }
            }
            return (int) newId;
        } else {
            db.update("Cobros", cv, "id=?", new String[]{String.valueOf(id)});
            return id;
        }
    }

    public ArrayList<String> listTemporadasCobros() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT DISTINCT temporada FROM Cobros ORDER BY temporada DESC", null);
        ArrayList<String> res = new ArrayList<>();
        try { while (c.moveToNext()) res.add(c.getString(0)); } finally { c.close(); }
        return res;
    }

    public ArrayList<EstadoCobroRow> estadoCobroPorConcepto(int cobroId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT jugador_id, jugador, cobro_id, nombre, temporada, importe_total, pagado, pendiente " +
                        "FROM vw_estado_cobro WHERE cobro_id=? ORDER BY jugador COLLATE NOCASE",
                new String[]{String.valueOf(cobroId)});
        ArrayList<EstadoCobroRow> res = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                EstadoCobroRow r = new EstadoCobroRow();
                r.jugadorId = c.getInt(0);
                r.jugador = c.getString(1);
                r.cobroId = c.getInt(2);
                r.cobroNombre = c.getString(3);
                r.temporada = c.getString(4);
                r.importeTotal = c.getDouble(5);
                r.pagado = c.getDouble(6);
                r.pendiente = c.getDouble(7);
                res.add(r);
            }
        } finally { c.close(); }
        return res;
    }


    public boolean insertarAbono(int jugadorId, int cobroId, double importe, String fecha) {
        var db = helper.getWritableDatabase();

        String tipo = null;
        var c = db.rawQuery("SELECT nombre FROM Cobros WHERE id=?", new String[]{ String.valueOf(cobroId) });
        try { if (c.moveToFirst()) tipo = c.getString(0); } finally { c.close(); }

        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put("jugador_id", jugadorId);
        cv.put("cobro_id", cobroId);
        cv.put("cantidad", importe);
        cv.put("fecha", fecha);
        if (tipo != null) cv.put("tipo", tipo); // compatibilidad con pantallas antiguas

        long id = db.insert("Cuotas", null, cv);
        return id > 0;
    }

    // En Repo.java
    public boolean deleteEncuentro(int encuentroId) {
        var db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Borramos primero las stats por jugador de ese encuentro
            db.delete("Partidos", "encuentro_id=?", new String[]{ String.valueOf(encuentroId) });
            // Luego el encuentro
            int rows = db.delete("Encuentros", "id=?", new String[]{ String.valueOf(encuentroId) });
            db.setTransactionSuccessful();
            return rows > 0;
        } finally {
            db.endTransaction();
        }
    }



}
