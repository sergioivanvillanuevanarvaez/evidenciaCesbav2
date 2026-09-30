package com.example.evidenciacesba;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    ImageView imgFoto;

    Button btnTomarFoto;
    Button btnOtraFoto;
    Button btnEliminarFoto;
    Button btnGuardar;
    Button btnCompartir;

    EditText edtTitulo;
    EditText edtDescripcion;

    TextView txtEstado;
    TextView txtContador;
    TextView txtFechaHora;

    LinearLayout galeria;

    ArrayList<String> fotos = new ArrayList<>();
    ArrayList<String> fechas = new ArrayList<>();

    int fotoSeleccionada = -1;

    Uri fotoUriActual;
    File archivoFotoActual;

    boolean camaraAbierta = false;

    SharedPreferences preferences;

    ActivityResultLauncher<Intent> camaraLauncher;
    ActivityResultLauncher<String> permisoCamaraLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // =========================
        // CONECTAR VISTAS
        // =========================

        imgFoto = findViewById(R.id.imgFoto);

        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnOtraFoto = findViewById(R.id.btnOtraFoto);
        btnEliminarFoto = findViewById(R.id.btnEliminarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCompartir = findViewById(R.id.btnCompartir);

        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);

        txtEstado = findViewById(R.id.txtEstado);
        txtContador = findViewById(R.id.txtContador);
        txtFechaHora = findViewById(R.id.txtFechaHora);

        galeria = findViewById(R.id.galeria);


        // =========================
        // PREFERENCIAS
        // =========================

        preferences =
                getSharedPreferences(
                        "EvidenciaCESBA",
                        MODE_PRIVATE
                );


        // =========================
        // RESULTADO DE LA CÁMARA
        // =========================

        camaraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        resultado -> {

                            camaraAbierta = false;

                            procesarResultadoCamara(
                                    resultado.getResultCode()
                            );
                        }
                );


        // =========================
        // PERMISO DE CÁMARA
        // =========================

        permisoCamaraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        concedido -> {

                            if (concedido) {

                                abrirCamara();

                            } else {

                                camaraAbierta = false;

                                Toast.makeText(
                                        MainActivity.this,
                                        "Se necesita permiso para usar la cámara",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );


        // =========================
        // CARGAR DATOS
        // =========================

        cargarDatos();


        // =========================
        // BOTONES
        // =========================

        btnTomarFoto.setOnClickListener(
                v -> abrirCamaraConPermiso()
        );

        btnOtraFoto.setOnClickListener(
                v -> abrirCamaraConPermiso()
        );

        btnEliminarFoto.setOnClickListener(
                v -> eliminarFoto()
        );

        btnGuardar.setOnClickListener(
                v -> guardarEvidencia()
        );

        btnCompartir.setOnClickListener(
                v -> compartirEvidencia()
        );
    }


    // =====================================================
    // PERMISO
    // =====================================================

    private void abrirCamaraConPermiso() {

        if (camaraAbierta) {

            Toast.makeText(
                    this,
                    "La cámara ya está abierta",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
        ) {

            abrirCamara();

        } else {

            permisoCamaraLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }


    // =====================================================
    // CREAR ARCHIVO / URI
    // =====================================================

    private boolean prepararFoto() {

        try {

            String nombre =
                    "EVIDENCIA_" +
                            new SimpleDateFormat(
                                    "yyyyMMdd_HHmmss_SSS",
                                    Locale.getDefault()
                            ).format(new Date()) +
                            ".jpg";


            // ==========================================
            // ANDROID 10 O SUPERIOR
            // ==========================================

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                ContentValues valores =
                        new ContentValues();

                valores.put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        nombre
                );

                valores.put(
                        MediaStore.Images.Media.MIME_TYPE,
                        "image/jpeg"
                );

                valores.put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES
                                + "/EvidenciaCESBA"
                );


                fotoUriActual =
                        getContentResolver().insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                valores
                        );


                if (fotoUriActual == null) {

                    return false;
                }


                return true;
            }


            // ==========================================
            // ANDROID 9 O INFERIOR
            // ==========================================

            File directorio =
                    new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES
                            ),
                            "EvidenciaCESBA"
                    );


            if (
                    !directorio.exists() &&
                            !directorio.mkdirs()
            ) {

                return false;
            }


            archivoFotoActual =
                    new File(
                            directorio,
                            nombre
                    );


            fotoUriActual =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".provider",
                            archivoFotoActual
                    );


            return true;

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error preparando fotografía:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }
    }


    // =====================================================
    // ABRIR CÁMARA
    // =====================================================

    private void abrirCamara() {

        if (camaraAbierta) {
            return;
        }


        camaraAbierta = true;


        // Preparar ubicación
        if (!prepararFoto()) {

            camaraAbierta = false;

            Toast.makeText(
                    this,
                    "No se pudo preparar la fotografía",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        try {

            Intent intent =
                    new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE
                    );


            intent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    fotoUriActual
            );


            // Dar permiso de lectura/escritura
            intent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
            );


            intent.setClipData(
                    android.content.ClipData.newRawUri(
                            "foto",
                            fotoUriActual
                    )
            );


            // Buscar aplicación de cámara
            android.content.pm.ResolveInfo camara =
                    getPackageManager()
                            .resolveActivity(
                                    intent,
                                    PackageManager.MATCH_DEFAULT_ONLY
                            );


            if (camara == null) {

                camaraAbierta = false;

                eliminarFotoTemporal();

                Toast.makeText(
                        this,
                        "No se encontró una aplicación de cámara",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }


            // Dar permisos directamente a la cámara
            try {

                grantUriPermission(
                        camara.activityInfo.packageName,
                        fotoUriActual,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

            } catch (Exception ignored) {
            }


            // Abrir cámara
            camaraLauncher.launch(intent);


        } catch (Exception e) {

            camaraAbierta = false;

            eliminarFotoTemporal();

            Toast.makeText(
                    this,
                    "Error al abrir la cámara:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =====================================================
    // PROCESAR RESULTADO
    // =====================================================

    private void procesarResultadoCamara(int resultado) {

        // Esperar un momento para que la cámara termine de escribir
        new android.os.Handler(
                android.os.Looper.getMainLooper()
        ).postDelayed(
                () -> verificarFotoGuardada(resultado),
                500
        );
    }


    // =====================================================
    // VERIFICAR FOTO
    // =====================================================

    private void verificarFotoGuardada(int resultado) {

        if (fotoUriActual == null) {

            mostrarErrorFoto();

            return;
        }


        boolean existe =
                fotoExiste(fotoUriActual);


        if (existe) {

            finalizarFotografia();

        } else {

            // Segundo intento después de esperar
            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).postDelayed(
                    () -> {

                        if (
                                fotoUriActual != null &&
                                        fotoExiste(fotoUriActual)
                        ) {

                            finalizarFotografia();

                        } else {

                            mostrarErrorFoto();
                        }

                    },
                    1000
            );
        }
    }


    // =====================================================
    // COMPROBAR SI REALMENTE EXISTE LA FOTO
    // =====================================================

    private boolean fotoExiste(Uri uri) {

        try {

            // ==========================================
            // ANDROID 10+
            // ==========================================

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                Cursor cursor =
                        getContentResolver().query(
                                uri,
                                new String[]{
                                        MediaStore.Images.Media.SIZE
                                },
                                null,
                                null,
                                null
                        );


                if (cursor != null) {

                    boolean existe = false;

                    if (cursor.moveToFirst()) {

                        int columna =
                                cursor.getColumnIndex(
                                        MediaStore.Images.Media.SIZE
                                );


                        if (columna >= 0) {

                            long tamaño =
                                    cursor.getLong(columna);

                            existe =
                                    tamaño > 0;
                        }
                    }

                    cursor.close();

                    return existe;
                }
            }


            // ==========================================
            // ANDROID 9-
            // ==========================================

            if (archivoFotoActual != null) {

                return archivoFotoActual.exists()
                        &&
                        archivoFotoActual.length() > 0;
            }


        } catch (Exception e) {

            return false;
        }


        return false;
    }


    // =====================================================
    // FINALIZAR FOTO
    // =====================================================

    private void finalizarFotografia() {

        try {

            // Agregar foto
            fotos.add(
                    fotoUriActual.toString()
            );


            // Fecha
            String fecha =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()
                    ).format(new Date());


            fechas.add(fecha);


            // Seleccionar última
            fotoSeleccionada =
                    fotos.size() - 1;


            // Guardar
            guardarFotos();


            // Mostrar
            mostrarFotoActual();

            actualizarGaleria();

            actualizarContador();


            txtEstado.setText(
                    "Fotografía guardada en Galería"
            );


            Toast.makeText(
                    this,
                    "Fotografía guardada correctamente",
                    Toast.LENGTH_SHORT
            ).show();


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error guardando la fotografía:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }


        fotoUriActual = null;
        archivoFotoActual = null;

        camaraAbierta = false;
    }


    // =====================================================
    // ERROR
    // =====================================================

    private void mostrarErrorFoto() {

        eliminarFotoTemporal();


        txtEstado.setText(
                "No se pudo guardar la fotografía"
        );


        Toast.makeText(
                this,
                "No se encontró la foto tomada",
                Toast.LENGTH_LONG
        ).show();
    }


    // =====================================================
    // ELIMINAR FOTO TEMPORAL
    // =====================================================

    private void eliminarFotoTemporal() {

        try {

            if (fotoUriActual != null) {

                getContentResolver().delete(
                        fotoUriActual,
                        null,
                        null
                );
            }

        } catch (Exception ignored) {
        }


        try {

            if (
                    archivoFotoActual != null &&
                            archivoFotoActual.exists()
            ) {

                archivoFotoActual.delete();
            }

        } catch (Exception ignored) {
        }


        fotoUriActual = null;
        archivoFotoActual = null;

        camaraAbierta = false;
    }


    // =====================================================
    // MOSTRAR FOTO
    // =====================================================

    private void mostrarFotoActual() {

        if (
                fotoSeleccionada < 0 ||
                        fotoSeleccionada >= fotos.size()
        ) {

            imgFoto.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );

            txtFechaHora.setText(
                    "Fecha y hora: --"
            );

            return;
        }


        try {

            Uri uri =
                    Uri.parse(
                            fotos.get(
                                    fotoSeleccionada
                            )
                    );


            imgFoto.setImageURI(uri);


            if (
                    fotoSeleccionada < fechas.size()
            ) {

                txtFechaHora.setText(
                        "Fecha y hora: "
                                + fechas.get(
                                fotoSeleccionada
                        )
                );
            }

        } catch (Exception e) {

            imgFoto.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );
        }
    }


    // =====================================================
    // ELIMINAR FOTO
    // =====================================================

    private void eliminarFoto() {

        if (fotos.isEmpty()) {

            Toast.makeText(
                    this,
                    "No hay fotografías para eliminar",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (fotoSeleccionada < 0) {

            fotoSeleccionada =
                    fotos.size() - 1;
        }


        try {

            Uri uri =
                    Uri.parse(
                            fotos.get(
                                    fotoSeleccionada
                            )
                    );


            getContentResolver().delete(
                    uri,
                    null,
                    null
            );

        } catch (Exception ignored) {
        }


        fotos.remove(
                fotoSeleccionada
        );


        if (
                fotoSeleccionada <
                        fechas.size()
        ) {

            fechas.remove(
                    fotoSeleccionada
            );
        }


        if (fotos.isEmpty()) {

            fotoSeleccionada = -1;

        } else if (
                fotoSeleccionada >= fotos.size()
        ) {

            fotoSeleccionada =
                    fotos.size() - 1;
        }


        guardarFotos();

        mostrarFotoActual();

        actualizarGaleria();

        actualizarContador();


        txtEstado.setText(
                "Fotografía eliminada"
        );


        Toast.makeText(
                this,
                "Fotografía eliminada",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =====================================================
    // CONTADOR
    // =====================================================

    private void actualizarContador() {

        txtContador.setText(
                "Fotografías: "
                        + fotos.size()
        );
    }


    // =====================================================
    // GALERÍA
    // =====================================================

    private void actualizarGaleria() {

        galeria.removeAllViews();


        for (
                int i = 0;
                i < fotos.size();
                i++
        ) {

            final int posicion = i;


            ImageView miniatura =
                    new ImageView(this);


            LinearLayout.LayoutParams parametros =
                    new LinearLayout.LayoutParams(
                            100,
                            100
                    );


            parametros.setMargins(
                    8,
                    4,
                    8,
                    4
            );


            miniatura.setLayoutParams(
                    parametros
            );


            miniatura.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );


            try {

                Uri uri =
                        Uri.parse(
                                fotos.get(i)
                        );


                miniatura.setImageURI(uri);

            } catch (Exception e) {

                miniatura.setImageResource(
                        android.R.drawable.ic_menu_gallery
                );
            }


            miniatura.setOnClickListener(
                    v -> {

                        fotoSeleccionada =
                                posicion;

                        mostrarFotoActual();

                        txtEstado.setText(
                                "Fotografía "
                                        + (posicion + 1)
                                        + " seleccionada"
                        );
                    }
            );


            galeria.addView(
                    miniatura
            );
        }
    }


    // =====================================================
    // GUARDAR FOTOS
    // =====================================================

    private void guardarFotos() {

        SharedPreferences.Editor editor =
                preferences.edit();


        editor.putInt(
                "cantidadFotos",
                fotos.size()
        );


        for (
                int i = 0;
                i < fotos.size();
                i++
        ) {

            editor.putString(
                    "foto_" + i,
                    fotos.get(i)
            );
        }


        for (
                int i = 0;
                i < fechas.size();
                i++
        ) {

            editor.putString(
                    "fecha_" + i,
                    fechas.get(i)
            );
        }


        editor.apply();
    }


    // =====================================================
    // CARGAR FOTOS
    // =====================================================

    private void cargarDatos() {

        String titulo =
                preferences.getString(
                        "titulo",
                        ""
                );


        String descripcion =
                preferences.getString(
                        "descripcion",
                        ""
                );


        edtTitulo.setText(titulo);

        edtDescripcion.setText(
                descripcion
        );


        cargarFotos();
    }


    private void cargarFotos() {

        int cantidad =
                preferences.getInt(
                        "cantidadFotos",
                        0
                );


        fotos.clear();

        fechas.clear();


        for (
                int i = 0;
                i < cantidad;
                i++
        ) {

            String uri =
                    preferences.getString(
                            "foto_" + i,
                            ""
                    );


            String fecha =
                    preferences.getString(
                            "fecha_" + i,
                            "--"
                    );


            if (!uri.isEmpty()) {

                try {

                    Uri uriFoto =
                            Uri.parse(uri);


                    android.content.res.AssetFileDescriptor descriptor =
                            getContentResolver()
                                    .openAssetFileDescriptor(
                                            uriFoto,
                                            "r"
                                    );


                    if (descriptor != null) {

                        descriptor.close();

                        fotos.add(uri);

                        fechas.add(fecha);
                    }

                } catch (Exception ignored) {
                }
            }
        }


        if (!fotos.isEmpty()) {

            fotoSeleccionada =
                    fotos.size() - 1;

            mostrarFotoActual();
        }


        actualizarContador();

        actualizarGaleria();
    }


    // =====================================================
    // GUARDAR EVIDENCIA
    // =====================================================

    private void guardarEvidencia() {

        String titulo =
                edtTitulo
                        .getText()
                        .toString()
                        .trim();


        String descripcion =
                edtDescripcion
                        .getText()
                        .toString()
                        .trim();


        if (titulo.isEmpty()) {

            edtTitulo.setError(
                    "Escribe un título"
            );

            edtTitulo.requestFocus();

            return;
        }


        if (fotos.isEmpty()) {

            Toast.makeText(
                    this,
                    "Debes tomar al menos una fotografía",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        SharedPreferences.Editor editor =
                preferences.edit();


        editor.putString(
                "titulo",
                titulo
        );


        editor.putString(
                "descripcion",
                descripcion
        );


        editor.apply();


        txtEstado.setText(
                "Evidencia guardada correctamente"
        );


        Toast.makeText(
                this,
                "Evidencia guardada",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =====================================================
    // COMPARTIR
    // =====================================================

    private void compartirEvidencia() {

        if (fotos.isEmpty()) {

            Toast.makeText(
                    this,
                    "No hay fotografías para compartir",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (fotoSeleccionada < 0) {

            fotoSeleccionada =
                    fotos.size() - 1;
        }


        try {

            Uri uri =
                    Uri.parse(
                            fotos.get(
                                    fotoSeleccionada
                            )
                    );


            String titulo =
                    edtTitulo
                            .getText()
                            .toString()
                            .trim();


            String descripcion =
                    edtDescripcion
                            .getText()
                            .toString()
                            .trim();


            Intent compartir =
                    new Intent(
                            Intent.ACTION_SEND
                    );


            compartir.setType(
                    "image/jpeg"
            );


            compartir.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );


            compartir.putExtra(
                    Intent.EXTRA_TEXT,
                    "Evidencia CESBA\n\n"
                            + "Título: "
                            + titulo
                            + "\nDescripción: "
                            + descripcion
            );


            compartir.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );


            startActivity(
                    Intent.createChooser(
                            compartir,
                            "Compartir evidencia"
                    )
            );


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "No se pudo compartir la fotografía",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =====================================================
    // RESUME
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (galeria != null) {

            actualizarGaleria();
        }
    }
}