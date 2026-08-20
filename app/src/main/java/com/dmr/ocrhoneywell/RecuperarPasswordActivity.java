package com.dmr.ocrhoneywell;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RecuperarPasswordActivity
        extends AppCompatActivity {

    private EditText txtCorreoRecuperacion;
    private Button btnEnviarRecuperacion;
    private Button btnVolverLogin;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_recuperar_password
        );

        auth =
                FirebaseAuth.getInstance();

        txtCorreoRecuperacion =
                findViewById(
                        R.id.txtCorreoRecuperacion
                );

        btnEnviarRecuperacion =
                findViewById(
                        R.id.btnEnviarRecuperacion
                );

        btnVolverLogin =
                findViewById(
                        R.id.btnVolverLogin
                );

        btnEnviarRecuperacion
                .setOnClickListener(
                        v -> enviarRecuperacion()
                );

        btnVolverLogin
                .setOnClickListener(
                        v -> finish()
                );
    }

    private void enviarRecuperacion() {

        String correo =
                txtCorreoRecuperacion
                        .getText()
                        .toString()
                        .trim()
                        .toLowerCase();

        if (TextUtils.isEmpty(correo)) {

            txtCorreoRecuperacion.setError(
                    "Ingrese su correo electrónico"
            );

            txtCorreoRecuperacion.requestFocus();

            return;
        }

        btnEnviarRecuperacion.setEnabled(false);

        auth.sendPasswordResetEmail(correo)
                .addOnSuccessListener(
                        unused -> {

                            btnEnviarRecuperacion
                                    .setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Se envió un correo para restablecer su contraseña.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            btnEnviarRecuperacion
                                    .setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "No fue posible enviar el correo de recuperación.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }
}

