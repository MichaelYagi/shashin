package com.miyagi.shashin.component

import com.miyagi.shashin.util.FileUtils
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.min

class DuplicateImageChecker {
    private var one: BufferedImage? = null
    private var two: BufferedImage? = null
    private var difference = 0.0
    private var x = 0
    private var y = 0

    fun isDuplicate(): Boolean {
        if (one == null || two == null) {

            return false
        }

        if (one!!.width + one!!.height < two!!.width + two!!.height) {
            val tmpOne = one
            val tmpTwo = two
            one = tmpTwo
            two = tmpOne
        }

        var width = one!!.width
        var height = one!!.height
        if (one!!.width > two!!.width) {
            width = two!!.width
            height = two!!.height
        }

        one = Thumbnails.of(one)
            .outputQuality(0.5)
            .size(height, width)
            .asBufferedImage()

        two = Thumbnails.of(two)
            .outputQuality(0.5)
            .size(height, width)
            .asBufferedImage()

        var f = 20
        val w1 = min(50, (one?.width ?: 0) - (two?.width ?: 0))
        val h1 = min(50, (one?.height ?: 0) - (two?.height ?: 0))
        val w2 = min(5, (one?.width ?: 0) - (two?.width ?: 0))
        val h2 = min(5, (one?.height ?: 0) - (two?.height ?: 0))

        for (i in 0..(one?.width ?: 0) - (two?.width ?: 0) step f) {
            for (j in 0..(one?.height ?: 0) - (two?.height ?: 0) step f) {
                compareSubset(i, j, f)
            }
        }

        one = one?.getSubimage(
            maxOf(0, x - w1),
            maxOf(0, y - h1),
            min(two?.width?.plus(w1) ?: 0, (one?.width ?: 0) - x + w1),
            min(two?.height?.plus(h1) ?: 0, (one?.height ?: 0) - y + h1)
        )
        x = 0
        y = 0
        difference = 0.0
        f = 5
        for (i in 0..(one?.width ?: 0) - (two?.width ?: 0) step f) {
            for (j in 0..(one?.height ?: 0) - (two?.height ?: 0) step f) {
                compareSubset(i, j, f)
            }
        }
        one = one?.getSubimage(
            maxOf(0, x - w2),
            maxOf(0, y - h2),
            min(two?.width?.plus(w2) ?: 0, (one?.width ?: 0) - x + w2),
            min(two?.height?.plus(h2) ?: 0, (one?.height ?: 0) - y + h2)
        )
        f = 1
        for (i in 0..(one?.width ?: 0) - (two?.width ?: 0) step f) {
            for (j in 0..(one?.height ?: 0) - (two?.height ?: 0) step f) {
                compareSubset(i, j, f)
            }
        }

        return difference < 0.1
    }

    private fun compareSubset(a: Int, b: Int, f: Int) {
        var diff = 0.0
        for (i in 0 until (two?.width ?: 0) step f) {
            for (j in 0 until (two?.height ?: 0) step f) {
                val onepx = one?.getRGB(i + a, j + b) ?: 0
                val twopx = two?.getRGB(i, j) ?: 0
                val r1 = (onepx shr 16)
                val g1 = (onepx shr 8) and 0xff
                val b1 = (onepx) and 0xff
                val r2 = (twopx shr 16)
                val g2 = (twopx shr 8) and 0xff
                val b2 = (twopx) and 0xff
                diff += (abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)) / 3.0 / 255.0
            }
        }
        val percentDiff = diff * f * f / ((two?.width ?: 0) * (two?.height ?: 0))
        if (percentDiff < difference || difference == 0.0) {
            difference = percentDiff
            x = a
            y = b
        }
    }

    fun getFirstImage(): BufferedImage? {
        return one
    }

    fun setFirstImage(one: String) {
        val bi = ImageIO.read(File(one))
        this.one = bi
    }

    fun getSecondImage(): BufferedImage? {
        return two
    }

    fun setSecondImage(two: String) {
        val bi = ImageIO.read(File(two))
        this.two = bi
    }

    fun getDifference(): Double? {
        return difference
    }
}