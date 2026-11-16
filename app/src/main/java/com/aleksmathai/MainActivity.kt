package com.aleksmathai

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    private lateinit var exercise: EditText
    private lateinit var answer: TextView
    private val imageCode = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 26)
            setBackgroundColor(0xFFF3F6FC.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "🧠 ALEKS Math AI 4.3"
            textSize = 26f
            setTextColor(0xFF102A56.toInt())
        })
        layout.addView(TextView(this).apply {
            text = "Gratis • Sin API ni suscripción • OCR de imágenes"
            textSize = 14f
        })
        exercise = EditText(this).apply {
            hint = "Escribe el ejercicio… Ej.: 2x^2-5x+2=0, sqrt(81), area circle 5"
            minLines = 4
            gravity = Gravity.TOP
            textSize = 17f
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        layout.addView(exercise, LinearLayout.LayoutParams(-1, 150))
        layout.addView(Button(this).apply {
            text = "Leer ejercicio de una imagen"
            setOnClickListener { pickImage() }
        })
        layout.addView(Button(this).apply {
            text = "Resolver"
            setOnClickListener {
                val q = exercise.text.toString().trim()
                answer.text = if (q.isBlank()) "Escribe o carga un ejercicio."
                    else FreeSolver.solve(q)
            }
        })
        answer = TextView(this).apply {
            text = "La respuesta aparecerá aquí."
            textSize = 19f
            setTextColor(0xFF102A56.toInt())
            setPadding(0, 20, 0, 0)
        }
        layout.addView(answer)
        layout.addView(TextView(this).apply {
            text = "\\nEjemplos compatibles:\\n• 3*(8+2)^2\\n• 5x-7=18\\n• x^2-5x+6=0\\n• sqrt(144)\\n• 15% de 80: 15%*80\\n• area circle 5\\n• area triangle 10 8\\n• area rectangle 4 6\\n• volume cube 3"
            textSize = 14f
            setTextColor(0xFF42536B.toInt())
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun pickImage() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(i, "Selecciona una captura"), imageCode)
    }

    @Deprecated("Legacy picker callback for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != imageCode || resultCode != Activity.RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            answer.text = "Leyendo imagen…"
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener {
                    exercise.setText(it.text)
                    answer.text = "Texto leído. Revisa los signos y pulsa «Resolver»."
                }
                .addOnFailureListener { answer.text = "No pude leer la imagen. Prueba otra captura." }
        } catch (_: Exception) {
            answer.text = "No se pudo abrir la imagen."
        }
    }
}

private object FreeSolver {
    fun solve(raw: String): String {
        val original = raw.trim()
        val geo = geometry(original)
        if (geo != null) return geo
        val s = original.lowercase()
            .replace("×", "*").replace("÷", "/").replace("−", "-")
            .replace(" ", "").replace(",", ".")
        if (s.contains('=') && s.contains('x')) {
            quadratic(s)?.let { return it }
            linear(s)?.let { return it }
        }
        val v = calculate(s) ?: return "Este ejercicio todavía no está incluido en el motor gratuito. Prueba escribirlo como una expresión o ecuación estándar."
        return fmt(v)
    }

    private fun fmt(v: Double): String {
        if (!v.isFinite()) return "No definido"
        val r = round(v * 1e10) / 1e10
        return if (abs(r - round(r)) < 1e-9) round(r).toLong().toString() else r.toString()
    }

    private fun geometry(raw: String): String? {
        val s = raw.lowercase().replace(",", ".")
        val nums = Regex("-?\\d+(?:\\.\\d+)?").findAll(s).map { it.value.toDouble() }.toList()
        return try {
            when {
                ("area" in s || "área" in s) && ("circle" in s || "circulo" in s || "círculo" in s) && nums.size >= 1 ->
                    "Área = ${fmt(PI * nums[0].pow(2))}"
                ("circumference" in s || "circunferencia" in s) && nums.size >= 1 ->
                    "Circunferencia = ${fmt(2 * PI * nums[0])}"
                ("area" in s || "área" in s) && ("rectangle" in s || "rectangulo" in s || "rectángulo" in s) && nums.size >= 2 ->
                    "Área = ${fmt(nums[0] * nums[1])}"
                ("perimeter" in s || "perimetro" in s || "perímetro" in s) && ("rectangle" in s || "rectangulo" in s || "rectángulo" in s) && nums.size >= 2 ->
                    "Perímetro = ${fmt(2 * (nums[0] + nums[1]))}"
                ("area" in s || "área" in s) && ("triangle" in s || "triangulo" in s || "triángulo" in s) && nums.size >= 2 ->
                    "Área = ${fmt(nums[0] * nums[1] / 2)}"
                ("volume" in s || "volumen" in s) && ("cube" in s || "cubo" in s) && nums.size >= 1 ->
                    "Volumen = ${fmt(nums[0].pow(3))}"
                ("volume" in s || "volumen" in s) && ("cylinder" in s || "cilindro" in s) && nums.size >= 2 ->
                    "Volumen = ${fmt(PI * nums[0].pow(2) * nums[1])}"
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun quadratic(s: String): String? {
        val sides = s.split("=")
        if (sides.size != 2) return null
        val l = poly(sides[0]) ?: return null
        val r = poly(sides[1]) ?: return null
        val a = l[2] - r[2]
        val b = l[1] - r[1]
        val c = l[0] - r[0]
        if (abs(a) < 1e-12) return null
        val d = b*b - 4*a*c
        if (d < -1e-10) return "No hay soluciones reales"
        if (abs(d) < 1e-10) return "x = ${fmt(-b/(2*a))}"
        val root = sqrt(d)
        val x1 = (-b + root)/(2*a)
        val x2 = (-b - root)/(2*a)
        return "x = ${fmt(x1)} o x = ${fmt(x2)}"
    }

    // Reads polynomial terms such as 2x^2-5x+3; degree up to 2.
    private fun poly(e: String): DoubleArray? {
        val s = e.replace("-", "+-")
        val out = doubleArrayOf(0.0, 0.0, 0.0)
        for (t0 in s.split("+").filter { it.isNotEmpty() }) {
            val t = t0.trim()
            val idx = t.indexOf('x')
            if (idx < 0) {
                out[0] += t.toDoubleOrNull() ?: return null
            } else {
                val coeffText = t.substring(0, idx)
                val coeff = when (coeffText) { "", "+" -> 1.0; "-" -> -1.0; else -> coeffText.toDoubleOrNull() ?: return null }
                val degree = if (t.substring(idx + 1).startsWith("^2")) 2 else if (t.length == idx + 1) 1 else return null
                out[degree] += coeff
            }
        }
        return out
    }

    private fun linear(s: String): String? {
        val sides = s.split("=")
        if (sides.size != 2) return null
        fun parts(e: String): Pair<Double, Double>? {
            var a = 0.0; var b = 0.0
            for (term in e.replace("-", "+-").split("+").filter { it.isNotEmpty() }) {
                if (term.contains("x")) {
                    val n = term.replace("x", "")
                    a += when (n) { "", "+" -> 1.0; "-" -> -1.0; else -> n.toDoubleOrNull() ?: return null }
                } else b += term.toDoubleOrNull() ?: return null
            }
            return a to b
        }
        val l = parts(sides[0]) ?: return null
        val r = parts(sides[1]) ?: return null
        val a = l.first - r.first
        val b = r.second - l.second
        if (abs(a) < 1e-12) return if (abs(b) < 1e-12) "Infinitas soluciones" else "Sin solución"
        return "x = ${fmt(b / a)}"
    }

    private fun calculate(s: String): Double? {
        if (!s.matches(Regex("[0-9.+*/()^%a-z-]+"))) return null
        return try {
            val p = Parser(s)
            val v = p.expr()
            if (p.i == s.length && v.isFinite()) v else null
        } catch (_: Exception) { null }
    }

    private class Parser(val s: String) {
        var i = 0
        fun expr(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]; val n = term()
                v = if (op == '+') v + n else v - n
            }
            return v
        }
        private fun term(): Double {
            var v = power()
            while (i < s.length && s[i] in "*/%") {
                val op = s[i++]; val n = power()
                v = when (op) { '*' -> v*n; '/' -> v/n; else -> v%n }
            }
            return v
        }
        private fun power(): Double {
            val v = atom()
            return if (i < s.length && s[i] == '^') { i++; v.pow(power()) } else v
        }
        private fun atom(): Double {
            if (i < s.length && s[i] == '(') {
                i++; val v = expr()
                require(i < s.length && s[i++] == ')')
                return v
            }
            if (s.startsWith("sqrt(", i)) {
                i += 5; val v = expr(); require(i < s.length && s[i++] == ')'); return sqrt(v)
            }
            if (s.startsWith("abs(", i)) {
                i += 4; val v = expr(); require(i < s.length && s[i++] == ')'); return abs(v)
            }
            if (s.startsWith("pi", i)) { i += 2; return PI }
            val start = i
            if (i < s.length && s[i] in "+-") i++
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(start != i)
            return s.substring(start, i).toDouble()
        }
    }
}
