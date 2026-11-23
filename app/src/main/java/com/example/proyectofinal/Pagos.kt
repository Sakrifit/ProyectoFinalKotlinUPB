package com.example.proyectofinal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

data class PagoItem(
    val id: String = "",
    val uid: String = "",
    val usuario: String = "",
    val personas: Int = 0,
    val sala: Int = 0,
    val fecha: String = "",
    val hora: String = "",
    val cuenta: Double = 0.0,
    )
class Pagos : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseDatabase
        .getInstance("https://proyectofinalkotlinupb-default-rtdb.firebaseio.com/")
        .reference

    private lateinit var tvBienvenida: TextView
    private lateinit var btnVolver: ImageButton
    private lateinit var tablaPagos: TableLayout
    private lateinit var btnListaFilmes: Button
    private lateinit var etUsuario: EditText
    private lateinit var etPersonas: EditText

    private lateinit var etSala: EditText
    private lateinit var etFecha: EditText
    private lateinit var etHora: EditText
    private lateinit var etCuenta: EditText
    private lateinit var btnCrear: ImageButton
    private lateinit var btnBuscar: ImageButton
    private lateinit var btnActualizar: ImageButton
    private lateinit var btnEliminar: ImageButton

    private val listaPagos = mutableListOf<PagoItem>()
    private var pagoSeleccionado: PagoItem? = null
    private var nombreUsuarioActual: String = "Usuario"
    private val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pagos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        tvBienvenida = findViewById(R.id.tvBienvenida)
        btnVolver = findViewById(R.id.btnVolver)
        tablaPagos = findViewById(R.id.tablaPagos)
        btnListaFilmes = findViewById(R.id.btnListaFilmes)
        etUsuario = findViewById(R.id.etUsuario)
        etPersonas = findViewById(R.id.etPersonas)
        etSala = findViewById(R.id.etSala)
        etFecha = findViewById(R.id.etFecha)
        etHora = findViewById(R.id.etHora)
        etCuenta = findViewById(R.id.etCuenta)
        btnCrear = findViewById(R.id.btnCrear)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)

        limpiarSeleccionPagos()
        cargarNombreUsuario()
        escucharPagos()

        btnVolver.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        btnListaFilmes.setOnClickListener {
            val intent = Intent(this, InterfazAdmin::class.java)
            startActivity(intent)
            finish()
        }
        btnCrear.setOnClickListener {
            crearPago()
        }
        btnBuscar.setOnClickListener {
            buscarPago()
        }
        btnActualizar.setOnClickListener {
            actualizarPago()
        }
        btnEliminar.setOnClickListener {
            eliminarPago()
        }

    }
    private fun crearPago() {
    }
    private fun buscarPago() {
    }
    private fun actualizarPago() {
    }
    private fun eliminarPago() {
    }
    private fun limpiarSeleccionPagos() {
        pagoSeleccionado = null
        actualizarTablaPagos()
    }
    private fun actualizarTablaPagos() {
        val totalFilas = tablaPagos.childCount
        if (totalFilas > 1) {
            tablaPagos.removeViews(1, totalFilas - 1)
        }

        for (pago in listaPagos) {
            val fila = TableRow(this)
            val esSeleccionada = pagoSeleccionado?.id == pago.id
            fila.setBackgroundColor(if (esSeleccionada) Color.parseColor("#E3ECFF") else Color.WHITE)

            fila.addView(crearCelda(pago.usuario))
            fila.addView(crearCelda(pago.personas.toString()))
            fila.addView(crearCelda(pago.sala.toString()))
            fila.addView(crearCelda(pago.fecha))
            fila.addView(crearCelda(pago.hora))
            fila.addView(crearCelda(formatoMoneda.format(pago.cuenta)))

            fila.setOnClickListener {
                seleccionarPago(pago)
            }

            tablaPagos.addView(fila)
        }
    }
    private fun cargarNombreUsuario() {
        val user = auth.currentUser
        if (user == null) {
            tvBienvenida.text = "¡Bienvenido, Admin!"
            return
        }
        tvBienvenida.text = "¡Bienvenido, Admin!"

        db.child("usuarios").child(user.uid).child("nombre").get()
            .addOnSuccessListener { snap ->
                val nombreDb = snap.getValue(String::class.java)
                if (!nombreDb.isNullOrBlank()) {
                    nombreUsuarioActual = nombreDb
                    tvBienvenida.text = "¡Bienvenido, Admin $nombreUsuarioActual!"
                }
            }
    }
    private fun crearCelda(texto: String): TextView {
        return TextView(this).apply {
            setTextAppearance(R.style.TablaCelda)
            text = texto
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
        }
    }
    private fun seleccionarPago(pago: PagoItem) {
        pagoSeleccionado = pago
        actualizarTablaPagos()
    }
    private fun escucharPagos() {
        db.child("pagos").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaPagos.clear()

                for (hijo in snapshot.children) {
                    val id = hijo.key ?: continue
                    val uid = hijo.child("uid").getValue(String::class.java) ?: ""
                    val usuario = hijo.child("usuario").getValue(String::class.java) ?: "Sin usuario"
                    val fecha = hijo.child("fecha").getValue(String::class.java) ?: ""
                    val hora = hijo.child("hora").getValue(String::class.java) ?: ""
                    val personas = when (val rawPersonas = hijo.child("personas").value) {
                        is Long -> rawPersonas.toInt()
                        is Int -> rawPersonas
                        is String -> rawPersonas.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val sala = when (val rawSala = hijo.child("sala").value) {
                        is Long -> rawSala.toInt()
                        is Int -> rawSala
                        is String -> rawSala.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val cuenta = when (val rawCuenta = hijo.child("cuenta").value) {
                        is Long -> rawCuenta.toDouble()
                        is Double -> rawCuenta
                        is Int -> rawCuenta.toDouble()
                        else -> 0.0
                    }

                    listaPagos.add(
                        PagoItem(
                            id = id,
                            uid = uid,
                            usuario = usuario,
                            personas = personas,
                            sala = sala,
                            fecha = fecha,
                            hora = hora,
                            cuenta = cuenta
                        )
                    )
                }

                // Mantiene sincronizado el pago seleccionado si sigue existiendo
                pagoSeleccionado?.let { actual ->
                    pagoSeleccionado = listaPagos.find { it.id == actual.id }
                }

                actualizarTablaPagos()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@Pagos,
                    "Error al cargar pagos: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}