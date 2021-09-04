package com.miyagi.shashin.model

interface TrainingData {
    fun getMetadataId(): String?
    fun getType(): String?
    fun getPath(): String?
    fun getThumbnailPathSmall(): String?
    fun getRecognitionLabelId(): Int?
    fun getRecognitionLabelName(): String?
}