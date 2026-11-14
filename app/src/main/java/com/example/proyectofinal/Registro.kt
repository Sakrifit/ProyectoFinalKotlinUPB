package com.example.proyectofinal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database


class Registro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etClave = findViewById<EditText>(R.id.etClave)
        val etClaveComfirm = findViewById<EditText>(R.id.etClaveComfirm)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        // ---------- REGISTRARSE ----------
        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val clave = etClave.text.toString()
            val claveComfirm = etClaveComfirm.text.toString()

            if (nombre.isEmpty() || correo.isEmpty() || clave.length < 6 || claveComfirm.isEmpty()) {
                tvEstado.text = "Llena todos los campos (clave mínimo 6)"
                return@setOnClickListener
            }
            if (claveComfirm != clave) {
                tvEstado.text = "La contraseña debe ser la misma"
                return@setOnClickListener
            }

            // 1. Crear el usuario en Authentication
            auth.createUserWithEmailAndPassword(correo, clave)
                .addOnSuccessListener { resultado ->
                    val uid = resultado.user!!.uid

                    // 2. Guardar su perfil en Realtime Database
                    val usuario = mapOf(
                        "nombre" to nombre,
                        "correo" to correo
                    )
                    db.child("usuarios").child(uid).setValue(usuario)
                        .addOnSuccessListener {
                            tvEstado.text = "Usuario registrado: $nombre"
                        }
                        .addOnFailureListener { e ->
                            tvEstado.text = "Error al guardar: ${e.message}"
                        }
                }
                .addOnFailureListener { e ->
                    tvEstado.text = "Error al registrar: ${e.message}"
                }
        }
    }
}