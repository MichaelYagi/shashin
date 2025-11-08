package com.miyagi.shashin.service

import dev.brachtendorf.jimagehash.hash.Hash
import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Base64
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.math.abs

class DuplicateImageChecker {

    // Configure perceptual hash algorithm (64-bit is common)
    // aHash (Average Hash)
    //Simplest method: resize → grayscale → average pixel value → bitstring.
    //Fast but less robust to transformations.
    private val ahash = AverageHash(64)

    // dHash (Difference Hash)
    //Compares adjacent pixel values (gradient-based).
    //Good balance of speed and robustness.
    private val dhash = DifferenceHash(64, DifferenceHash.Precision.Simple)

    // pHash (Perceptual Hash)
    //Uses DCT (Discrete Cosine Transform) to capture frequency domain features.
    //Most resilient to compression, scaling, and minor edits.
    private val phash = PerceptiveHash(64)

    /**
     * Compute perceptual hash for an image input stream.
     */
    fun computeHash(file: File): Hash {
        val image: BufferedImage = ImageIO.read(file)
        // ahash, dhash or phash
        return ahash.hash(image)
    }

    fun getBase64(file: File?): String? {
        if (file != null) {
            var os = ByteArrayOutputStream()
            ImageIO.write(ImageIO.read(file), "jpg", os)
            return Base64.getEncoder().encodeToString(os.toByteArray())
        }
        return null
    }

    /**
     * Compare two hashes using raw Hamming distance.
     * @param threshold maximum allowed bit difference to consider duplicates
     */
    fun isDuplicate(hash1: Hash, hash2: Hash, threshold: Int = 5): Boolean {
        val distance = hash1.hammingDistance(hash2)
        return distance <= threshold
    }

    /**
     * Compare two hashes using normalized distance (0.0 identical, 1.0 completely different).
     */
    fun similarityScore(hash1: Hash, hash2: Hash): Double {
        return 1.0 - hash1.normalizedHammingDistance(hash2)
    }
}