package com.example.proyectofinal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
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

class InterfazAdmin : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseDatabase
        .getInstance("https://proyectofinalkotlinupb-default-rtdb.firebaseio.com/")
        .reference

    private lateinit var btnVolver: ImageButton
    private lateinit var btnListaPagos: Button
    private lateinit var tablaPagos: TableLayout
    private lateinit var tablaPagosInferior: TableLayout
    private lateinit var btnCrear: ImageButton
    private lateinit var btnActualizar: ImageButton
    private lateinit var btnBuscar: ImageButton
    private lateinit var btnEliminar: ImageButton

    private val listaFilmes = mutableListOf<FilmeItem>()
    private var filmeSeleccionado: FilmeItem? = null
    private var modoAccion: String = ""
    private val editoresInferiores = mutableListOf<EditText>()

    private val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interfaz_admin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        inicializarVistas()
        escucharFilmes()

        btnVolver.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnListaPagos.setOnClickListener {
            val intent = Intent(this, Pagos::class.java)
            startActivity(intent)
        }

        btnCrear.setOnClickListener {
            if (tablaPagosInferior.visibility == View.VISIBLE && modoAccion == "CREAR") {
                ejecutarAccionInferior()
            } else {
                modoAccion = "CREAR"
                prepararTablaInferiorInteractiva("Complete los datos abajo y vuelva a presionar (+)")
            }
        }

        btnActualizar.setOnClickListener {
            if (tablaPagosInferior.visibility == View.VISIBLE && modoAccion == "BUSCAR") {
                ejecutarAccionInferior()
            } else {
                modoAccion = "BUSCAR"
                prepararTablaInferiorInteractiva("Ingrese el criterio de búsqueda abajo y presione la lupa")
            }
        }

        btnBuscar.setOnClickListener {
            filmeSeleccionado = null
            tablaPagosInferior.visibility = View.GONE
            escucharFilmes()
            Toast.makeText(this, "Tabla actualizada", Toast.LENGTH_SHORT).show()
        }

        btnEliminar.setOnClickListener {
            val seleccionado = filmeSeleccionado
            if (seleccionado == null) {
                Toast.makeText(this, "Selecciona una fila en la tabla superior para eliminar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.child("filmes").child(seleccionado.id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Filme eliminado correctamente", Toast.LENGTH_SHORT).show()
                    filmeSeleccionado = null
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun inicializarVistas() {
        btnVolver = findViewById(R.id.btnVolver)
        btnListaPagos = findViewById(R.id.btnListaPagos)
        tablaPagos = findViewById(R.id.tablaPagos)
        tablaPagosInferior = findViewById(R.id.tablaPagosInferior)
        btnCrear = findViewById(R.id.btnCrear)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnEliminar = findViewById(R.id.btnEliminar)

        tablaPagosInferior.visibility = View.GONE
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
                    val sala = when (val rawSala = hijo.child("sala").value) {
                        is Long -> rawSala.toInt()
                        is Int -> rawSala
                        is String -> rawSala.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val asientos = when (val rawAsientos = hijo.child("asientos").value) {
                        is Long -> rawAsientos.toInt()
                        is Int -> rawAsientos
                        is String -> rawAsientos.toIntOrNull() ?: 0
                        else -> 0
                    }
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
                actualizarTablaPrincipal(listaFilmes)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InterfazAdmin, "Error al cargar datos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarTablaPrincipal(filmes: List<FilmeItem>) {
        val totalFilas = tablaPagos.childCount
        if (totalFilas > 1) {
            tablaPagos.removeViews(1, totalFilas - 1)
        }

        for (filme in filmes) {
            val fila = TableRow(this)
            val esSeleccionada = filmeSeleccionado?.id == filme.id
            fila.setBackgroundColor(if (esSeleccionada) Color.parseColor("#E3ECFF") else Color.WHITE)

            fila.addView(crearCeldaTexto(filme.sala.toString()))
            fila.addView(crearCeldaTexto(filme.asientos.toString()))
            fila.addView(crearCeldaTexto(filme.nombre, esNombre = true))
            fila.addView(crearCeldaTexto(filme.fecha))
            fila.addView(crearCeldaTexto(filme.hora))
            fila.addView(crearCeldaTexto(formatoMoneda.format(filme.precio)))

            fila.setOnClickListener {
                filmeSeleccionado = filme
                actualizarTablaPrincipal(filmes)
            }

            tablaPagos.addView(fila)
        }
    }

    private fun crearCeldaTexto(texto: String, esNombre: Boolean = false): TextView {
        return TextView(this).apply {
            setTextAppearance(R.style.TablaCelda)
            text = texto
            gravity = Gravity.CENTER
            setPadding(12, 12, 12, 12)
            if (esNombre) {
                maxWidth = 350
                setSingleLine(false)
                isSingleLine = false
            }
        }
    }

    private fun prepararTablaInferiorInteractiva(mensajeToast: String) {
        tablaPagosInferior.visibility = View.VISIBLE
        editoresInferiores.clear()

        if (tablaPagosInferior.childCount > 0) {
            val fila = tablaPagosInferior.getChildAt(0) as? TableRow
            if (fila != null) {
                fila.removeAllViews()
                val hints = listOf("Sala", "Asiento", "Nombre", "Fecha", "Hora", "Precio")

                for (hintText in hints) {
                    val et = EditText(this).apply {
                        hint = hintText
                        setTextAppearance(R.style.TablaCelda)
                        setTextColor(Color.parseColor("#000000"))      // Texto que escribes: Negro puro
                        setHintTextColor(Color.parseColor("#666666"))  // Texto guía: Gris visible
                        gravity = Gravity.CENTER
                        setPadding(12, 16, 12, 16)
                        setBackgroundColor(Color.parseColor("#F0F0F0"))
                        setText("")

                        if (hintText == "Nombre") {
                            maxWidth = 350
                            setSingleLine(false)
                        }
                    }
                    editoresInferiores.add(et)
                    fila.addView(et)
                }
            }
        }
        Toast.makeText(this, mensajeToast, Toast.LENGTH_LONG).show()
    }

    private fun ejecutarAccionInferior() {
        if (editoresInferiores.size < 6) return

        val salaStr = editoresInferiores[0].text.toString().trim()
        val asientosStr = editoresInferiores[1].text.toString().trim()
        val nombre = editoresInferiores[2].text.toString().trim()
        val fecha = editoresInferiores[3].text.toString().trim()
        val hora = editoresInferiores[4].text.toString().trim()
        val precioStr = editoresInferiores[5].text.toString().trim()

        if (modoAccion == "CREAR") {
            val sala = salaStr.toIntOrNull()
            val asientos = asientosStr.toIntOrNull()
            val precio = precioStr.toDoubleOrNull()

            if (sala == null || asientos == null || nombre.isEmpty() || fecha.isEmpty() || hora.isEmpty() || precio == null) {
                Toast.makeText(this, "Por favor completa todos los campos para crear", Toast.LENGTH_SHORT).show()
                return
            }

            val idNuevo = db.child("filmes").push().key ?: return
            val nuevoFilme = FilmeItem(
                id = idNuevo,
                asientos = asientos,
                fecha = fecha,
                hora = hora,
                nombre = nombre,
                precio = precio,
                sala = sala
            )

            db.child("filmes").child(idNuevo).setValue(nuevoFilme)
                .addOnSuccessListener {
                    Toast.makeText(this, "Película creada con éxito", Toast.LENGTH_SHORT).show()
                    tablaPagosInferior.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        } else if (modoAccion == "BUSCAR") {
            val salaFiltro = salaStr.toIntOrNull()
            val asientosFiltro = asientosStr.toIntOrNull()
            val precioFiltro = precioStr.toDoubleOrNull()

            val filtrados = listaFilmes.filter { filme ->
                val cSala = salaFiltro == null || filme.sala == salaFiltro
                val cAsientos = asientosFiltro == null || filme.asientos == asientosFiltro
                val cNombre = nombre.isEmpty() || filme.nombre.lowercase().contains(nombre.lowercase())
                val cFecha = fecha.isEmpty() || filme.fecha.contains(fecha)
                val cHora = hora.isEmpty() || filme.hora.contains(hora)
                val cPrecio = precioFiltro == null || filme.precio == precioFiltro

                cSala && cAsientos && cNombre && cFecha && cHora && cPrecio
            }

            actualizarTablaPrincipal(filtrados)
            tablaPagosInferior.visibility = View.GONE
            Toast.makeText(this, "Se encontraron ${filtrados.size} resultados", Toast.LENGTH_SHORT).show()
        }
    }
}