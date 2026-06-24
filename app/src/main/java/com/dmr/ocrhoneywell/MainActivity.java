package com.dmr.ocrhoneywell;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaScannerConnection;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;


import android.widget.Spinner;
import android.widget.ArrayAdapter;
import java.util.ArrayList;


import java.io.File;
import java.io.FileWriter;

import android.content.Intent;
import android.net.Uri;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    private FirebaseFirestore db;

    private EditText txtCedula, txtFecha, txtApellido1, txtApellido2, txtNombre, txtFechaRegistro, txtHoraRegistro;
    private Button btnGuardar, btnLimpiar, btnSalir, btnMenu;

    private Spinner spnPersonaVisitable;
    private final ArrayList<String> personasIds = new ArrayList<>();
    private final ArrayList<String> personasNombres = new ArrayList<>();

    private final StringBuilder bufferOCR = new StringBuilder();

    private static final int REQUEST_EXPORTAR_CSV = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        txtCedula = findViewById(R.id.txtCedula);
        txtFecha = findViewById(R.id.txtFecha);
        txtApellido1 = findViewById(R.id.txtApellido1);
        txtApellido2 = findViewById(R.id.txtApellido2);
        txtNombre = findViewById(R.id.txtNombre);
        txtFechaRegistro = findViewById(R.id.txtFechaRegistro);
        txtHoraRegistro = findViewById(R.id.txtHoraRegistro);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnLimpiar = findViewById(R.id.btnLimpiar);
        btnSalir = findViewById(R.id.btnSalir);
        btnMenu = findViewById(R.id.btnMenu);

        spnPersonaVisitable = findViewById(R.id.spnPersonaVisitable);
        cargarPersonasVisitables();

        actualizarFechaHora();

        btnGuardar.setOnClickListener(v -> guardarCSV());
        btnLimpiar.setOnClickListener(v -> limpiarCampos());
        btnSalir.setOnClickListener(v -> finish());

        btnMenu.setOnClickListener(v -> mostrarMenuConfiguracion());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return super.dispatchKeyEvent(event);

        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            procesarLectura(bufferOCR.toString());
            bufferOCR.setLength(0);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_TAB) {
            bufferOCR.append("\t");
            return true;
        }

        char c = (char) event.getUnicodeChar();
        if (c != 0) {
            bufferOCR.append(c);
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private void procesarLectura(String lectura) {
        limpiarCamposSinMensaje();

        String[] partes = lectura.split("\\t");

        if (partes.length >= 5) {
            txtCedula.setText(partes[0].trim());
            txtFecha.setText(formatearFechaNacimiento(partes[1].trim()));
            txtApellido1.setText(capitalizar(partes[2].trim()));
            txtApellido2.setText(capitalizar(partes[3].trim()));
            txtNombre.setText(capitalizar(partes[4].trim()));
            actualizarFechaHora();

            Toast.makeText(this, "Lectura procesada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Formato incompleto: " + lectura, Toast.LENGTH_LONG).show();
        }
    }
    private void guardarCSV() {
        if (txtCedula.getText().toString().trim().isEmpty()) {
            bufferOCR.setLength(0);
            Toast.makeText(this, "No hay datos para guardar", Toast.LENGTH_SHORT).show();
            return;
        }


        if (spnPersonaVisitable.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Seleccione persona a visitar", Toast.LENGTH_SHORT).show();
            return;
        }


        try {
            File dir = new File(getExternalFilesDir(null), "export");
            if (!dir.exists()) dir.mkdirs();

            File archivo = new File(dir, "datos_ocr.csv");
            boolean nuevo = !archivo.exists() || archivo.length() == 0;

            FileWriter fw = new FileWriter(archivo, true);

            if (nuevo) {
                fw.write("cedula,fecha_nacimiento,apellido1,apellido2,nombre,fecha_lectura,hora_lectura\n");
            }

            fw.write(
                    limpiarCSV(txtCedula.getText().toString()) + "," +
                            limpiarCSV(txtFecha.getText().toString()) + "," +
                            limpiarCSV(txtApellido1.getText().toString()) + "," +
                            limpiarCSV(txtApellido2.getText().toString()) + "," +
                            limpiarCSV(txtNombre.getText().toString()) + "," +
                            limpiarCSV(txtFechaRegistro.getText().toString()) + "," +
                            limpiarCSV(txtHoraRegistro.getText().toString()) + "\n"
            );

            fw.close();

            guardarFirebase();

            bufferOCR.setLength(0);
            limpiarCamposSinMensaje();

            Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show();


        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void guardarFirebase() {
        Map<String, Object> visita = new HashMap<>();

        String nombreCompleto = txtNombre.getText().toString().trim();
        String apellidoCompleto = (
                txtApellido1.getText().toString().trim() + " " +
                        txtApellido2.getText().toString().trim()
        ).trim();

        String mesAnio = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        visita.put("visitanteNombre", nombreCompleto);
        visita.put("visitanteApellido", apellidoCompleto);
        visita.put("visitanteCedula", txtCedula.getText().toString().trim());
        visita.put("visitanteFechaNacimiento", txtFecha.getText().toString().trim());

        visita.put("fechaLectura", txtFechaRegistro.getText().toString().trim());
        visita.put("horaLectura", txtHoraRegistro.getText().toString().trim());
        visita.put("fecha", new Date());
        visita.put("mesAnio", mesAnio);

        visita.put("organizacionId", "dmr");

        int pos = spnPersonaVisitable.getSelectedItemPosition();


        String personaId = "";
        String personaNombre = "";

        if (pos > 0) {
            personaId = personasIds.get(pos);
            personaNombre = personasNombres.get(pos);
        }

        visita.put("personaVisitableId", personaId);
        visita.put("personaVisitableNombre", personaNombre);



        visita.put("origen", "OCR Honeywell");
        visita.put("dispositivo", "tablet-ocr-01");

        db.collection("visitas")
                .add(visita)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Visita guardada en Firebase", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void mostrarMenuConfiguracion() {
        new AlertDialog.Builder(this)
                .setTitle("Configuración")
                .setItems(new CharSequence[]{"Exportar a Downloads", "Elegir ubicación", "Vaciar lote interno"}, (dialog, which) -> {
                    if (which == 0) {
                        exportarDatosDownloads();
                    } else if (which == 1) {
                        elegirUbicacionExportacion();
                    } else {
                        confirmarVaciarLote();
                    }
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void exportarDatosDownloads() {
        try {
            File origen = new File(getExternalFilesDir(null), "export/datos_ocr.csv");

            if (!origen.exists() || !csvTieneDatos(origen)) {
                Toast.makeText(this, "No hay datos guardados para exportar", Toast.LENGTH_LONG).show();
                return;
            }

            String fechaHora = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String nombreArchivo = "datos_ocr_" + fechaHora + ".csv";

            File carpetaDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!carpetaDownloads.exists()) carpetaDownloads.mkdirs();

            File destino = new File(carpetaDownloads, nombreArchivo);

            copiarArchivo(origen, destino);

            MediaScannerConnection.scanFile(
                    this,
                    new String[]{destino.getAbsolutePath()},
                    new String[]{"text/csv"},
                    null
            );

            Toast.makeText(this, "Copia creada en: " + destino.getAbsolutePath(), Toast.LENGTH_LONG).show();


        } catch (Throwable e) {
            Toast.makeText(this, "Error exportando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void elegirUbicacionExportacion() {
        String fechaHora = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreArchivo = "datos_ocr_" + fechaHora + ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, nombreArchivo);
        startActivityForResult(intent, REQUEST_EXPORTAR_CSV);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EXPORTAR_CSV && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            exportarAUri(uri);
        }
    }

    private void exportarAUri(Uri uri) {
        try {
            File origen = new File(getExternalFilesDir(null), "export/datos_ocr.csv");

            if (!origen.exists() || !csvTieneDatos(origen)) {
                Toast.makeText(this, "No hay datos guardados para exportar", Toast.LENGTH_LONG).show();
                return;
            }

            OutputStream salida = getContentResolver().openOutputStream(uri);
            FileInputStream entrada = new FileInputStream(origen);

            byte[] buffer = new byte[4096];
            int leidos;

            while ((leidos = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, leidos);
            }

            entrada.close();
            salida.close();

            Toast.makeText(this, "CSV exportado correctamente", Toast.LENGTH_LONG).show();



        } catch (Throwable e) {
            Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copiarArchivo(File origen, File destino) throws Exception {
        FileInputStream entrada = new FileInputStream(origen);
        FileOutputStream salida = new FileOutputStream(destino);

        byte[] buffer = new byte[4096];
        int leidos;

        while ((leidos = entrada.read(buffer)) != -1) {
            salida.write(buffer, 0, leidos);
        }

        entrada.close();
        salida.close();
    }

    private void limpiarCampos() {
        bufferOCR.setLength(0);
        limpiarCamposSinMensaje();
        txtCedula.requestFocus();
        Toast.makeText(this, "Campos limpios", Toast.LENGTH_SHORT).show();
    }

    private void limpiarCamposSinMensaje() {
        txtCedula.setText("");
        txtFecha.setText("");
        txtApellido1.setText("");
        txtApellido2.setText("");
        txtNombre.setText("");
        actualizarFechaHora();

        spnPersonaVisitable.setSelection(0);

        txtCedula.requestFocus();
    }

    private void actualizarFechaHora() {
        Date ahora = new Date();
        txtFechaRegistro.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora));
        txtHoraRegistro.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(ahora));
    }

    private String formatearFechaNacimiento(String valor) {
        valor = valor.replace("/", "").replace("-", "").trim();

        if (valor.length() == 6) {
            String yy = valor.substring(0, 2);
            String mm = valor.substring(2, 4);
            String dd = valor.substring(4, 6);
            return dd + "/" + mm + "/" + yy;
        }

        if (valor.length() == 8) {
            String yyyy = valor.substring(0, 4);
            String mm = valor.substring(4, 6);
            String dd = valor.substring(6, 8);
            return dd + "/" + mm + "/" + yyyy;
        }

        return valor;
    }

    private String capitalizar(String texto) {
        texto = texto.toLowerCase(Locale.ROOT).trim();
        if (texto.isEmpty()) return "";
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

    private String limpiarCSV(String valor) {
        return valor.replace(",", " ").replace("\n", " ").trim();
    }


    private void vaciarCSVInterno() {
        try {
            File archivo = new File(getExternalFilesDir(null), "export/datos_ocr.csv");

            if (archivo.exists()) {
                FileWriter fw = new FileWriter(archivo, false);
                fw.write("cedula,fecha_nacimiento,apellido1,apellido2,nombre,fecha_lectura,hora_lectura\n");
                fw.close();
            }
        } catch (Exception ignored) {
        }
    }


    private boolean csvTieneDatos(File archivo) {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(archivo));
            int lineas = 0;

            while (br.readLine() != null) {
                lineas++;
                if (lineas > 1) {
                    br.close();
                    return true;
                }
            }

            br.close();
        } catch (Exception ignored) {
        }

        return false;
    }

    private void confirmarVaciarLote() {
        new AlertDialog.Builder(this)
                .setTitle("Vaciar lote interno")
                .setMessage("Esto borra las lecturas guardadas dentro de la app. Use esta opción solo después de confirmar que el CSV fue copiado correctamente.")
                .setPositiveButton("Vaciar", (dialog, which) -> {
                    vaciarCSVInterno();
                    Toast.makeText(this, "Lote interno vacío", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



    private void cargarPersonasVisitables() {
        db.collection("personasVisitable")
                .whereEqualTo("organizacionId", "organizacion-1")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error cargando personas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    personasIds.clear();
                    personasNombres.clear();

                    personasIds.add("");
                    personasNombres.add("Seleccione persona a visitar");

                    if (queryDocumentSnapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            String id = doc.getId();
                            String nombre = doc.getString("nombre");
                            String apellido = doc.getString("apellido");

                            String nombreCompleto = (
                                    (nombre != null ? nombre : "") + " " +
                                            (apellido != null ? apellido : "")
                            ).trim();

                            personasIds.add(id);
                            personasNombres.add(nombreCompleto);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            personasNombres
                    );

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spnPersonaVisitable.setAdapter(adapter);
                    spnPersonaVisitable.setSelection(0);
                });
    }


}