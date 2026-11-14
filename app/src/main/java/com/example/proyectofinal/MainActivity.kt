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

class MainActivity : AppCompatActivity() {

    // Conexión con Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Enlazar las vistas del diseño
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etClave = findViewById<EditText>(R.id.etClave)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val tvEstado = findViewById<TextView>(R.id.tvEstado)



        // ---------- INGRESAR ----------
        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val clave = etClave.text.toString()

            if (correo.isEmpty() || clave.isEmpty()) {
                tvEstado.text = "Rellena todos los campos"
                return@setOnClickListener
            }

            // 1. Verificar correo y contraseña
            auth.signInWithEmailAndPassword(correo, clave)
                .addOnSuccessListener { resultado ->
                    val uid = resultado.user!!.uid

                    // 2. Leer su nombre desde Realtime Database
                    db.child("usuarios").child(uid).child("nombre").get()
                        .addOnSuccessListener { dato ->
                            val nombre = dato.getValue(String::class.java)
                            tvEstado.text = "¡Bienvenido, $nombre!"
                        }
                }
                .addOnFailureListener {
                    tvEstado.text = "Correo o contraseña incorrectos"
                }
        }
    }
}
