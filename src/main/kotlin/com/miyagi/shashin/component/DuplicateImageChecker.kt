package com.miyagi.shashin.component

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.logging.Logger
import javax.imageio.ImageIO

//https://stackoverflow.com/questions/17282272/comparing-images-to-find-duplicates
class DuplicateImageChecker {
    private var one: BufferedImage? = null
    private var two: BufferedImage? = null
    private var difference = 0.0
    private var threshold = 15

    private var logger: Logger = Logger.getLogger(DuplicateImageChecker::class.simpleName)

    fun isDuplicate(): Boolean {

        if (one != null && two != null) {
            var adjustedWidth = one!!.width
            var adjustedHeight = one!!.height

            if (one!!.width > two!!.width) {
                adjustedWidth = two!!.width
            }

            if (one!!.height > two!!.height) {
                adjustedHeight = two!!.height
            }

            var size = if (adjustedWidth > adjustedHeight) adjustedHeight else adjustedWidth

            one = Thumbnails.of(one)
                .outputQuality(0.5)
                .crop(Positions.CENTER)
                .size(size, size)
                .asBufferedImage()

            two = Thumbnails.of(two)
                .outputQuality(0.5)
                .crop(Positions.CENTER)
                .size(size, size)
                .asBufferedImage()

            if (one!!.width == two!!.width && one!!.height == two!!.height) {
                val width = one!!.width
                val height = one!!.height

                var diff = 0.0
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        diff += pixelDiff(one!!.getRGB(x, y), two!!.getRGB(x, y))
                    }
                }
                val maxDiff = 3L * 255 * width * height

                difference = diff / maxDiff

                return (100.0 * (difference)).toInt() < threshold
            }

            difference = 0.0
            return false
        } else {
            difference = 0.0
            return false
        }
    }

    private fun pixelDiff(rgb1: Int, rgb2: Int): Int {
        val r1 = (rgb1 shr 16) and 0xff
        val g1 = (rgb1 shr 8) and 0xff
        val b1 = rgb1 and 0xff
        val r2 = (rgb2 shr 16) and 0xff
        val g2 = (rgb2 shr 8) and 0xff
        val b2 = rgb2 and 0xff
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2)
    }

    fun getFirstImage(): BufferedImage? {
        return one
    }

    fun getBase64FirstImage(): String? {
        if (one != null) {
            var os = ByteArrayOutputStream()
            ImageIO.write(one, "jpg", os)
            return Base64.getEncoder().encodeToString(os.toByteArray())
        }
        return null
    }

    fun getBase64SecondImage(): String? {
        if (two != null) {
            var os = ByteArrayOutputStream()
            ImageIO.write(two, "jpg", os)
            return Base64.getEncoder().encodeToString(os.toByteArray())
        }
        return null
    }

    fun setFirstImage(one: String) {
        val file = File(one)
        if (file.exists()) {
            val bi = ImageIO.read(file)
            this.one = bi
        }
    }

    fun getSecondImage(): BufferedImage? {
        return two
    }

    fun setSecondImage(two: String) {
        val file = File(two)
        if (file.exists()) {
            val bi = ImageIO.read(file)
            this.two = bi
        }
    }

    // 1-100
    fun setThreshold(threshold: Int) {
        this.threshold = threshold
    }

    fun getDifference(): Double? {
        return difference
    }
}