package cl.panelled.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Panel LED que hace pasar un texto de derecha a izquierda.
 *
 * El texto se dibuja una sola vez en un bitmap de [FILAS] píxeles de alto y cada
 * píxel se convierte en un LED. Las letras toman el color del modo elegido; los
 * emojis conservan sus colores originales.
 */
class LedView(context: Context, texto: String) : View(context) {

    enum class Modo { ROSA, ROJO, DORADO, ARCOIRIS }

    var modo = Modo.ROSA

    private val tipo: ByteArray        // 0 = apagado, 1 = letra, 2 = emoji
    private val colorEmoji: IntArray
    private val columnas: Int

    private var desplazamiento = 0f
    private var ultimoFrame = 0L
    private var tiempo = 0f

    private val pintaLed = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pintaBrillo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pintaApagado = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(42, 14, 24) }
    private val hsv = FloatArray(3)

    init {
        setBackgroundColor(Color.rgb(8, 3, 5))

        val pinta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = FILAS * 0.82f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
        }
        val fm = pinta.fontMetrics
        columnas = ceil(pinta.measureText(texto)).toInt() + 2
        val bmp = Bitmap.createBitmap(columnas, FILAS, Bitmap.Config.ARGB_8888)
        val linea = (FILAS - (fm.descent - fm.ascent)) / 2f - fm.ascent
        Canvas(bmp).drawText(texto, 1f, linea, pinta)

        val pixeles = IntArray(columnas * FILAS)
        bmp.getPixels(pixeles, 0, columnas, 0, 0, columnas, FILAS)
        bmp.recycle()

        tipo = ByteArray(pixeles.size)
        colorEmoji = IntArray(pixeles.size)
        for (i in pixeles.indices) {
            val p = pixeles[i]
            if (Color.alpha(p) < 110) continue
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            val saturacion = maxOf(r, g, b) - minOf(r, g, b)
            if (saturacion > 40) {
                tipo[i] = 2
                colorEmoji[i] = Color.rgb(r, g, b)
            } else {
                tipo[i] = 1
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val ahora = System.nanoTime()
        if (ultimoFrame != 0L) {
            val dt = (ahora - ultimoFrame) / 1_000_000_000f
            desplazamiento += VELOCIDAD * dt
            tiempo += dt
        }
        ultimoFrame = ahora

        val led = height.toFloat() / FILAS
        val visibles = ceil(width / led).toInt()
        val vuelta = columnas + visibles
        val inicio = floor(desplazamiento).toInt()
        val radio = led * 0.40f
        val radioBrillo = led * 0.62f

        for (x in 0 until visibles) {
            val col = (inicio + x) % vuelta - visibles
            val cx = x * led + led / 2
            for (y in 0 until FILAS) {
                val cy = y * led + led / 2
                val i = y * columnas + col
                val t = if (col in 0 until columnas) tipo[i].toInt() else 0
                if (t == 0) {
                    canvas.drawCircle(cx, cy, radio * 0.8f, pintaApagado)
                    continue
                }
                val color = if (t == 2) colorEmoji[i] else colorLetra(col)
                pintaBrillo.color = color
                pintaBrillo.alpha = 70
                canvas.drawCircle(cx, cy, radioBrillo, pintaBrillo)
                pintaLed.color = color
                canvas.drawCircle(cx, cy, radio, pintaLed)
            }
        }
        postInvalidateOnAnimation()
    }

    private fun colorLetra(col: Int): Int = when (modo) {
        Modo.ROSA -> Color.rgb(255, 79, 163)
        Modo.ROJO -> Color.rgb(255, 30, 60)
        Modo.DORADO -> Color.rgb(255, 200, 61)
        Modo.ARCOIRIS -> {
            hsv[0] = (col * 4f + tiempo * 120f) % 360f
            hsv[1] = 0.85f
            hsv[2] = 1f
            Color.HSVToColor(hsv)
        }
    }

    fun siguienteModo() {
        modo = Modo.entries[(modo.ordinal + 1) % Modo.entries.size]
    }

    companion object {
        private const val FILAS = 24
        private const val VELOCIDAD = 14f   // columnas de LEDs por segundo
    }
}
