package com.miyagi.shashin.component

class Prediction(
    val percentage: Float,
    val isIdentified: Boolean,
    val identifier: Int,
    val distance: Float
) {

    fun getPercentage(): Double {
        return percentage.toDouble()
    }

}