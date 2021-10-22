package com.miyagi.shashin.component

class FullFaceFeatures() {
    private var left: FaceFeatures? = null
    private var center: FaceFeatures? = null
    private var right: FaceFeatures? = null
    private var identifier = 0

    fun setIdentifier(identifier: Int) {
        this.identifier = identifier
    }


    fun getIdentifier(): Int {
        return identifier
    }

    fun setFaceFeatures(faceType: Int, features: FaceFeatures?) {
        when (faceType) {
            FaceFeatures.LEFT_FACE -> left = features
            FaceFeatures.CENTER_FACE -> center = features
            FaceFeatures.RIGHT_FACE -> right = features
            else -> throw IllegalArgumentException("not expected facetype")
        }
    }

    fun getFaceFeatures(faceType: Int): FaceFeatures? {
        return when (faceType) {
            FaceFeatures.LEFT_FACE -> left
            FaceFeatures.CENTER_FACE -> center
            FaceFeatures.RIGHT_FACE -> right
            else -> null
        }
    }

    fun allFacesAreSet(): Boolean {
        return left != null && center != null && right != null
    }
}