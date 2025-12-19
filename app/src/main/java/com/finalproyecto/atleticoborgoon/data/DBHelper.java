package com.finalproyecto.atleticoborgoon.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "borgo.db";
    // v6: columnas amarillas/rojas en Partidos + backfill
    public static final int DB_VERSION = 6;

    public DBHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // === Jugadores ===
        db.execSQL("CREATE TABLE IF NOT EXISTS Jugadores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "apellido TEXT," +
                "dorsal INTEGER," +
                "posicion TEXT," +
                "telefono TEXT," +
                "foto TEXT," +
                "activo INTEGER DEFAULT 1," +
                "observaciones TEXT DEFAULT ''" +
                ")");

        // === Partidos (stats por jugador) ===
        db.execSQL("CREATE TABLE IF NOT EXISTS Partidos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "jugador_id INTEGER NOT NULL," +
                "goles INTEGER DEFAULT 0," +
                "asistencias INTEGER DEFAULT 0," +
                "tarjetas TEXT," +                 // compat
                "amarillas INTEGER DEFAULT 0," +   // v6
                "rojas INTEGER DEFAULT 0," +       // v6
                "fecha TEXT," +                    // compat (cuando no hay encuentro_id)
                "encuentro_id INTEGER," +          // enlace a Encuentros
                "FOREIGN KEY(jugador_id) REFERENCES Jugadores(id) ON DELETE CASCADE" +
                ")");

        // === Equipos (rivales) ===
        db.execSQL("CREATE TABLE IF NOT EXISTS Equipos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL UNIQUE" +
                ")");

        // === Encuentros (partidos del equipo) ===
        db.execSQL("CREATE TABLE IF NOT EXISTS Encuentros (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "fecha TEXT NOT NULL," +                   // 'YYYY-MM-DD'
                "rival_id INTEGER NOT NULL," +
                "goles_favor INTEGER DEFAULT 0," +
                "goles_contra INTEGER DEFAULT 0," +
                "competicion TEXT," +
                "lugar TEXT," +                            // CASA/FUERA o libre
                "notas TEXT," +
                "FOREIGN KEY(rival_id) REFERENCES Equipos(id) ON DELETE RESTRICT" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_encuentros_fecha ON Encuentros(fecha DESC)");

        // === Cobros (conceptos por temporada) ===
        db.execSQL("CREATE TABLE IF NOT EXISTS Cobros (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +                  // 'Seguro', 'Cuota 1', ...
                "temporada TEXT," +                        // '2025/26'
                "importe_total REAL NOT NULL," +
                "UNIQUE(nombre, temporada)" +
                ")");

        // === Cuotas (histórico de abonos) ===
        db.execSQL("CREATE TABLE IF NOT EXISTS Cuotas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "jugador_id INTEGER NOT NULL," +
                "tipo TEXT," +                 // compat con pantallas viejas
                "cantidad REAL NOT NULL," +
                "fecha TEXT," +
                "cobro_id INTEGER," +          // concepto (nullable para datos antiguos)
                "FOREIGN KEY(jugador_id) REFERENCES Jugadores(id) ON DELETE CASCADE," +
                "FOREIGN KEY(cobro_id)  REFERENCES Cobros(id)     ON DELETE CASCADE" +
                ")");

        // Trigger que rellena 'tipo' con el nombre del cobro si viene null
        createCuotasTipoTrigger(db);

        // Vista de estado (pagado/pendiente por jugador y concepto) con pendiente truncado a 0
        createEstadoCobroView(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v2: campo observaciones en Jugadores
        if (oldVersion < 2) {
            safeExec(db, "ALTER TABLE Jugadores ADD COLUMN observaciones TEXT DEFAULT ''");
        }
        // v3: Equipos/Encuentros e índice + encuentro_id en Partidos
        if (oldVersion < 3) {
            safeExec(db, "CREATE TABLE IF NOT EXISTS Equipos (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE)");
            safeExec(db, "CREATE TABLE IF NOT EXISTS Encuentros (id INTEGER PRIMARY KEY AUTOINCREMENT, fecha TEXT NOT NULL, rival_id INTEGER NOT NULL, goles_favor INTEGER DEFAULT 0, goles_contra INTEGER DEFAULT 0, competicion TEXT, lugar TEXT, notas TEXT, FOREIGN KEY(rival_id) REFERENCES Equipos(id) ON DELETE RESTRICT)");
            safeExec(db, "CREATE INDEX IF NOT EXISTS idx_encuentros_fecha ON Encuentros(fecha DESC)");
            safeExec(db, "ALTER TABLE Partidos ADD COLUMN encuentro_id INTEGER");
        }
        // v4: Cobros, cobro_id en Cuotas, vista vw_estado_cobro
        if (oldVersion < 4) {
            safeExec(db, "CREATE TABLE IF NOT EXISTS Cobros (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, temporada TEXT, importe_total REAL NOT NULL, UNIQUE(nombre, temporada))");
            safeExec(db, "ALTER TABLE Cuotas ADD COLUMN cobro_id INTEGER");
            createEstadoCobroView(db);
        }
        // v5: trigger de tipo y backfill de tipo desde Cobros
        if (oldVersion < 5) {
            createCuotasTipoTrigger(db);
            createEstadoCobroView(db);
            safeExec(db, "UPDATE Cuotas " +
                    "SET tipo = (SELECT nombre FROM Cobros c WHERE c.id = Cuotas.cobro_id) " +
                    "WHERE tipo IS NULL AND cobro_id IS NOT NULL");
        }
        // v6: columnas amarillas/rojas en Partidos + backfill desde 'tarjetas'
        if (oldVersion < 6) {
            safeExec(db, "ALTER TABLE Partidos ADD COLUMN amarillas INTEGER DEFAULT 0");
            safeExec(db, "ALTER TABLE Partidos ADD COLUMN rojas INTEGER DEFAULT 0");
            // Migración básica desde texto:
            safeExec(db, "UPDATE Partidos SET amarillas = 1 WHERE LOWER(IFNULL(tarjetas,'')) = 'amarilla'");
            safeExec(db, "UPDATE Partidos SET rojas = 1 WHERE LOWER(IFNULL(tarjetas,'')) = 'roja'");
        }
    }

    private void createCuotasTipoTrigger(SQLiteDatabase db) {
        safeExec(db, "DROP TRIGGER IF EXISTS trg_cuotas_set_tipo");
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_cuotas_set_tipo " +
                        "AFTER INSERT ON Cuotas " +
                        "FOR EACH ROW " +
                        "WHEN NEW.cobro_id IS NOT NULL AND NEW.tipo IS NULL " +
                        "BEGIN " +
                        "  UPDATE Cuotas " +
                        "  SET tipo = (SELECT nombre FROM Cobros WHERE id = NEW.cobro_id) " +
                        "  WHERE id = NEW.id; " +
                        "END"
        );
    }

    private void createEstadoCobroView(SQLiteDatabase db) {
        safeExec(db, "DROP VIEW IF EXISTS vw_estado_cobro");
        db.execSQL(
                "CREATE VIEW vw_estado_cobro AS " +
                        "SELECT j.id AS jugador_id, (j.nombre || ' ' || IFNULL(j.apellido,'')) AS jugador, " +
                        "c.id AS cobro_id, c.nombre, c.temporada, c.importe_total, " +
                        "IFNULL(SUM(q.cantidad),0) AS pagado, " +
                        "CASE WHEN (c.importe_total - IFNULL(SUM(q.cantidad),0)) < 0 THEN 0 " +
                        "     ELSE (c.importe_total - IFNULL(SUM(q.cantidad),0)) END AS pendiente " +
                        "FROM Cobros c CROSS JOIN Jugadores j " +
                        "LEFT JOIN Cuotas q ON q.jugador_id=j.id AND q.cobro_id=c.id " +
                        "GROUP BY j.id, c.id"
        );
    }

    private void safeExec(SQLiteDatabase db, String sql) {
        try { db.execSQL(sql); } catch (Exception ignore) {}
    }
}
