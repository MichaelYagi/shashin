package com.miyagi.shashin.service

import dev.brachtendorf.jimagehash.hash.Hash
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
    private val phash = PerceptiveHash(64)

    /**
     * Compute perceptual hash for an image input stream.
     */
    fun computeHash(file: File): Hash {
        val image: BufferedImage = ImageIO.read(file)
        return phash.hash(image)
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

//    private var one: BufferedImage? = null
//    private var two: BufferedImage? = null
//    private var oneFileName: String? = null
//    private var twoFileName: String? = null
//    private var similarity = 0.0
//    private var threshold = 85.0
//    private var crop = false
//    private var differentSize = false
//    private var greyScale = false
//
//    private var logger: Logger = Logger.getLogger(DuplicateImageChecker::class.simpleName)
//
//    private fun isGreyScale(image: BufferedImage): Boolean {
//        try {
//            val width = image.width
//            val height = image.height
//
//            var pixel: Int
//            var red: Int
//            var green: Int
//            var blue: Int
//
//            for (i in 0 until width) {
//                for (j in 0 until height) {
//                    // scan through each pixel
//                    pixel = image.getRGB(i, j)
//                    red = (pixel shr 16) and 0xff
//                    green = (pixel shr 8) and 0xff
//                    blue = pixel and 0xff
//
//                    // check if R=G=B
//                    if (red != green || green != blue) {
//                        return false
//                    }
//                }
//            }
//
//            return true
//        } catch (e: IOException) {
//            logger.log(Level.INFO, "Error detecting greyscale", e)
//            return false
//        }
//    }
//
//    fun isDuplicate(): Boolean {
//        if (one != null && two != null) {
//            var adjustedWidth = one!!.width
//            var adjustedHeight = one!!.height
//
//            if (one!!.width > two!!.width) {
//                adjustedWidth = two!!.width
//            }
//
//            if (one!!.height > two!!.height) {
//                adjustedHeight = two!!.height
//            }
//
//            var size = if (adjustedWidth > adjustedHeight) adjustedHeight else adjustedWidth
//
//            val oneTn = Thumbnails.of(one).outputQuality(0.5)
//
//            var width = adjustedWidth
//            var height = adjustedHeight
//            if (crop) {
//                width = size
//                height = size
//                oneTn.crop(Positions.CENTER).size(size, size)
//            } else {
//                oneTn.size(adjustedWidth, adjustedHeight)
//            }
//
//            var oneTnBI = oneTn.asBufferedImage()
//            if (greyScale) {
//                val grayscaleImage  = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
//                val g2d = grayscaleImage.createGraphics()
//                g2d.drawImage(oneTnBI, 0, 0, null)
//                g2d.dispose()
//                oneTnBI = grayscaleImage
//            }
//
//            one = oneTnBI
//
//            val twoTn = Thumbnails.of(two).outputQuality(0.5)
//
//            if (crop) {
//                twoTn.crop(Positions.CENTER).size(size, size)
//            } else {
//                twoTn.size(adjustedWidth, adjustedHeight)
//            }
//
//            var twoTnBI = twoTn.asBufferedImage()
//            if (greyScale) {
//                val grayscaleImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
//                val g2d = grayscaleImage.createGraphics()
//                g2d.drawImage(twoTnBI, 0, 0, null)
//                g2d.dispose()
//                twoTnBI = grayscaleImage
//            }
//
//            two = twoTnBI
//
//            if (one!!.width == two!!.width && one!!.height == two!!.height) {
//                similarity = compareImages()
//
//                // Format to 2 decimal points
//                similarity = String.format("%.2f", similarity).toDouble()
//
//                return (100.0 * similarity) >= threshold
//            } else {
//                differentSize = true
//            }
//
//            similarity = 0.0
//            return false
//        } else {
//            similarity = 0.0
//            return false
//        }
//    }
//
//    private fun compareImages(): Double {
//        val width = one!!.width
//        val height = one!!.height
//
//        var diff = 0.0
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                diff += pixelDiff(one!!.getRGB(x, y), two!!.getRGB(x, y))
//            }
//        }
//        val maxDiff = 3L * 255 * width * height
//
//        return 1.0-(diff / maxDiff)
//    }
//
//    private fun pixelDiff(rgb1: Int, rgb2: Int): Int {
//        val r1 = (rgb1 shr 16) and 0xff
//        val g1 = (rgb1 shr 8) and 0xff
//        val b1 = rgb1 and 0xff
//        val r2 = (rgb2 shr 16) and 0xff
//        val g2 = (rgb2 shr 8) and 0xff
//        val b2 = rgb2 and 0xff
//        return abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
//    }
//
//    fun getFirstImage(): BufferedImage? {
//        return one
//    }
//
//    fun getBase64FirstImage(): String? {
//        if (one != null) {
//            var os = ByteArrayOutputStream()
//            ImageIO.write(one, "jpg", os)
//            return Base64.getEncoder().encodeToString(os.toByteArray())
//        }
//        return null
//    }
//
//    fun getBase64SecondImage(): String? {
//        if (two != null) {
//            var os = ByteArrayOutputStream()
//            ImageIO.write(two, "jpg", os)
//            return Base64.getEncoder().encodeToString(os.toByteArray())
//        }
//        return null
//    }
//
//    fun getFirstFilename(): String? {
//        return oneFileName
//    }
//
//    fun getSecondFilename(): String? {
//        return twoFileName
//    }
//
//    fun setFirstImage(one: String) {
//        oneFileName = one
//        val file = File(one)
//        if (file.exists()) {
//            val bi = ImageIO.read(file)
//            this.one = bi
//        } else {
//            logger.log(Level.INFO, "File $one does not exist for image 1")
//        }
//    }
//
//    fun getSecondImage(): BufferedImage? {
//        return two
//    }
//
//    fun setSecondImage(two: String) {
//        twoFileName = two
//        val file = File(two)
//        if (file.exists()) {
//            val bi = ImageIO.read(file)
//            this.two = bi
//        } else {
//            logger.log(Level.INFO, "File $two does not exist for image 2")
//        }
//    }
//
//    // 1-100
//    fun setThreshold(threshold: Double) {
//        this.threshold = threshold
//    }
//
//    fun getThreshold(): Double {
//        return this.threshold
//    }
//
//    fun setCrop(crop: Boolean) {
//        this.crop = crop
//    }
//
//    fun setGreyScale(greyScale: Boolean) {
//        this.greyScale = greyScale
//    }
//
//    fun getCrop(): Boolean {
//        return this.crop
//    }
//
//    fun getSimilarity(): Double? {
//        return similarity
//    }
//
//    fun getDifferentSize(): Boolean {
//        return differentSize
//    }
}