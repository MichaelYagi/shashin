package com.miyagi.shashin.component

class FaceFeatures {
    //128 features to characterize each face
    var features = FloatArray(128)
    private var faceType = -1

    private constructor() {}
    constructor(features: FloatArray, faceType: Int) {
        this.features = features
        this.faceType = faceType
    }

    fun setFaceType(faceType: Int) {
        this.faceType = faceType
    }

    fun getFaceType(): Int {
        check(faceType > -1) { "face type is not expected" }
        return faceType
    }

    companion object {
        const val LEFT_FACE = 0
        const val CENTER_FACE = 1
        const val RIGHT_FACE = 2
    }
}