package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
//codigo para delay
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class Registro : AppCompatActivity() {

    // Conexión con Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

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
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)
        val tvEstado = findViewById<TextView>(R.id.tvEstado)
        // volver a iniciar sesion
        btnVolver.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
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
                tvEstado.text = "Contraseña desigual en la confirmación"
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
                            lifecycleScope.launch {
                                delay(3000L.milliseconds)
                                val intent = Intent(this@Registro, InterfazUsuario::class.java)
                                startActivity(intent)
                                finish()
                            }
                            return@addOnSuccessListener
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