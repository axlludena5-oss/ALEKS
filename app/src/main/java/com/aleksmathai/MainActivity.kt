
package com.aleksmathai

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(32, 50, 32, 32)

        val title = TextView(this)
        title.text = "ALEKS MATH AI"
        title.textSize = 26f

        val input = EditText(this)
        input.hint = "Escribe tu ejercicio matemático"

        val button = Button(this)
        button.text = "RESOLVER"

        val result = TextView(this)
        result.textSize = 18f

        button.setOnClickListener {
            result.text = resolver(input.text.toString())
        }

        layout.addView(title)
        layout.addView(input)
        layout.addView(button)
        layout.addView(result)

        setContentView(layout)
    }

    private fun resolver(texto: String): String {
        return try {
            val limpio = texto.replace(" ", "")
            val partes = limpio.split("+")
            if (partes.size == 2) {
                val a = partes[0].toDouble()
                val b = partes[1].toDouble()
                "Respuesta: ${a + b}"
            } else {
                "Ejercicio recibido. Escribe una suma como 25+15."
            }
        } catch (e: Exception) {
            "No pude interpretar el ejercicio."
        }
    }
}
