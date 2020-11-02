package helpers

import kotlin.math.pow

object MathHelper {
    fun round(value: Float, places: Int): Double {
        val scale = 10.0.pow(places.toDouble())
        return kotlin.math.round(value * scale) / scale
    }

    fun round(value: Double, places: Int): Double {
        val scale = 10.0.pow(places.toDouble())
        return kotlin.math.round(value * scale) / scale
    }
}