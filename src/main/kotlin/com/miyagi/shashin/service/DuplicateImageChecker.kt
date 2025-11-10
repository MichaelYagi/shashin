package com.miyagi.shashin.service

import dev.brachtendorf.jimagehash.hash.Hash
import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.util.Base64
import javax.imageio.ImageIO

class DuplicateImageChecker {

    // Default resolution
    private var resolution: Int = 64

    // Default algorithm: aHash
    private var algorithm: HashingAlgorithm = AverageHash(resolution)

    private var algorithmName = "ahash"

    /**
     * Setter to change algorithm dynamically.
     * Supported: "ahash", "dhash", "phash"
     */
    fun setAlgorithm(nameParam: String = "ahash", resolution: Int = 64) {
        var name = nameParam
        this.resolution = resolution

        algorithm = when (name.lowercase()) {
            "ahash" -> AverageHash(resolution)
            "dhash" -> DifferenceHash(resolution, DifferenceHash.Precision.Simple)
            "phash" -> PerceptiveHash(resolution)
            else -> {
                name = "ahash"
                AverageHash(resolution)
            } // fallback
        }

        this.algorithmName = name.lowercase()
    }

    fun getAlgorithmName(): String {
        return this.algorithmName
    }

    fun getResolution(): Int {
        return resolution
    }

    /**
     * Compute perceptual hash for an image file.
     */
    fun computeHash(file: File): Hash {
        val image: BufferedImage = ImageIO.read(file)
        return algorithm.hash(image) // use currently selected algorithm
    }

    fun computeHashValue(file: File): String {
        val image: BufferedImage = ImageIO.read(file)
        return algorithm.hash(image).hashValue.toString() // use currently selected algorithm
    }

    fun computeHashFromString(value: String, resolution: Int = 64, algorithmId: Int = 1): Hash {
        val bigInt = BigInteger(value)
        return Hash(bigInt, resolution, algorithmId)
    }

    fun computeHashFromValue(value: BigInteger, resolution: Int = 64, algorithmId: Int = 1): Hash {
        return Hash(value, resolution, algorithmId)
    }

    fun getBase64(file: File?): String? {
        if (file != null) {
            val os = ByteArrayOutputStream()
            ImageIO.write(ImageIO.read(file), "jpg", os)
            return Base64.getEncoder().encodeToString(os.toByteArray())
        }
        return null
    }

    fun isDuplicate(hash1: Hash, hash2: Hash, threshold: Int = 5): Boolean {
        val distance = hash1.hammingDistance(hash2)
        return distance <= threshold
    }

    fun similarityScore(hash1: Hash, hash2: Hash): Double {
        return 1.0 - hash1.normalizedHammingDistance(hash2)
    }
}