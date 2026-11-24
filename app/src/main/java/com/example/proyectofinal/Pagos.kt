package com.example.proyectofinal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
    val cuenta: Double = 0.0
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
    private var filtroActivo: List<PagoItem>? = null
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
        val usuario = etUsuario.text.toString().trim()
        val personasStr = etPersonas.text.toString().trim()
        val salaStr = etSala.text.toString().trim()
        val fecha = etFecha.text.toString().trim()
        val hora = etHora.text.toString().trim()
        val cuentaStr = etCuenta.text.toString().trim()

        val personas = personasStr.toIntOrNull()
        val sala = salaStr.toIntOrNull()
        val cuenta = cuentaStr.replace(",", ".").toDoubleOrNull()

        if (usuario.isEmpty() || personas == null || sala == null || fecha.isEmpty() || hora.isEmpty() || cuenta == null) {
            Toast.makeText(this, "Por favor completa todos los campos para crear", Toast.LENGTH_SHORT).show()
            return
        }

        val idNuevo = db.child("pagos").push().key ?: return
        val nuevoPago = PagoItem(
            id = idNuevo,
            uid = "particular",
            usuario = usuario,
            personas = personas,
            sala = sala,
            fecha = fecha,
            hora = hora,
            cuenta = cuenta
        )

        db.child("pagos").child(idNuevo).setValue(nuevoPago)
            .addOnSuccessListener {
                Toast.makeText(this, "Pago creado con éxito", Toast.LENGTH_SHORT).show()
                limpiarCamposCRUD()
                pagoSeleccionado = null
                filtroActivo = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buscarPago() {
        val usuarioFiltro = etUsuario.text.toString().trim()
        val personasFiltro = etPersonas.text.toString().trim().toIntOrNull()
        val salaFiltro = etSala.text.toString().trim().toIntOrNull()
        val fechaFiltro = etFecha.text.toString().trim()
        val horaFiltro = etHora.text.toString().trim()
        val cuentaFiltro = etCuenta.text.toString().trim().replace(",", ".").toDoubleOrNull()

        val filtrados = listaPagos.filter { pago ->
            val cUsuario = usuarioFiltro.isEmpty() || pago.usuario.lowercase().contains(usuarioFiltro.lowercase())
            val cPersonas = personasFiltro == null || pago.personas == personasFiltro
            val cSala = salaFiltro == null || pago.sala == salaFiltro
            val cFecha = fechaFiltro.isEmpty() || pago.fecha.contains(fechaFiltro)
            val cHora = horaFiltro.isEmpty() || pago.hora.contains(horaFiltro)
            val cCuenta = cuentaFiltro == null || pago.cuenta == cuentaFiltro

            cUsuario && cPersonas && cSala && cFecha && cHora && cCuenta
        }

        filtroActivo = filtrados
        actualizarTablaPagos(filtrados)
        Toast.makeText(this, "Se encontraron ${filtrados.size} resultados", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarPago() {
        val seleccionado = pagoSeleccionado
        if (seleccionado == null) {
            Toast.makeText(this, "Selecciona un pago en la tabla para actualizar", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioTexto = etUsuario.text.toString().trim()
        val personasTexto = etPersonas.text.toString().trim()
        val salaTexto = etSala.text.toString().trim()
        val fechaTexto = etFecha.text.toString().trim()
        val horaTexto = etHora.text.toString().trim()
        val cuentaTexto = etCuenta.text.toString().trim()

        val nuevoUsuario = if (usuarioTexto.isEmpty()) seleccionado.usuario else usuarioTexto
        val nuevasPersonas = if (personasTexto.isEmpty()) {
            seleccionado.personas
        } else {
            personasTexto.toIntOrNull() ?: run {
                Toast.makeText(this, "Cantidad de personas inválida", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val nuevaSala = if (salaTexto.isEmpty()) {
            seleccionado.sala
        } else {
            salaTexto.toIntOrNull() ?: run {
                Toast.makeText(this, "Sala inválida", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val nuevaFecha = if (fechaTexto.isEmpty()) seleccionado.fecha else fechaTexto
        val nuevaHora = if (horaTexto.isEmpty()) seleccionado.hora else horaTexto
        val nuevaCuenta = if (cuentaTexto.isEmpty()) {
            seleccionado.cuenta
        } else {
            cuentaTexto.replace(",", ".").toDoubleOrNull() ?: run {
                Toast.makeText(this, "Cuenta inválida", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val pagoActualizado = seleccionado.copy(
            uid = if (seleccionado.uid.isBlank()) "particular" else seleccionado.uid,
            usuario = nuevoUsuario,
            personas = nuevasPersonas,
            sala = nuevaSala,
            fecha = nuevaFecha,
            hora = nuevaHora,
            cuenta = nuevaCuenta
        )

        db.child("pagos").child(seleccionado.id).setValue(pagoActualizado)
            .addOnSuccessListener {
                Toast.makeText(this, "Pago actualizado correctamente", Toast.LENGTH_SHORT).show()
                pagoSeleccionado = pagoActualizado
                limpiarCamposCRUD()
                filtroActivo = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarPago() {
        val seleccionado = pagoSeleccionado
        if (seleccionado == null) {
            Toast.makeText(this, "Selecciona una fila en la tabla para eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("pagos").child(seleccionado.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Pago eliminado correctamente", Toast.LENGTH_SHORT).show()
                pagoSeleccionado = null
                limpiarCamposCRUD()
                filtroActivo = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun limpiarCamposCRUD() {
        etUsuario.setText("")
        etPersonas.setText("")
        etSala.setText("")
        etFecha.setText("")
        etHora.setText("")
        etCuenta.setText("")
    }
    private fun limpiarSeleccionPagos() {
        pagoSeleccionado = null
        actualizarTablaPagos()
    }
    private fun actualizarTablaPagos(pagosAMostrar: List<PagoItem> = listaPagos) {
        val totalFilas = tablaPagos.childCount
        if (totalFilas > 1) {
            tablaPagos.removeViews(1, totalFilas - 1)
        }

        for (pago in pagosAMostrar) {
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
                seleccionarPago(pago, pagosAMostrar)
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
    private fun seleccionarPago(pago: PagoItem, listaActual: List<PagoItem>) {
        pagoSeleccionado = pago
        actualizarTablaPagos(listaActual)
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

                actualizarTablaPagos(filtroActivo ?: listaPagos)
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