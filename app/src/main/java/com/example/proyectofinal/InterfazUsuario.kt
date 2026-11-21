package com.example.proyectofinal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
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

data class FilmeItem(
    val id: String = "",
    val asientos: Int = 0,
    val fecha: String = "",
    val hora: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    val sala: Int = 0
)

data class PagoRegistro(
    val uid: String = "",
    val usuario: String = "",
    val personas: Int = 0,
    val fecha: String = "",
    val hora: String = "",
    val cuenta: Double = 0.0,
    val sala: Int = 0
)

class InterfazUsuario : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var tvBienvenida: TextView
    private lateinit var btnVolver: ImageButton
    private lateinit var tablaReservas: TableLayout
    private lateinit var spinnerFormaDePago: Spinner
    private lateinit var tvPeliculaSeleccionada: TextView
    private lateinit var tvPrecioBase: TextView
    private lateinit var spinnerPersonas: Spinner
    private lateinit var btnReservar: Button
    private lateinit var tvEstado: TextView

    private val listaFilmes = mutableListOf<FilmeItem>()
    private var peliculaSeleccionada: FilmeItem? = null
    private var nombreUsuarioActual: String = "Usuario"

    private var formaPagoSeleccionada: String? = null
    private var descuentoPago: Double = 0.0

    private val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interfaz_usuario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        tvBienvenida = findViewById(R.id.tvBienvenida)
        btnVolver = findViewById(R.id.btnVolver)
        tablaReservas = findViewById(R.id.tablaReservas)
        spinnerFormaDePago = findViewById(R.id.spinnerFormaDePago)
        tvPeliculaSeleccionada = findViewById(R.id.tvPeliculaSeleccionada)
        tvPrecioBase = findViewById(R.id.tvPrecioBase)
        spinnerPersonas = findViewById(R.id.spinnerPersonas)
        btnReservar = findViewById(R.id.btnReservar)
        tvEstado = findViewById(R.id.tvEstado)

        tvEstado.text = ""
        limpiarSeleccionPelicula()
        configurarSpinnerFormaPago()
        configurarSpinnerPersonas()
        cargarNombreUsuario()
        escucharFilmes()

        btnVolver.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnReservar.setOnClickListener {
            procesarReserva()
        }
    }

    private fun configurarSpinnerFormaPago() {
        val opcionesPago = listOf(
            "Forma de pago",
            "Efectivo (Precio completo)",
            "Tarjeta (10% desc.)",
            "Tarjeta premium (30% desc.)"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesPago)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFormaDePago.adapter = adapter

        spinnerFormaDePago.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    1 -> {
                        formaPagoSeleccionada = "Efectivo"
                        descuentoPago = 0.0
                    }
                    2 -> {
                        formaPagoSeleccionada = "Tarjeta"
                        descuentoPago = 0.10
                    }
                    3 -> {
                        formaPagoSeleccionada = "Tarjeta premium"
                        descuentoPago = 0.30
                    }
                    else -> {
                        formaPagoSeleccionada = null
                        descuentoPago = 0.0
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                formaPagoSeleccionada = null
                descuentoPago = 0.0
            }
        }
    }

    private fun configurarSpinnerPersonas() {
        val cantidades = listOf("Personas") + (1..10).map { it.toString() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cantidades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPersonas.adapter = adapter
    }

    private fun seleccionarPelicula(filme: FilmeItem) {
        peliculaSeleccionada = filme
        tvPeliculaSeleccionada.text = filme.nombre
        tvPrecioBase.text = "Precio base: ${formatoMoneda.format(filme.precio)}"
        actualizarTablaFilmes()
    }

    private fun limpiarSeleccionPelicula() {
        peliculaSeleccionada = null
        tvPeliculaSeleccionada.text = "Selecciona una película"
        tvPrecioBase.text = ""
        actualizarTablaFilmes()
    }

    private fun cargarNombreUsuario() {
        val user = auth.currentUser
        if (user == null) {
            tvBienvenida.text = "¡Bienvenido!"
            return
        }

        nombreUsuarioActual = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
                    ?: "Usuario"
        tvBienvenida.text = "¡Bienvenido, $nombreUsuarioActual!"

        db.child("usuarios").child(user.uid).child("nombre").get()
            .addOnSuccessListener { snap ->
                val nombreDb = snap.getValue(String::class.java)
                if (!nombreDb.isNullOrBlank()) {
                    nombreUsuarioActual = nombreDb
                    tvBienvenida.text = "¡Bienvenido, $nombreUsuarioActual!"
                }
            }
    }

    private fun escucharFilmes() {
        db.child("filmes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaFilmes.clear()

                for (hijo in snapshot.children) {
                    val id = hijo.key ?: continue
                    val nombre = hijo.child("nombre").getValue(String::class.java) ?: "Sin nombre"
                    val fecha = hijo.child("fecha").getValue(String::class.java) ?: ""
                    val hora = hijo.child("hora").getValue(String::class.java) ?: ""
                    val sala = hijo.child("sala").getValue(Int::class.java) ?: 0
                    val asientos = hijo.child("asientos").getValue(Int::class.java) ?: 0
                    val precio = when (val rawPrecio = hijo.child("precio").value) {
                        is Long -> rawPrecio.toDouble()
                        is Double -> rawPrecio
                        is Int -> rawPrecio.toDouble()
                        else -> 0.0
                    }

                    listaFilmes.add(
                        FilmeItem(
                            id = id,
                            asientos = asientos,
                            fecha = fecha,
                            hora = hora,
                            nombre = nombre,
                            precio = precio,
                            sala = sala
                        )
                    )
                }

                // Mantiene sincronizada la película seleccionada si sigue existiendo
                peliculaSeleccionada?.let { actual ->
                    val actualizada = listaFilmes.find { it.id == actual.id }
                    if (actualizada != null) {
                        peliculaSeleccionada = actualizada
                        tvPeliculaSeleccionada.text = actualizada.nombre
                        tvPrecioBase.text = "Precio base: ${formatoMoneda.format(actualizada.precio)}"
                    } else {
                        peliculaSeleccionada = null
                        tvPeliculaSeleccionada.text = "Selecciona una película"
                        tvPrecioBase.text = ""
                    }
                }

                actualizarTablaFilmes()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@InterfazUsuario,
                    "Error al cargar filmes: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun actualizarTablaFilmes() {
        val totalFilas = tablaReservas.childCount
        if (totalFilas > 1) {
            tablaReservas.removeViews(1, totalFilas - 1)
        }

        for (filme in listaFilmes) {
            val fila = TableRow(this)
            val esSeleccionada = peliculaSeleccionada?.id == filme.id
            fila.setBackgroundColor(if (esSeleccionada) Color.parseColor("#E3ECFF") else Color.WHITE)

            fila.addView(crearCelda(filme.sala.toString()))
            fila.addView(crearCelda(filme.asientos.toString()))
            fila.addView(crearCelda(filme.nombre))
            fila.addView(crearCelda(filme.fecha))
            fila.addView(crearCelda(filme.hora))
            fila.addView(crearCelda(formatoMoneda.format(filme.precio)))

            fila.setOnClickListener {
                seleccionarPelicula(filme)
            }

            tablaReservas.addView(fila)
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

    private fun procesarReserva() {
        val user = auth.currentUser
        if (user == null) {
            tvEstado.setTextColor(Color.parseColor("#B00020"))
            tvEstado.text = "Debes iniciar sesión para reservar."
            return
        }

        val filmeActual = peliculaSeleccionada
        val pagoElegido = formaPagoSeleccionada
        val personasElegidas = spinnerPersonas.selectedItem?.toString()?.toIntOrNull()

        if (filmeActual == null || pagoElegido == null || personasElegidas == null) {
            tvEstado.setTextColor(Color.parseColor("#B00020"))
            tvEstado.text = "Se deben rellenar todos los campos."
            return
        }

        if (filmeActual.asientos < personasElegidas) {
            tvEstado.setTextColor(Color.parseColor("#B00020"))
            tvEstado.text = "No hay asientos disponibles para $personasElegidas persona(s)."
            return
        }

        val totalPagar = (filmeActual.precio * personasElegidas) * (1.0 - descuentoPago)
        val filmeAsientosRef = db.child("filmes").child(filmeActual.id).child("asientos")

        filmeAsientosRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val asientosActuales = currentData.getValue(Int::class.java)
                    ?: return Transaction.success(currentData)

                if (asientosActuales < personasElegidas) {
                    return Transaction.abort()
                }

                currentData.value = asientosActuales - personasElegidas
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    tvEstado.setTextColor(Color.parseColor("#B00020"))
                    tvEstado.text = "Error al reservar: ${error.message}"
                    return
                }

                if (!committed) {
                    tvEstado.setTextColor(Color.parseColor("#B00020"))
                    tvEstado.text = "No hay asientos disponibles para $personasElegidas persona(s)."
                    return
                }

                val pago = PagoRegistro(
                    uid = user.uid,
                    usuario = nombreUsuarioActual,
                    personas = personasElegidas,
                    fecha = filmeActual.fecha,
                    hora = filmeActual.hora,
                    cuenta = totalPagar,
                    sala = filmeActual.sala
                )

                db.child("pagos").push().setValue(pago)
                    .addOnSuccessListener {
                        tvEstado.setTextColor(Color.parseColor("#1B5E20"))
                        tvEstado.text = "Reserva exitosa. Total: ${formatoMoneda.format(totalPagar)}"
                        limpiarSeleccionPelicula()
                        spinnerFormaDePago.setSelection(0)
                        spinnerPersonas.setSelection(0)
                    }
                    .addOnFailureListener { e ->
                        tvEstado.setTextColor(Color.parseColor("#B00020"))
                        tvEstado.text = "Asientos descontados, pero falló guardar el pago: ${e.message}"
                    }
            }
        })
    }
}