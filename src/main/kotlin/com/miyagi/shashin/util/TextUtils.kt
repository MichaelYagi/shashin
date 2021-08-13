package com.miyagi.shashin.util

import org.springframework.stereotype.Component
import java.util.*

@Component
class TextUtils {
    companion object {
        fun capitalized(str: String): String {
            return str.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase(Locale.getDefault())
                else it.toString()
            }
        }

        fun convertDecimalToFraction(x: Double): String {
            if (x < 0) {
                return "-" + convertDecimalToFraction(-x)
            }
            val tolerance = 1.0E-6
            var h1 = 1.0
            var h2 = 0.0
            var k1 = 0.0
            var k2 = 1.0
            var b = x
            do {
                val a = Math.floor(b)
                var aux = h1
                h1 = a * h1 + h2
                h2 = aux
                aux = k1
                k1 = a * k1 + k2
                k2 = aux
                b = 1 / (b - a)
            } while (Math.abs(x - h1 / k1) > x * tolerance)
            return "${h1.toInt()}/${k1.toInt()}"
        }

        fun generateUUID(path:String?,takenAt:String?,lat:String?,lng:String?): UUID {
            val uuidInput = path+takenAt+lat+lng
            return UUID.nameUUIDFromBytes(uuidInput.toByteArray())
        }
    }
}