package com.finalproyecto.atleticoborgoon;
import com.finalproyecto.atleticoborgoon.data.DBHelper;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.io.File;

// ==== Edge-to-edge + insets ====
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.view.View;
// ===============================

public class EditJugadorActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private int jugadorId = -1;
    private EditText etNombre, etApellido, etDorsal, etPosicion;
    private ImageView ivFoto;
    private String fotoUri = "";

    // OpenDocument (API 19+) → permite persistir permiso de lectura
    private final ActivityResultLauncher<String[]> pickImageOpenDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        // Persistimos permiso de lectura para que Picasso pueda leerla siempre
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException ignored) { /* algunos proveedores no lo permiten */ }
                    fotoUri = uri.toString();
                    Picasso.get().load(uri).fit().centerCrop().into(ivFoto);
                }
            });

    // Fallback para API < 19
    private final ActivityResultLauncher<String> pickImageGetContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    fotoUri = uri.toString();
                    Picasso.get().load(uri).fit().centerCrop().into(ivFoto);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---- EDGE-TO-EDGE + INSETS (Opción A) ----
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_jugador);
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
        // ------------------------------------------

        dbHelper   = new DBHelper(this);
        ivFoto     = findViewById(R.id.ivFoto);
        etNombre   = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etDorsal   = findViewById(R.id.etDorsal);
        etPosicion = findViewById(R.id.etPosicion);
        Button btnElegirFoto = findViewById(R.id.btnElegirFoto);
        Button btnGuardar    = findViewById(R.id.btnGuardar);

        jugadorId = getIntent().getIntExtra("jugador_id", -1);
        if (jugadorId != -1) cargarJugador();

        btnElegirFoto.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                pickImageOpenDocument.launch(new String[]{"image/*"});
            } else {
                pickImageGetContent.launch("image/*");
            }
        });

        btnGuardar.setOnClickListener(v -> guardar());
    }

    private void cargarJugador() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT nombre, apellido, dorsal, posicion, foto FROM Jugadores WHERE id=?",
                new String[]{ String.valueOf(jugadorId) }
        );
        try {
            if (c.moveToFirst()) {
                etNombre.setText(c.getString(0));
                etApellido.setText(c.getString(1));
                etDorsal.setText(String.valueOf(c.getInt(2)));
                etPosicion.setText(c.getString(3));

                fotoUri = c.getString(4);
                if (fotoUri != null && !fotoUri.trim().isEmpty()) {
                    cargarImagenRobusta(fotoUri);
                } else {
                    ivFoto.setImageResource(R.drawable.ic_jugador);
                }
            }
        } finally {
            c.close();
        }
    }

    private void cargarImagenRobusta(String valor) {
        try {
            String f = valor.trim();
            if (f.startsWith("file://")) {
                Picasso.get().load(Uri.parse(f)).fit().centerCrop().into(ivFoto);
            } else if (f.startsWith("/")) {
                Picasso.get().load(new File(f)).fit().centerCrop().into(ivFoto);
            } else {
                // content:// o http(s)://
                Picasso.get().load(Uri.parse(f)).fit().centerCrop().into(ivFoto);
            }
        } catch (Exception e) {
            ivFoto.setImageResource(R.drawable.ic_jugador);
        }
    }

    private void guardar() {
        String nombre   = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String dorsalStr= etDorsal.getText().toString().trim();
        String posicion = etPosicion.getText().toString().trim();

        int dorsal = 0;
        try { if (!dorsalStr.isEmpty()) dorsal = Integer.parseInt(dorsalStr); }
        catch (NumberFormatException ignored) {}

        if (nombre.isEmpty() || apellido.isEmpty()) {
            Toast.makeText(this, "Nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        cv.put("apellido", apellido);
        cv.put("dorsal", dorsal);
        cv.put("posicion", posicion);
        cv.put("foto", fotoUri == null ? "" : fotoUri);

        if (jugadorId == -1) {
            db.insert("Jugadores", null, cv);
            Toast.makeText(this, "Jugador añadido", Toast.LENGTH_SHORT).show();
        } else {
            db.update("Jugadores", cv, "id=?", new String[]{ String.valueOf(jugadorId) });
            Toast.makeText(this, "Jugador actualizado", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
