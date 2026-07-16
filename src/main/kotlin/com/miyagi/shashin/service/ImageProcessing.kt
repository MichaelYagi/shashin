package com.miyagi.shashin.service

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.javascript.jscomp.jarjar.com.google.common.io.Files
import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.NetworkUtils
import com.miyagi.shashin.util.TextUtils
import com.twelvemonkeys.image.ConvolveWithEdgeOp
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.springframework.context.MessageSource
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ConvolveOp
import java.awt.image.DataBufferInt
import java.awt.image.Kernel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Base64
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


@Suppress("UNCHECKED_CAST")
class ImageProcessing(private var apiVersion: String?, private var file: File, private var sidecarDir: String, private var metadataObj: Metadata?) {

    private var logger: Logger = Logger.getLogger(ImageProcessing::class.simpleName)

    fun createThumbnails(): Metadata? {
        var _metadataObj = metadataObj

        // Check rotation
        var rotation = 0
        val fileMetadata: com.drew.metadata.Metadata
        try {
            fileMetadata = ImageMetadataReader.readMetadata(file)
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Error reading Metadata for " + file.name + ": " + e.message
            )

            return _metadataObj
        }
        var jpegImageWidth = false
        var jpegImageHeight = false
        var rotationFromExif = false
        var originalPixelWidth: Int? = null
        var originalPixelHeight: Int? = null
        try {
            for (directory in fileMetadata.directories) {
                for (tag in directory.tags) {
                    when (tag.tagName) {
                        "Orientation", "Rotation" -> {
                            if ((tag.description.contains("Rotate") && ((!jpegImageHeight && !jpegImageWidth) || directory.name == "Exif IFD0")) || directory.name == "MP4" || directory.name == "QuickTime") {
                                val digit = tag.description.filter { it.isDigit() }
                                if (TextUtils.Companion.isInteger(digit)) {
                                    rotation = digit.toInt()
                                    rotationFromExif = true
                                }
                            }
                        }

                        "Exif Image Height", "Height", "Image Height" -> {
                            val heightValue = tag.description.filter { it.isDigit() }

                            if (jpegImageHeight) {
                                continue
                            }

                            if (directory.name == "JPEG" && tag.tagName == "Image Height") {
                                jpegImageHeight = true
                            }

                            if (jpegImageHeight) {
                                continue
                            }

                            if (directory.name == "JPEG" && tag.tagName == "Image Height") {
                                jpegImageHeight = true
                            }

                            if ((originalPixelHeight == null && heightValue != "") || (originalPixelHeight != null && heightValue.toInt() > originalPixelHeight)) {
                                originalPixelHeight = heightValue.toInt()
                            }
                        }

                        "Exif Image Width", "Width", "Image Width" -> {
                            val widthValue = tag.description.filter { it.isDigit() }

                            if (jpegImageWidth) {
                                continue
                            }

                            if (directory.name == "JPEG" && tag.tagName == "Image Width") {
                                jpegImageWidth = true
                            }

                            if ((originalPixelWidth == null && widthValue != "") || (originalPixelWidth != null && widthValue.toInt() > originalPixelWidth)) {
                                originalPixelWidth = widthValue.toInt()
                            }
                        }
                    }
                }
            }
//        println("rotation:$rotation")
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not get rotation when reading metadata for " + file.name + ": " + e.message
            )
        }

        // Map path to sidecar file
        val fileRootDir: String = FileUtils.Companion.getRootDir(file)
        val supportedImageFormats = FileUtils.Companion.allowableImageFiles()
        val supportedVideoFormats = FileUtils.Companion.allowableVideoFiles()
        var mediaExtension = FileUtils.Companion.probeFileExtension(file)
        var extension = "jpg"

        var img: BufferedImage? = null
        if (FileUtils.Companion.isRaw(mediaExtension)) {
            try {
                img = ImageIO.read(file)
                if (rotation != 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedImageFormats.contains(mediaExtension)) {
            extension = mediaExtension
            try {
                img = ImageIO.read(file)
//                if (extension == "png" || extension == "gif") {
                    // make sure it's rgb
                    val tempImg = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
                    tempImg.createGraphics().drawImage(img, 0, 0, img.width, img.height, Color.WHITE, null)
                    img = tempImg
//                }
                if (rotation != 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedVideoFormats.contains(mediaExtension)) {
            // Grab screenshot
            val videoProcessing = VideoProcessing(file)
            img = videoProcessing.getVideoScreenshot()
//            _metadataObj?.setVideoUrl("/api/$apiVersion/original/video$fileRootDir/" + file.name)
        }

        if (supportedVideoFormats.contains(mediaExtension) && !rotationFromExif && rotation == 0) {
            rotation = getVideoRotation(file)?.toInt() ?: 0
        }

        // Create thumbnails
        if (img != null && _metadataObj != null) {
            if (originalPixelHeight == null || originalPixelWidth == null) {
                originalPixelHeight = img.height
                originalPixelWidth = img.width
            }

            if (_metadataObj.getOriginalImageWidth() == null && _metadataObj.getOriginalImageHeight() == null) {
                if (rotation == 90 || rotation == 270 || rotation == -90 || rotation == -270) {
                    _metadataObj.setOriginalImageWidth(originalPixelHeight)
                    _metadataObj.setOriginalImageHeight(originalPixelWidth)
                } else {
                    _metadataObj.setOriginalImageWidth(originalPixelWidth)
                    _metadataObj.setOriginalImageHeight(originalPixelHeight)
                }
            }

            _metadataObj.setFolder(fileRootDir)

            _metadataObj = setThumbnails(
                img,
                _metadataObj,
                (FileUtils.Companion.isRaw(mediaExtension) || supportedVideoFormats.contains(mediaExtension)),
                extension
            )
        } else {
            logger.log(Level.WARNING, "File not supported: " + file.name)
            _metadataObj = null
        }

        return _metadataObj
    }

    fun setThumbnails(
        img: BufferedImage,
        metadataObj: Metadata,
        isRawOrVideo: Boolean,
        extension: String,
        overwriteThumbnails: Boolean = false
    ): Metadata {
        if (file.exists()) {
            val mediaExtension = FileUtils.Companion.probeFileExtension(file)

            val thumbnailDirectory = sidecarDir.dropLast(1) + "/thumbnails"
            val fileRootDir: String = FileUtils.Companion.getRootDir(file)
            val tempFile = File(System.getProperty("java.io.tmpdir") + "/temp.jpg")

            // Raw file to image conversion
            var thumbnailFileStr: String
            var tnFile: File?
            if (isRawOrVideo) {
                thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_original." + extension
                tnFile = FileUtils.Companion.createFile(
                    thumbnailFileStr,
                    overwriteThumbnails
                )
                if (tnFile != null) {
                    Thumbnails.of(img)
                        .size(img.width, img.height)
                        .outputQuality(0.3)
                        .toFile(tnFile)
                }

//                metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_original." + extension)
                metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/image/${metadataObj.getId()}")
            }

            metadataObj.setOriginalImageWidth(img.width)
            metadataObj.setOriginalImageHeight(img.height)

            // Gallery small thumbnails
            thumbnailFileStr =
                thumbnailDirectory + fileRootDir + "/" + file.name + "_" + FileUtils.Companion.thumbnailHeight() + "." + extension
            tnFile = FileUtils.Companion.createFile(
                thumbnailFileStr,
                overwriteThumbnails
            )

            if (tnFile != null) {
                val thumbnails = Thumbnails.of(img)
                    .outputQuality(1.0)
                if (mediaExtension == "gif") {
                    thumbnails
                        .imageType(BufferedImage.TYPE_INT_ARGB)
                }
                // If panorama dimensions
                if (img.width > img.height * 2) {
                    thumbnails
                        .crop(Positions.CENTER)
                        .size(FileUtils.Companion.thumbnailHeight(), FileUtils.Companion.thumbnailHeight())
                } else {
                    thumbnails
                        .height(FileUtils.Companion.thumbnailHeight())
                }
                thumbnails.toFile(tempFile)

                if (tempFile.exists()) {
                    val scaledImage: BufferedImage?
                    try {
                        val bi = ImageIO.read(tempFile)
                        scaledImage = sharpenAndBrightenImage(bi)
                        ImageIO.write(scaledImage, "jpg", tnFile)
                        metadataObj.setThumbnailSmallHeight(scaledImage.height)
                        metadataObj.setThumbnailSmallWidth(scaledImage.width)
                        metadataObj.setThumbnailPathSmall(thumbnailFileStr)
//                        metadataObj.setThumbnailUrlSmall("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_" + FileUtils.Companion.thumbnailHeight() + "." + extension)
                        metadataObj.setThumbnailUrlSmall("/api/$apiVersion/thumbnails/225/${metadataObj.getId()}")
                        logger.log(Level.INFO, "Small thumbnail created: " + file.path)
                    } catch (e: IOException) {
                        logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                    }
                } else {
                    logger.log(Level.WARNING, "File DNE: " + tnFile.path)
                }
            }

            // Square image thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_centered." + extension
            tnFile = FileUtils.Companion.createFile(
                thumbnailFileStr,
                overwriteThumbnails
            )
            if (tnFile != null) {
                Thumbnails.of(img)
                    .crop(Positions.CENTER)
                    .size(209, 209)
                    .outputQuality(1.0)
                    .toFile(tempFile)

                if (tempFile.exists()) {
                    val scaledImage: BufferedImage?
                    try {
                        val bi = ImageIO.read(tempFile)
                        scaledImage = sharpenAndBrightenImage(bi)
                        ImageIO.write(scaledImage, "jpg", tnFile)
                        metadataObj.setThumbnailPathCentered(thumbnailFileStr)
//                        metadataObj.setThumbnailUrlCentered("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_centered." + extension)
                        metadataObj.setThumbnailUrlCentered("/api/$apiVersion/thumbnails/centered/${metadataObj.getId()}")
                        logger.log(Level.INFO, "Centered thumbnail created: " + file.path)
                    } catch (e: IOException) {
                        logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                    }
                } else {
                    logger.log(Level.WARNING, "File DNE: " + tnFile.path)
                }
            }

            // Map marker thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_mapmarker." + extension
            tnFile = FileUtils.Companion.createFile(
                thumbnailFileStr,
                overwriteThumbnails
            )
            if (tnFile != null) {
                Thumbnails.of(img)
                    .crop(Positions.CENTER)
                    .size(45, 45)
                    .outputQuality(1.0)
                    .toFile(tempFile)

                if (tempFile.exists()) {
                    var scaledImage: BufferedImage?
                    try {
                        scaledImage = ImageIO.read(tempFile)
                        scaledImage = sharpenAndBrightenImage(scaledImage)
                        scaledImage = borderImage(scaledImage)
                        ImageIO.write(scaledImage, "jpg", tnFile)
                        metadataObj.setMapMarkerPath(thumbnailFileStr)
//                        metadataObj.setMapMarkerUrl("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_mapmarker." + extension)
                        metadataObj.setMapMarkerUrl("/api/$apiVersion/thumbnails/map/${metadataObj.getId()}")
                        logger.log(Level.INFO, "Map thumbnail created: " + file.path)
                    } catch (e: IOException) {
                        logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                    }
                } else {
                    logger.log(Level.WARNING, "File DNE: " + tnFile.path)
                }
            }

            tempFile.delete()
        } else {
            logger.log(Level.WARNING, "File " + file.path + " does not exist.")
        }

        return metadataObj
    }

    private fun getVideoRotation(file: File): Double? {
        try {
            val videoProcessing = VideoProcessing(file)
            return videoProcessing.getVideoRotation()
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Could not get rotation for video " + file.name + ": " + e.message)
            return null
        }

        return null
    }

    private fun blurImage(img: BufferedImage): BufferedImage {
        val radius = 11
        val size = radius * 2 + 1
        val weight = 1.0f / (size * size)
        val data = FloatArray(size * size)

        for (i in data.indices) {
            data[i] = weight
        }

        val kernel = Kernel(size, size, data)
        val op: BufferedImageOp = ConvolveWithEdgeOp(kernel, 2, null)
        
        return op.filter(img, null)
    }

    companion object {
        private var logger: Logger = Logger.getLogger(FileUtils::class.simpleName)

        private val NCPUS = Runtime.getRuntime().availableProcessors()

        fun getBase64(file: File?, includePrefix: Boolean = true): String? {
            try {
                if (file != null) {
                    val os = ByteArrayOutputStream()
                    ImageIO.write(ImageIO.read(file), "jpg", os)

                    return if (includePrefix == true) {
                        "data:image/png;base64, ${Base64.getEncoder().encodeToString(os.toByteArray())}"
                    } else {
                        Base64.getEncoder().encodeToString(os.toByteArray())
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error getBase64: ${e.message}")
            }

            return null
        }

        fun getBase64(bi: BufferedImage?, includePrefix: Boolean = true): String? {
            try {
                if (bi != null) {
                    val os = ByteArrayOutputStream()
                    ImageIO.write(bi, "jpg", os)

                    return if (includePrefix == true) {
                        "data:image/png;base64, ${Base64.getEncoder().encodeToString(os.toByteArray())}"
                    } else {
                        Base64.getEncoder().encodeToString(os.toByteArray())
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error getBase64: ${e.message}")
            }

            return null
        }

        fun createVideoGif(metadataId: String, metadataRepository: MetadataRepository?, overwrite: Boolean = false) {

            val metadataObj = metadataRepository?.findByMetadataId(metadataId)

            if (metadataObj != null && metadataObj.getType()!!.contains("video", ignoreCase = true)) {
                val gifFilePath = metadataObj.getThumbnailPathSmall()!!.replace("_225.jpg", "_225.gif")
                val gifFile = File(gifFilePath)

                if (((overwrite && gifFile.exists()) || !gifFile.exists()) && metadataObj.getPath() != null) {
                    val videoProcessing = VideoProcessing(File(metadataObj.getPath()!!))
                    val processedGifFile = videoProcessing.getVideoGifFile()

                    if (processedGifFile != null) {
                        Files.copy(processedGifFile, gifFile)
                    }
                }
            }
        }

        fun flipVertically(buffImage: BufferedImage): BufferedImage {
            val tx = AffineTransform.getScaleInstance(-1.0, 1.0)
            tx.translate(-buffImage.width.toDouble(), 0.0)
            val op = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
            return op.filter(buffImage, null)
        }

        fun flipHorizontally(buffImage: BufferedImage): BufferedImage {
            val tx = AffineTransform.getScaleInstance(1.0, -1.0)
            tx.translate(0.0, -buffImage.height.toDouble())
            val op = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
            return op.filter(buffImage, null)
        }

        fun rotateImage(buffImage: BufferedImage, angle: Double): BufferedImage {
            val radian = Math.toRadians(angle)
            val sin = abs(sin(radian))
            val cos = abs(cos(radian))
            val width = buffImage.width
            val height = buffImage.height
            val nWidth = floor(width.toDouble() * cos + height.toDouble() * sin).toInt()
            val nHeight = floor(height.toDouble() * cos + width.toDouble() * sin).toInt()
            val rotatedImage = BufferedImage(
                nWidth, nHeight, BufferedImage.TYPE_INT_RGB
            )
            val graphics = rotatedImage.createGraphics()
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            )
            graphics.translate((nWidth - width) / 2, (nHeight - height) / 2)
            // rotation around the center point
            graphics.rotate(radian, (width / 2).toDouble(), (height / 2).toDouble())
            graphics.drawImage(buffImage, 0, 0, null)
            graphics.dispose()
            return rotatedImage
        }

        fun adjustBrightness(image: BufferedImage, brightness: Double): BufferedImage {
            val width = image.width
            val height = image.height

            val rgbImage = if (image.type != BufferedImage.TYPE_INT_RGB) {
                val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                converted.graphics.drawImage(image, 0, 0, null)
                converted
            } else {
                image
            }

            val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val srcPixels = (rgbImage.raster.dataBuffer as DataBufferInt).data
            val destPixels = (result.raster.dataBuffer as DataBufferInt).data

            val brightnessLUT = IntArray(256) { i ->
                (i * brightness).toInt().coerceIn(0, 255)
            }

            val availableCores = Runtime.getRuntime().availableProcessors()
            val rowsPerChunk = maxOf(1, height / availableCores)
            val threadPool = Executors.newFixedThreadPool(availableCores)
            val dispatcher = threadPool.asCoroutineDispatcher()

            runBlocking(dispatcher) {
                val jobs = (0 until height step rowsPerChunk).map { startY ->
                    launch {
                        val endY = minOf(startY + rowsPerChunk, height)
                        for (y in startY until endY step 8) {
                            for (x in 0 until width step 8) {
                                val blockHeight = minOf(8, endY - y)
                                val blockWidth = minOf(8, width - x)
                                for (by in 0 until blockHeight) {
                                    for (bx in 0 until blockWidth) {
                                        val index = (y + by) * width + (x + bx)
                                        val rgb = srcPixels[index]
                                        val r = brightnessLUT[(rgb ushr 16) and 0xFF]
                                        val g = brightnessLUT[(rgb ushr 8) and 0xFF]
                                        val b = brightnessLUT[rgb and 0xFF]
                                        destPixels[index] = (r shl 16) or (g shl 8) or b
                                    }
                                }
                            }
                        }
                    }
                }
                jobs.joinAll()
            }

            threadPool.shutdown()
            return result
        }

        fun adjustContrast(image: BufferedImage, contrast: Double): BufferedImage {
            val width = image.width
            val height = image.height

            val rgbImage = if (image.type != BufferedImage.TYPE_INT_RGB) {
                val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                converted.graphics.drawImage(image, 0, 0, null)
                converted
            } else {
                image
            }

            val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val srcPixels = (rgbImage.raster.dataBuffer as DataBufferInt).data
            val destPixels = (result.raster.dataBuffer as DataBufferInt).data

            val contrastLUT = IntArray(256) { i ->
                (((i - 128) * contrast + 128).toInt()).coerceIn(0, 255)
            }

            val availableCores = Runtime.getRuntime().availableProcessors()
            val rowsPerChunk = maxOf(1, height / availableCores)
            val threadPool = Executors.newFixedThreadPool(availableCores)
            val dispatcher = threadPool.asCoroutineDispatcher()

            runBlocking(dispatcher) {
                val jobs = (0 until height step rowsPerChunk).map { startY ->
                    launch {
                        val endY = minOf(startY + rowsPerChunk, height)
                        for (y in startY until endY step 8) {
                            for (x in 0 until width step 8) {
                                val blockHeight = minOf(8, endY - y)
                                val blockWidth = minOf(8, width - x)
                                for (by in 0 until blockHeight) {
                                    for (bx in 0 until blockWidth) {
                                        val index = (y + by) * width + (x + bx)
                                        val rgb = srcPixels[index]
                                        val r = contrastLUT[(rgb ushr 16) and 0xFF]
                                        val g = contrastLUT[(rgb ushr 8) and 0xFF]
                                        val b = contrastLUT[rgb and 0xFF]
                                        destPixels[index] = (r shl 16) or (g shl 8) or b
                                    }
                                }
                            }
                        }
                    }
                }
                jobs.joinAll()
            }

            threadPool.shutdown()
            return result
        }

        fun adjustSaturation(image: BufferedImage, saturation: Float, brightness: Float = 1.0f, contrast: Float = 1.0f): BufferedImage {
            val width = image.width
            val height = image.height

            val rgbImage = if (image.type != BufferedImage.TYPE_INT_RGB) {
                val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                converted.graphics.drawImage(image, 0, 0, null)
                converted
            } else {
                image
            }

            val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val srcPixels = (rgbImage.raster.dataBuffer as DataBufferInt).data
            val destPixels = (result.raster.dataBuffer as DataBufferInt).data

            val saturationAdj = saturation.coerceIn(0.0f, 2.0f)

            val threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            val dispatcher = threadPool.asCoroutineDispatcher()
            val rowsPerChunk = maxOf(1, height / Runtime.getRuntime().availableProcessors())

            runBlocking(dispatcher) {
                val jobs = (0 until height step rowsPerChunk).map { startY ->
                    launch {
                        val endY = minOf(startY + rowsPerChunk, height)
                        for (y in startY until endY) {
                            for (x in 0 until width) {
                                val index = y * width + x
                                val rgb = srcPixels[index]
                                var r = (rgb ushr 16) and 0xFF
                                var g = (rgb ushr 8) and 0xFF
                                var b = rgb and 0xFF

                                // Apply brightness
                                var rf = (r * brightness).toDouble()
                                var gf = (g * brightness).toDouble()
                                var bf = (b * brightness).toDouble()

                                // Apply contrast
                                rf = ((rf / 255.0 - 0.5) * contrast + 0.5) * 255.0
                                gf = ((gf / 255.0 - 0.5) * contrast + 0.5) * 255.0
                                bf = ((bf / 255.0 - 0.5) * contrast + 0.5) * 255.0

                                // Saturation in RGB space
                                val gray = 0.299 * rf + 0.587 * gf + 0.114 * bf
                                val deltaR = rf - gray
                                val deltaG = gf - gray
                                val deltaB = bf - gray

                                var rSat = gray + deltaR * saturationAdj
                                var gSat = gray + deltaG * saturationAdj
                                var bSat = gray + deltaB * saturationAdj

                                // Red dampening
                                if (saturationAdj > 1.0f) {
                                    val redFactor = 1.0 - 0.10 * (saturationAdj - 1.0)
                                    rSat = gray + deltaR * saturationAdj * redFactor
                                }

                                // Perceptual brightness boost
                                val brightnessBoost = if (saturationAdj > 1.0f) (saturationAdj - 1.0f) * 0.006 else 0.0
                                rSat += brightnessBoost * 255.0
                                gSat += brightnessBoost * 255.0
                                bSat += brightnessBoost * 255.0

                                // Clamp and round
                                val rFinal = rSat.roundToInt().coerceIn(0, 255)
                                val gFinal = gSat.roundToInt().coerceIn(0, 255)
                                val bFinal = bSat.roundToInt().coerceIn(0, 255)

                                destPixels[index] = (rFinal shl 16) or (gFinal shl 8) or bFinal
                            }
                        }
                    }
                }
                jobs.joinAll()
            }

            threadPool.shutdown()
            return result
        }

        fun adjustSharpness(image: BufferedImage, sharpness: Double): BufferedImage {
            val width = image.width
            val height = image.height

            // Convert to TYPE_INT_RGB if needed
            val rgbImage = if (image.type != BufferedImage.TYPE_INT_RGB) {
                val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                converted.graphics.drawImage(image, 0, 0, null)
                converted
            } else {
                image
            }

            val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val srcPixels = (rgbImage.raster.dataBuffer as DataBufferInt).data
            val destPixels = (result.raster.dataBuffer as DataBufferInt).data

            // Resolution-aware sampling scale (matching WebGL)
            val resolutionScale = sqrt((width * height).toDouble()).div(512.0).clamp(0.5, 2.0)

            // Sharpening parameters (matching WebGL exactly)
            val sharpenWeight = if (sharpness > 1.0) (sharpness - 1.0) / 4.5 else 0.0 // Map 1-10 to 0-2
            val centerWeight = 1.0 + 4.0 * sharpenWeight
            val neighborWeight = -sharpenWeight

            val availableCores = Runtime.getRuntime().availableProcessors()
            val rowsPerChunk = maxOf(1, height / availableCores)
            val threadPool = Executors.newFixedThreadPool(availableCores)
            val dispatcher = threadPool.asCoroutineDispatcher()

            runBlocking(dispatcher) {
                val jobs = (0 until height step rowsPerChunk).map { startY ->
                    launch {
                        val endY = minOf(startY + rowsPerChunk, height)
                        for (y in startY until endY) {
                            for (x in 0 until width) {
                                val index = y * width + x

                                if (sharpness <= 1.0) {
                                    // No sharpening, copy original pixel
                                    destPixels[index] = srcPixels[index]
                                    continue
                                }

                                // Apply 3x3 sharpening kernel with bilinear-like sampling
                                var rTotal = 0.0
                                var gTotal = 0.0
                                var bTotal = 0.0
                                var totalWeight = 1.0

                                // Center pixel
                                val rgbCenter = srcPixels[index]
                                val rCenter = ((rgbCenter ushr 16) and 0xFF).toDouble()
                                val gCenter = ((rgbCenter ushr 8) and 0xFF).toDouble()
                                val bCenter = (rgbCenter and 0xFF).toDouble()
                                rTotal += rCenter * centerWeight
                                gTotal += gCenter * centerWeight
                                bTotal += bCenter * centerWeight

                                // Neighbor pixels with resolution-adjusted sampling
                                val offsets = listOf(
                                    intArrayOf(-1, 0), // Left
                                    intArrayOf(1, 0),  // Right
                                    intArrayOf(0, -1), // Up
                                    intArrayOf(0, 1)   // Down
                                )

                                for (offset in offsets) {
                                    // Simulate WebGL linear interpolation by averaging neighboring pixels
                                    val nx = x + (offset[0] * resolutionScale).toInt()
                                    val ny = y + (offset[1] * resolutionScale).toInt()
                                    var rNeighbor = 0.0
                                    var gNeighbor = 0.0
                                    var bNeighbor = 0.0
                                    var sampleCount = 0

                                    // Sample a 2x2 grid around the neighbor to approximate bilinear filtering
                                    for (dx in 0..1) {
                                        for (dy in 0..1) {
                                            val sampleX = nx + dx
                                            val sampleY = ny + dy
                                            if (sampleX >= 0 && sampleX < width && sampleY >= 0 && sampleY < height) {
                                                val nIndex = sampleY * width + sampleX
                                                val rgb = srcPixels[nIndex]
                                                rNeighbor += ((rgb ushr 16) and 0xFF).toDouble()
                                                gNeighbor += ((rgb ushr 8) and 0xFF).toDouble()
                                                bNeighbor += (rgb and 0xFF).toDouble()
                                                sampleCount++
                                            }
                                        }
                                    }

                                    if (sampleCount > 0) {
                                        rNeighbor /= sampleCount.toDouble()
                                        gNeighbor /= sampleCount.toDouble()
                                        bNeighbor /= sampleCount.toDouble()
                                    } else {
                                        // Edge handling: use center pixel (mimics WebGL CLAMP_TO_EDGE)
                                        rNeighbor = rCenter
                                        gNeighbor = gCenter
                                        bNeighbor = bCenter
                                    }

                                    rTotal += rNeighbor * neighborWeight
                                    gTotal += gNeighbor * neighborWeight
                                    bTotal += bNeighbor * neighborWeight
                                }

                                // Normalize and clamp
                                val r = (rTotal / totalWeight).toInt().coerceIn(0, 255)
                                val g = (gTotal / totalWeight).toInt().coerceIn(0, 255)
                                val b = (bTotal / totalWeight).toInt().coerceIn(0, 255)
                                destPixels[index] = (r shl 16) or (g shl 8) or b
                            }
                        }
                    }
                }
                jobs.joinAll()
            }

            threadPool.shutdown()
            return result
        }

        // Extension function for clamping
        fun Double.clamp(min: Double, max: Double): Double = when {
            this < min -> min
            this > max -> max
            else -> this
        }

        fun sharpenAndBrightenImage(bufferedImage: BufferedImage): BufferedImage {
//        -0.15f, -0.15f, -0.15f,
//        -0.15f, 2.2f, -0.15f,
//        -0.15f, -0.15f, -0.15f
            val fnums = -0.05f
            val kernel = Kernel(
                3, 3, floatArrayOf(
                    fnums, fnums, fnums,
                    fnums, 1.42f, fnums,
                    fnums, fnums, fnums
                )
            )

            val op: BufferedImageOp = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
            return op.filter(bufferedImage, null)
        }

        fun transformAndAdjust(
            srcImage: BufferedImage,
            rotationDegrees: Double = 0.0,
            flipH: Boolean = false,
            flipV: Boolean = false,
            brightness: Double = 1.0,
            contrast: Double = 1.0,
            saturation: Float = 1.0f,
            sharpness: Double = 1.0
        ): BufferedImage {
            val flipX = flipV
            val flipY = flipH
            val needsFlip = flipX || flipY
            val needsRotation = rotationDegrees != 0.0
            val needsColor = brightness != 1.0 || contrast != 1.0 || saturation != 1.0f
            val needsSharpen = sharpness > 1.0

            // Fast path: nothing to do
            if (!needsFlip && !needsRotation && !needsColor && !needsSharpen) {
                return ensureIntRGB(srcImage)
            }

            // Start with the source
            var img: BufferedImage = srcImage

            // 1) Apply flips (if any). Flips keep same dimensions.
            if (needsFlip) {
                img = applyFlipTransform(img, flipX, flipY)
            }

            // 2) Color adjustments (single pass) if requested
            if (needsColor) {
                img = applyColorAdjustmentsSinglePass(ensureIntRGB(img), brightness, contrast, saturation)
            }

            // 3) Sharpen if requested (still before rotation as requested)
            if (needsSharpen) {
                img = applySharpen(ensureIntRGB(img), sharpness)
            }

            // 4) Apply rotation last (if requested)
            if (needsRotation) {
                img = applyRotation(img, rotationDegrees)
            }

            return img
        }

        // -------------------------
        // Flip transform only (no rotation)
        // -------------------------
        private fun applyFlipTransform(
            image: BufferedImage,
            flipH: Boolean,
            flipV: Boolean
        ): BufferedImage {
            if (!flipH && !flipV) return image

            val width = image.width
            val height = image.height

            val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val g = dest.createGraphics()
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)

                val tx = AffineTransform()

                var scaleX = 1.0
                var scaleY = 1.0
                var txExtraX = 0.0
                var txExtraY = 0.0

                if (flipH) {
                    scaleX = -scaleX
                    txExtraX = -width.toDouble()
                }
                if (flipV) {
                    scaleY = -scaleY
                    txExtraY = -height.toDouble()
                }
                tx.scale(scaleX, scaleY)
                tx.translate(txExtraX, txExtraY)

                g.drawImage(image, tx, null)
            } finally {
                g.dispose()
            }
            return dest
        }

        // -------------------------
        // Rotation only (applied last)
        // -------------------------
        private fun applyRotation(image: BufferedImage, rotationDegrees: Double): BufferedImage {
            if (rotationDegrees == 0.0) return image

            val radian = Math.toRadians(rotationDegrees)
            val sin = abs(kotlin.math.sin(radian))
            val cos = abs(kotlin.math.cos(radian))
            val width = image.width
            val height = image.height
            val nWidth = floor(width.toDouble() * cos + height.toDouble() * sin).toInt()
            val nHeight = floor(height.toDouble() * cos + width.toDouble() * sin).toInt()
            val rotatedImage = BufferedImage(nWidth, nHeight, BufferedImage.TYPE_INT_RGB)
            val graphics = rotatedImage.createGraphics()
            try {
                graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
                )
                // center translated so rotated image is centered
                graphics.translate((nWidth - width) / 2.0, (nHeight - height) / 2.0)
                // rotation around the center point of the original image
                graphics.rotate(radian, (width / 2).toDouble(), (height / 2).toDouble())
                graphics.drawImage(image, 0, 0, null)
            } finally {
                graphics.dispose()
            }
            return rotatedImage
        }

        // -------------------------
        // Color adjustments (single-pass)
        // -------------------------
        private fun applyColorAdjustmentsSinglePass(
            image: BufferedImage,
            brightness: Double,
            contrast: Double,
            saturation: Float
        ): BufferedImage {
            val width = image.width
            val height = image.height

            val rgbImage = if (image.type != BufferedImage.TYPE_INT_RGB) {
                val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                val g = converted.createGraphics()
                try {
                    g.drawImage(image, 0, 0, null)
                } finally {
                    g.dispose()
                }
                converted
            } else {
                image
            }

            val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val srcPixels = (rgbImage.raster.dataBuffer as DataBufferInt).data
            val destPixels = (result.raster.dataBuffer as DataBufferInt).data

            val satAdj = saturation.coerceIn(0.0f, 2.0f)
            val brightnessD = brightness
            val contrastD = contrast

            val rowsPerChunk = maxOf(1, height / NCPUS)
            val exec = Executors.newFixedThreadPool(NCPUS)
            try {
                val tasks = ArrayList<Callable<Unit>>()
                var startY = 0
                while (startY < height) {
                    val s = startY
                    val e = minOf(s + rowsPerChunk, height)
                    tasks.add(Callable {
                        for (y in s until e) {
                            val rowBase = y * width
                            for (x in 0 until width) {
                                val idx = rowBase + x
                                val rgb = srcPixels[idx]
                                var r = ((rgb ushr 16) and 0xFF).toDouble()
                                var g = ((rgb ushr 8) and 0xFF).toDouble()
                                var b = (rgb and 0xFF).toDouble()

                                // Brightness (multiplicative)
                                r *= brightnessD
                                g *= brightnessD
                                b *= brightnessD

                                // Contrast
                                r = ((r / 255.0 - 0.5) * contrastD + 0.5) * 255.0
                                g = ((g / 255.0 - 0.5) * contrastD + 0.5) * 255.0
                                b = ((b / 255.0 - 0.5) * contrastD + 0.5) * 255.0

                                if (satAdj != 1.0f) {
                                    val gray = 0.299 * r + 0.587 * g + 0.114 * b
                                    val dr = r - gray
                                    val dg = g - gray
                                    val db = b - gray

                                    var rSat = gray + dr * satAdj
                                    var gSat = gray + dg * satAdj
                                    var bSat = gray + db * satAdj

                                    if (satAdj > 1.0f) {
                                        val redFactor = 1.0 - 0.10 * (satAdj - 1.0f)
                                        rSat = gray + dr * satAdj * redFactor
                                    }

                                    if (satAdj > 1.0f) {
                                        val brightnessBoost = (satAdj - 1.0f) * 0.006
                                        rSat += brightnessBoost * 255.0
                                        gSat += brightnessBoost * 255.0
                                        bSat += brightnessBoost * 255.0
                                    }

                                    r = rSat
                                    g = gSat
                                    b = bSat
                                }

                                val rf = r.roundToInt().coerceIn(0, 255)
                                val gf = g.roundToInt().coerceIn(0, 255)
                                val bf = b.roundToInt().coerceIn(0, 255)
                                destPixels[idx] = (rf shl 16) or (gf shl 8) or bf
                            }
                        }
                    })
                    startY += rowsPerChunk
                }
                exec.invokeAll(tasks).forEach { /* waits */ }
            } finally {
                exec.shutdown()
                exec.awaitTermination(5, TimeUnit.SECONDS)
            }

            return result
        }

        // -------------------------
        // Sharpen pass (optimized)
        // -------------------------
        private fun applySharpen(image: BufferedImage, sharpness: Double): BufferedImage {
            val width = image.width
            val height = image.height

            val srcPixels = (image.raster.dataBuffer as DataBufferInt).data
            val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val destPixels = (dest.raster.dataBuffer as DataBufferInt).data

            // Helpers
            fun sampleBilinear(fx: Double, fy: Double): Triple<Double, Double, Double> {
                // clamp to image extent (CLAMP_TO_EDGE)
                val cx = fx.coerceIn(0.0, (width - 1).toDouble())
                val cy = fy.coerceIn(0.0, (height - 1).toDouble())

                val x0 = floor(cx).toInt()
                val y0 = floor(cy).toInt()
                val x1 = min(x0 + 1, width - 1)
                val y1 = min(y0 + 1, height - 1)

                val wx = cx - x0
                val wy = cy - y0

                val i00 = y0 * width + x0
                val i10 = y0 * width + x1
                val i01 = y1 * width + x0
                val i11 = y1 * width + x1

                val rgb00 = srcPixels[i00]
                val rgb10 = srcPixels[i10]
                val rgb01 = srcPixels[i01]
                val rgb11 = srcPixels[i11]

                val r00 = ((rgb00 ushr 16) and 0xFF).toDouble() / 255.0
                val g00 = ((rgb00 ushr 8) and 0xFF).toDouble() / 255.0
                val b00 = (rgb00 and 0xFF).toDouble() / 255.0

                val r10 = ((rgb10 ushr 16) and 0xFF).toDouble() / 255.0
                val g10 = ((rgb10 ushr 8) and 0xFF).toDouble() / 255.0
                val b10 = (rgb10 and 0xFF).toDouble() / 255.0

                val r01 = ((rgb01 ushr 16) and 0xFF).toDouble() / 255.0
                val g01 = ((rgb01 ushr 8) and 0xFF).toDouble() / 255.0
                val b01 = (rgb01 and 0xFF).toDouble() / 255.0

                val r11 = ((rgb11 ushr 16) and 0xFF).toDouble() / 255.0
                val g11 = ((rgb11 ushr 8) and 0xFF).toDouble() / 255.0
                val b11 = (rgb11 and 0xFF).toDouble() / 255.0

                // bilinear interpolation
                val rTop = r00 * (1.0 - wx) + r10 * wx
                val rBot = r01 * (1.0 - wx) + r11 * wx
                val r = rTop * (1.0 - wy) + rBot * wy

                val gTop = g00 * (1.0 - wx) + g10 * wx
                val gBot = g01 * (1.0 - wx) + g11 * wx
                val g = gTop * (1.0 - wy) + gBot * wy

                val bTop = b00 * (1.0 - wx) + b10 * wx
                val bBot = b01 * (1.0 - wx) + b11 * wx
                val b = bTop * (1.0 - wy) + bBot * wy

                return Triple(r, g, b)
            }

            fun applyColorAdjust(rgb: Triple<Double, Double, Double>, brightness: Double, contrast: Double, saturation: Double): Triple<Double, Double, Double> {
                var (r, g, b) = rgb
                // brightness (multiplicative)
                r *= brightness; g *= brightness; b *= brightness

                // contrast: ((c - 0.5) * contrast) + 0.5
                r = ((r - 0.5) * contrast) + 0.5
                g = ((g - 0.5) * contrast) + 0.5
                b = ((b - 0.5) * contrast) + 0.5

                // saturation
                val gray = 0.299 * r + 0.587 * g + 0.114 * b
                val dr = r - gray
                val dg = g - gray
                val db = b - gray
                var sr = gray + dr * saturation
                var sg = gray + dg * saturation
                var sb = gray + db * saturation

                // red dampening when saturation > 1.0 (match JS shader)
                if (saturation > 1.0) {
                    val redFactor = 1.0 - 0.10 * (saturation - 1.0)
                    sr = gray + (r - gray) * saturation * redFactor
                }

                // perceptual brightness boost for saturation > 1.0
                if (saturation > 1.0) {
                    val brightnessBoost = (saturation - 1.0) * 0.006
                    sr += brightnessBoost; sg += brightnessBoost; sb += brightnessBoost
                }

                sr = sr.coerceIn(0.0, 1.0)
                sg = sg.coerceIn(0.0, 1.0)
                sb = sb.coerceIn(0.0, 1.0)
                return Triple(sr, sg, sb)
            }

            // Match JS parameter behavior: resolutionScale is fractional and used directly as offsets,
            // and JS applies color adjustments to each sample BEFORE combining.
            val resolutionScale = (sqrt((width * height).toDouble()) / 512.0).coerceIn(0.5, 2.0)
            val sharpenWeight = if (sharpness > 1.0) (sharpness - 1.0) / 4.5 else 0.0
            val centerWeight = 1.0 + 4.0 * sharpenWeight
            val neighborWeight = -sharpenWeight

            // If you have brightness/contrast/saturation params in the Kotlin pipeline, replace these placeholders
            // with the actual parameters used by your JS call. If not used, default to 1.0.
            val brightness = 1.0
            val contrast = 1.0
            val saturation = 1.0

            val rowsPerChunk = maxOf(1, height / NCPUS)
            val exec = Executors.newFixedThreadPool(NCPUS)
            try {
                val tasks = ArrayList<Callable<Unit>>()
                var startY = 0
                while (startY < height) {
                    val s = startY
                    val e = minOf(s + rowsPerChunk, height)
                    tasks.add(Callable {
                        for (y in s until e) {
                            val rowBase = y * width
                            for (x in 0 until width) {
                                val idx = rowBase + x

                                // Compute fractional sample positions in the same way the shader computes UVs:
                                // center sample at pixel center: x + 0.5, y + 0.5
                                val cx = x + 0.5
                                val cy = y + 0.5

                                // offsets are fractional resolutionScale (not truncated)
                                val offs = arrayOf(
                                    doubleArrayOf(-resolutionScale, 0.0),
                                    doubleArrayOf(resolutionScale, 0.0),
                                    doubleArrayOf(0.0, -resolutionScale),
                                    doubleArrayOf(0.0, resolutionScale)
                                )

                                // sample center and apply color adjustments to each sample before combining (match JS)
                                val c0Raw = sampleBilinear(cx, cy)
                                val c0 = applyColorAdjust(c0Raw, brightness, contrast, saturation)

                                var rSum = c0.first * centerWeight
                                var gSum = c0.second * centerWeight
                                var bSum = c0.third * centerWeight

                                for (off in offs) {
                                    val fx = cx + off[0]
                                    val fy = cy + off[1]
                                    val nRaw = sampleBilinear(fx, fy)
                                    val n = applyColorAdjust(nRaw, brightness, contrast, saturation)
                                    rSum += n.first * neighborWeight
                                    gSum += n.second * neighborWeight
                                    bSum += n.third * neighborWeight
                                }

                                // clamp and convert to 0..255
                                val rF = (rSum.coerceIn(0.0, 1.0) * 255.0).roundToInt().coerceIn(0, 255)
                                val gF = (gSum.coerceIn(0.0, 1.0) * 255.0).roundToInt().coerceIn(0, 255)
                                val bF = (bSum.coerceIn(0.0, 1.0) * 255.0).roundToInt().coerceIn(0, 255)

                                destPixels[idx] = (rF shl 16) or (gF shl 8) or bF
                            }
                        }
                    })
                    startY += rowsPerChunk
                }
                exec.invokeAll(tasks).forEach { /* waits */ }
            } finally {
                exec.shutdown()
                exec.awaitTermination(5, TimeUnit.SECONDS)
            }

            return dest
        }

        // -------------------------
        // Helpers
        // -------------------------
        private fun ensureIntRGB(image: BufferedImage): BufferedImage {
            if (image.type == BufferedImage.TYPE_INT_RGB) return image
            val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
            val g = converted.createGraphics()
            try {
                g.drawImage(image, 0, 0, null)
            } finally {
                g.dispose()
            }
            return converted
        }

        private fun Double.coerceIn(min: Double, max: Double): Double = when {
            this < min -> min
            this > max -> max
            else -> this
        }

        fun borderImage(bufferedImage: BufferedImage): BufferedImage {
            val g: Graphics2D = bufferedImage.graphics as Graphics2D
            g.stroke = BasicStroke(4f)
            g.color = Color.WHITE
            g.drawRect(0, 0, bufferedImage.width, bufferedImage.height)

            return bufferedImage
        }

        private fun saveObject(objSubject: String?, metadataObj: Metadata, keywordRepository: KeywordRepository, keywordPhotoRepository: KeywordPhotoRepository, metadataRepository: MetadataRepository) {
            var keywordObj =
                keywordRepository.findByKeywordIgnoreCase(objSubject)
            if (keywordObj == null) {
                keywordObj = Keyword()
                keywordObj.setKeyword(objSubject)
                keywordObj.setCreatedAt(TextUtils.Companion.getCurrentTimestamp())
                keywordObj.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
                keywordRepository.save(keywordObj)
            }

            val keywordPhotoCount =
                keywordPhotoRepository.countByKeywordIdAndMetadataId(
                    keywordObj.getId(),
                    metadataObj.getId()
                )
            if (keywordPhotoCount == 0) {
                val keywordPhotoObj = KeywordPhoto()
                keywordPhotoObj.setKeywordId(keywordObj.getId())
                keywordPhotoObj.setMetadataId(metadataObj.getId())
                keywordPhotoObj.setCreatedAt(TextUtils.Companion.getCurrentTimestamp())
                keywordPhotoObj.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
                keywordPhotoRepository.save(keywordPhotoObj)
                metadataObj.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
                metadataRepository.save(metadataObj)
            }
        }

        // Store a single face detection from an Argus detect response. Argus returns the match
        // inline: if it assigned an identity, link the RecognitionLabelPhoto to the Shashin person
        // (via argusIdentityId) with the real confidence — auto-tagging it. Otherwise store an
        // unlabeled placeholder, which surfaces in the Matches review tab.
        fun storeFaceDetection(
            faceNode: JsonNode,
            metadataObj: Metadata,
            recognitionLabelRepository: RecognitionLabelRepository?,
            recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?,
            settings: Settings? = null,
            webClient: WebClient? = null,
            sourceImageId: String? = null
        ) {
            val detectionId = faceNode["detection_id"].asInt().toString()
            metadataObj.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
            val rlp = RecognitionLabelPhoto()
            rlp.setMetadataId(metadataObj.getId())
            rlp.setArgusDetectionId(detectionId)
            rlp.setAutoTagged(true)
            if (sourceImageId != null) rlp.setArgusSourceImageId(sourceImageId)

            val identityIdNode = faceNode.get("identity_id")
            if (identityIdNode != null && !identityIdNode.isNull) {
                val argusId = identityIdNode.asInt()
                var recognitionLabelObj = recognitionLabelRepository?.findByArgusIdentityId(argusId)

                // The identity may have been created directly in Argus, with no local person yet.
                // Create or link one from the returned label so it tags without re-labeling in Shashin.
                if (recognitionLabelObj == null && recognitionLabelRepository != null) {
                    val labelNode = faceNode.get("label")
                    val name = if (labelNode != null && !labelNode.isNull) labelNode.textValue()?.trim() else null
                    if (!name.isNullOrBlank()) {
                        val existingByName = recognitionLabelRepository.findByNameIgnoreCase(name)
                        if (existingByName != null) {
                            // A same-named local person exists — link it to this Argus identity.
                            if (existingByName.getArgusIdentityId() == null) {
                                existingByName.setArgusIdentityId(argusId)
                                existingByName.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
                                try { recognitionLabelRepository.save(existingByName) } catch (_: Exception) {}
                            }
                            recognitionLabelObj = existingByName
                        } else {
                            val newLabel = RecognitionLabel()
                            newLabel.setName(name)
                            newLabel.setArgusIdentityId(argusId)
                            newLabel.setCreatedAt(TextUtils.Companion.getCurrentTimestamp())
                            newLabel.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
                            recognitionLabelObj = try {
                                recognitionLabelRepository.save(newLabel)
                            } catch (_: Exception) {
                                recognitionLabelRepository.findByArgusIdentityId(argusId)
                            }
                        }
                    }
                }

                if (recognitionLabelObj != null) {
                    // Argus assigned an identity inline = confident match. Store as confirmed so it
                    // shows in the person's gallery rather than the review tab.
                    rlp.setRecognitionLabelId(recognitionLabelObj.getId())
                    rlp.setConfidence(
                        if (faceNode.has("similarity") && !faceNode["similarity"].isNull)
                            faceNode["similarity"].asText() else "0.0"
                    )
                    try { recognitionLabelPhotoRepository?.save(rlp) } catch (_: Exception) {}
                    if (webClient != null && settings?.getArgusKey() != null) {
                        try {
                            val mapper2 = ObjectMapper()
                            val extRefBody = mapper2.writeValueAsString(mapOf("external_ref" to recognitionLabelObj.getId().toString()))
                            webClient.put()
                                .uri("api/identities/$argusId/external_ref")
                                .header("X-API-Key", settings.getArgusKey())
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                .body(BodyInserters.fromValue(extRefBody))
                                .retrieve().bodyToMono(String::class.java).block()
                        } catch (_: Exception) {}
                    }
                    return
                }
            }
            // No identity assigned (pending review) or unknown person → unlabeled placeholder
            rlp.setConfidence("0.0")
            try { recognitionLabelPhotoRepository?.save(rlp) } catch (_: Exception) {}
        }

        // Detect faces in one photo via Argus and store the results. Argus returns matches inline,
        // so known people are auto-tagged. When replace=true, Argus clears the image's existing
        // face detections first, making re-detection idempotent (used by rescan).
        fun detectAndStoreFaces(
            metadataObj: Metadata,
            settings: Settings,
            recognitionLabelRepository: RecognitionLabelRepository?,
            recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?,
            metadataRepository: MetadataRepository? = null,
            replace: Boolean = false
        ): Boolean {
            if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return false
            if (recognitionLabelPhotoRepository == null) return false

            try {
                val mapper = ObjectMapper()
                val webClient = WebClient.create(settings.getArgusServer()!!.trimEnd('/') + "/")
                val builder = MultipartBodyBuilder()
                builder.part("file", FileSystemResource(argusImagePath(metadataObj)!!))
                builder.part("external_ref", metadataObj.getId().toString())
                if (replace) builder.part("replace", "true")

                val response = webClient.post()
                    .uri("api/detect/faces")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                    .header("X-API-Key", settings.getArgusKey())
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()

                val jsonObj = mapper.readTree(response)

                val sourceImageId = jsonObj["source_image_id"]?.takeUnless { it.isNull }?.asInt()?.toString()
                val facesNode = if (jsonObj.has("faces")) jsonObj["faces"] else null
                if (facesNode != null && facesNode.size() > 0) {
                    for (faceNode in facesNode) {
                        storeFaceDetection(faceNode, metadataObj, recognitionLabelRepository, recognitionLabelPhotoRepository, settings, webClient, sourceImageId)
                    }
                    try { metadataRepository?.save(metadataObj) } catch (_: Exception) {}
                } else {
                    val noFaceRecord = RecognitionLabelPhoto()
                    noFaceRecord.setMetadataId(metadataObj.getId())
                    noFaceRecord.setConfidence("-0.1")
                    noFaceRecord.setAutoTagged(true)
                    try { recognitionLabelPhotoRepository.save(noFaceRecord) } catch (_: Exception) {}
                }
                return true
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error detecting faces for ${metadataObj.getId()}: ${e.localizedMessage}")
                return false
            }
        }

        // Detect objects in one photo via Argus and store the class names as keywords.
        // The "person" class is skipped (faces handle people). If nothing else is found,
        // an "unidentified objects" sentinel keyword is stored so the photo isn't rescanned.
        fun detectAndStoreObjects(
            metadataObj: Metadata,
            settings: Settings,
            keywordRepository: KeywordRepository?,
            keywordPhotoRepository: KeywordPhotoRepository?,
            metadataRepository: MetadataRepository?,
            threadFile: File? = null,
            messageSource: MessageSource? = null,
            locale: Locale = Locale("en"),
            replace: Boolean = false
        ): Boolean {
            if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return false
            if (keywordRepository == null || keywordPhotoRepository == null || metadataRepository == null) return false

            try {
                val mapper = ObjectMapper()
                val webClient = WebClient.create(settings.getArgusServer()!!.trimEnd('/') + "/")
                val builder = MultipartBodyBuilder()
                builder.part("file", FileSystemResource(argusImagePath(metadataObj)!!))
                builder.part("external_ref", metadataObj.getId().toString())
                if (replace) builder.part("replace", "true")

                val response = webClient.post()
                    .uri("api/detect/objects")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                    .header("X-API-Key", settings.getArgusKey())
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()

                val jsonObj = mapper.readTree(response)
                val objectsNode = if (jsonObj.has("objects")) jsonObj["objects"] else null

                val classNames = mutableListOf<String>()
                if (objectsNode != null) {
                    for (obj in objectsNode) {
                        if (!obj.has("class_name") || obj["class_name"].isNull) continue
                        val className = obj["class_name"].textValue()
                        if (className.equals("person", ignoreCase = true)) continue
                        if (!classNames.contains(className)) classNames.add(className)
                    }
                }

                // Sentinel so a photo with no detectable objects isn't rescanned forever
                if (classNames.isEmpty()) classNames.add("unidentified objects")

                processObjects(classNames, metadataObj, keywordRepository, keywordPhotoRepository, metadataRepository)

                if (threadFile != null) {
                    FileUtils.Companion.writeToThreadFileAndLogMessage(
                        messageSource?.getMessage("main.pages.matching.identified", arrayOf(metadataObj.getPath()), locale)?.toString()
                            ?: "Objects identified for ${metadataObj.getPath()}",
                        threadFile
                    )
                }
                logger.log(Level.INFO, "Objects for ${metadataObj.getId()}: $classNames")
                return true
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error detecting objects for ${metadataObj.getId()}: ${e.localizedMessage}")
                return false
            }
        }

        // Batch object scan over photos that have no keywords yet.
        fun scanObjects(
            metadataRepository: MetadataRepository?,
            keywordRepository: KeywordRepository?,
            keywordPhotoRepository: KeywordPhotoRepository?,
            settings: Settings,
            threadFile: File? = null,
            shouldStop: AtomicBoolean? = null,
            messageSource: MessageSource? = null,
            locale: Locale = Locale("en")
        ): Int {
            if (settings.getObjectDetection() != true) return 0
            if (!NetworkUtils.Companion.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())) return 0

            val photos = metadataRepository?.findWithoutKeywords(settings.getMatchScanLimit()!!) ?: return 0
            var count = 0
            for (photo in photos) {
                if (shouldStop != null && shouldStop.get()) break
                val metadataObj = metadataRepository.findById(photo.getId()).orElse(null) ?: continue
                if (detectAndStoreObjects(metadataObj, settings, keywordRepository, keywordPhotoRepository, metadataRepository, threadFile, messageSource, locale)) {
                    count++
                }
            }
            return count
        }

        fun processObjects(keywordArray: List<String>, metadataObj: Metadata, keywordRepository: KeywordRepository, keywordPhotoRepository: KeywordPhotoRepository, metadataRepository: MetadataRepository) {
            if (keywordArray.isNotEmpty()) {
                for (keyword in keywordArray) {
                    saveObject(
                        keyword,
                        metadataObj,
                        keywordRepository,
                        keywordPhotoRepository,
                        metadataRepository
                    )
                }
            }
        }

        // Argus needs a full-resolution image to detect faces that aren't frame-filling.
        // The 225px thumbnail shrinks small/distant faces below the detection threshold.
        // Use the original file for images; videos must use the poster thumbnail since the
        // original is not a decodable still image.
        // Mirrors the resolution logic of /api/v1/thumbnails/original/{id}:
        // RAW files and videos have a rotation-corrected _original.jpg sidecar written at scan time;
        // regular JPEGs/PNGs fall back to the raw file (EXIF orientation tag is present for those).
        fun argusImagePath(metadata: Metadata): String? {
            val isVideo = metadata.getType()?.contains("video") == true
            val smallPath = metadata.getThumbnailPathSmall()
            if (smallPath != null) {
                val originalPath = smallPath.replace("_${FileUtils.Companion.thumbnailHeight()}.", "_original.")
                if (File(originalPath).exists()) return originalPath
            }
            // Videos must use a JPEG frame — never send the raw video file to Argus.
            return if (isVideo) smallPath else metadata.getPath()
        }

        fun buildPersonUpload(settings: Settings, personName: String?, metadata: Metadata?, argusDetectionIdMap: MutableMap<String, Any?>, recognitionLabelRepository: RecognitionLabelRepository? = null): MutableMap<String, Any?> {
            val mapper = ObjectMapper()
            val uploadResponse = mutableMapOf<String, Any?>()
            uploadResponse["responseData"] = mutableMapOf<String, Any?>()
            uploadResponse["similarity"] = 0.0

            uploadResponse["msg"] = ""
            uploadResponse["status"] = ApiResponse.FAIL.status

            if (NetworkUtils.Companion.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())) {
                if (!personName.isNullOrBlank() && !metadata?.getId().isNullOrBlank()) {
                    try {
                        val webClient = WebClient.create(settings.getArgusServer()!!.trimEnd('/') + "/")
                        val builder = MultipartBodyBuilder()
                        builder.part("file", FileSystemResource(argusImagePath(metadata!!)!!))
                        builder.part("label", personName)
                        builder.part("external_ref", metadata.getId().toString())

                        val response = webClient.post()
                            .uri("api/detect/faces")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                            .header("X-API-Key", settings.getArgusKey())
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(String::class.java)
                            .block()

                        val jsonObj = mapper.readTree(response)
                        logger.log(Level.INFO, "Face $personName for ${metadata?.getId()} detected+labeled: $response")

                        val responseData = mutableMapOf<String, Any?>()
                        if (jsonObj.has("faces") && jsonObj["faces"].size() > 0) {
                            val face = jsonObj["faces"][0]
                            responseData["detection_id"] = face["detection_id"].asInt()
                            val srcImgId = jsonObj["source_image_id"]?.takeUnless { it.isNull }?.asInt()
                            if (srcImgId != null) responseData["source_image_id"] = srcImgId

                            if (face.has("identity_id") && !face["identity_id"].isNull) {
                                val identityId = face["identity_id"].asInt()
                                val recognitionLabelObj = recognitionLabelRepository?.findByNameIgnoreCase(personName)
                                if (recognitionLabelObj != null) {
                                    if (recognitionLabelObj.getArgusIdentityId() != identityId) {
                                        recognitionLabelObj.setArgusIdentityId(identityId)
                                        recognitionLabelRepository?.save(recognitionLabelObj)
                                    }
                                    try {
                                        val extRefBody = mapper.writeValueAsString(mapOf("external_ref" to recognitionLabelObj.getId().toString()))
                                        webClient.put()
                                            .uri("api/identities/$identityId/external_ref")
                                            .header("X-API-Key", settings.getArgusKey())
                                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                            .body(BodyInserters.fromValue(extRefBody))
                                            .retrieve().bodyToMono(String::class.java).block()
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        uploadResponse["responseData"] = responseData
                        uploadResponse["msg"] = ""
                        uploadResponse["status"] = ApiResponse.SUCCESS.status
                    } catch (e: Exception) {
                        logger.log(Level.WARNING, "Error detecting+labeling face $personName for ${metadata?.getId()}: " + e.localizedMessage)
                        uploadResponse["responseData"] = e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                    }
                } else {
                    logger.log(Level.WARNING, "Person name or metadata ID blank")
                    uploadResponse["msg"] = "Person name or metadata ID blank"
                    uploadResponse["status"] = ApiResponse.FAIL.status
                }
            }

            return uploadResponse
        }

        fun buildPersonRecognition(settings: Settings, metadata: Metadata?): MutableMap<String, Any?> {
            val mapper = ObjectMapper()
            val recognitionResponse = mutableMapOf<String, Any?>()

            recognitionResponse["recognizeData"] = mutableMapOf<String, Any?>()
            recognitionResponse["msg"] = ""
            recognitionResponse["status"] = ApiResponse.FAIL.status

            if (NetworkUtils.Companion.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())) {

                val response: String?

                if (metadata !== null) {
                    try {
                        val webClient = WebClient.create(settings.getArgusServer()!!.trimEnd('/') + "/")

                        val builder = MultipartBodyBuilder()
                        builder.part("file", FileSystemResource(argusImagePath(metadata)!!))

                        response = webClient.post()
                            .uri("api/detect/faces")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                            .header("X-API-Key", settings.getArgusKey())
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(String::class.java)
                            .block()

                        val jsonObj = mapper.readTree(response)
                        // Normalize Argus faces[] to CompreFace-style subjects[] shape so callers are unchanged
                        val normalizedSubjects = ArrayList<Map<String, Any>>()
                        if (jsonObj.has("faces")) {
                            for (face in jsonObj["faces"]) {
                                val subjectMap = mutableMapOf<String, Any>()
                                subjectMap["subject"] = if (face.has("label") && !face["label"].isNull) face["label"].textValue() else ""
                                subjectMap["similarity"] = if (face.has("confidence")) face["confidence"].asDouble() else 0.0
                                normalizedSubjects.add(subjectMap)
                            }
                        }
                        recognitionResponse["recognizeData"] = normalizedSubjects
                        recognitionResponse["msg"] = ""
                        recognitionResponse["status"] = ApiResponse.SUCCESS.status

                    } catch (e: Exception) {
                        val errorResponse =
                            e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                        recognitionResponse["recognizeData"] = errorResponse
                    }
                } else {
                    recognitionResponse["msg"] = "Metadata ID blank"
                    recognitionResponse["status"] = ApiResponse.FAIL.status
                }
            }

            return recognitionResponse
        }

        fun detectAndStoreAll(
            metadataObj: Metadata,
            settings: Settings,
            recognitionLabelRepository: RecognitionLabelRepository?,
            recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?,
            keywordRepository: KeywordRepository?,
            keywordPhotoRepository: KeywordPhotoRepository?,
            metadataRepository: MetadataRepository?,
            doFaces: Boolean = true,
            doObjects: Boolean = true,
            replace: Boolean = false,
            threadFile: File? = null,
            messageSource: MessageSource? = null,
            locale: Locale = Locale("en")
        ): Int {
            if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return 0
            if (!doFaces && !doObjects) return 0
            var recognitionCount = 0
            try {
                val mapper = ObjectMapper()
                val webClient = WebClient.create(settings.getArgusServer()!!.trimEnd('/') + "/")
                val builder = MultipartBodyBuilder()
                builder.part("file", FileSystemResource(argusImagePath(metadataObj)!!))
                builder.part("external_ref", metadataObj.getId().toString())
                if (replace) builder.part("replace", "true")

                val uri = if (doFaces && doObjects) "api/detect/all"
                          else if (doFaces) "api/detect/faces"
                          else "api/detect/objects"
                logger.log(Level.INFO, "[DEBUG] detectAndStoreAll: sending ${metadataObj.getId()} to Argus ($uri)")
                val debugArgusT = System.currentTimeMillis()
                val response = webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                    .header("X-API-Key", settings.getArgusKey())
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()
                logger.log(Level.INFO, "[DEBUG] detectAndStoreAll: Argus responded in ${System.currentTimeMillis() - debugArgusT}ms")

                val jsonObj = mapper.readTree(response)

                if (doFaces && recognitionLabelPhotoRepository != null) {
                    val sourceImageId = jsonObj["source_image_id"]?.takeUnless { it.isNull }?.asInt()?.toString()
                    val facesNode = if (jsonObj.has("faces")) jsonObj["faces"] else null
                    if (facesNode != null && facesNode.size() > 0) {
                        for (faceNode in facesNode) {
                            storeFaceDetection(faceNode, metadataObj, recognitionLabelRepository, recognitionLabelPhotoRepository, settings, webClient, sourceImageId)
                            recognitionCount++
                        }
                        try { metadataRepository?.save(metadataObj) } catch (_: Exception) {}
                    } else {
                        val noFaceRecord = RecognitionLabelPhoto()
                        noFaceRecord.setMetadataId(metadataObj.getId())
                        noFaceRecord.setConfidence("-0.1")
                        noFaceRecord.setAutoTagged(true)
                        try { recognitionLabelPhotoRepository.save(noFaceRecord) } catch (_: Exception) {}
                    }
                }

                if (doObjects && keywordRepository != null && keywordPhotoRepository != null && metadataRepository != null) {
                    val objectsNode = if (jsonObj.has("objects")) jsonObj["objects"] else null
                    val classNames = mutableListOf<String>()
                    if (objectsNode != null) {
                        for (obj in objectsNode) {
                            if (!obj.has("class_name") || obj["class_name"].isNull) continue
                            val className = obj["class_name"].textValue()
                            if (className.equals("person", ignoreCase = true)) continue
                            if (!classNames.contains(className)) classNames.add(className)
                        }
                    }
                    if (classNames.isEmpty()) classNames.add("unidentified objects")
                    processObjects(classNames, metadataObj, keywordRepository, keywordPhotoRepository, metadataRepository)
                }

                if (threadFile != null) {
                    FileUtils.Companion.writeToThreadFileAndLogMessage(
                        messageSource?.getMessage("main.pages.matching.analyzing", arrayOf("", metadataObj.getPath()), locale).toString(),
                        threadFile
                    )
                }
                logger.log(Level.INFO, "detectAndStoreAll ${metadataObj.getId()}: faces=$recognitionCount")
                return recognitionCount
            } catch (e: Exception) {
                if (doFaces && recognitionLabelPhotoRepository != null) {
                    val errorRecord = RecognitionLabelPhoto()
                    errorRecord.setMetadataId(metadataObj.getId())
                    errorRecord.setConfidence("-0.1")
                    errorRecord.setAutoTagged(true)
                    try { recognitionLabelPhotoRepository.save(errorRecord) } catch (_: Exception) {}
                }
                logger.log(Level.WARNING, "Error in detectAndStoreAll for ${metadataObj.getId()}: ${e.localizedMessage}")
                return 0
            }
        }

        fun scanAll(
            metadataRepository: MetadataRepository?,
            recognitionLabelRepository: RecognitionLabelRepository?,
            recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?,
            keywordRepository: KeywordRepository?,
            keywordPhotoRepository: KeywordPhotoRepository?,
            settings: Settings,
            threadFile: File? = null,
            shouldStop: AtomicBoolean? = null,
            messageSource: MessageSource? = null,
            locale: Locale = Locale("en")
        ): Pair<Int, List<String>> {
            val facesEnabled = settings.getFacialDetection() == true
            val objectsEnabled = settings.getObjectDetection() == true
            if (!facesEnabled && !objectsEnabled) return Pair(0, emptyList())
            if (!NetworkUtils.Companion.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())) return Pair(0, emptyList())

            val hasPeopleEnrolled = facesEnabled &&
                (recognitionLabelPhotoRepository?.existsByRecognitionLabelIdIsNotNull() == true)

            val limit = settings.getMatchScanLimit()!!
            val photos = metadataRepository?.findWithoutFacesOrKeywords(limit) ?: return Pair(0, emptyList())

            val nonMatchedIds: Set<String> = if (facesEnabled && hasPeopleEnrolled)
                metadataRepository.findNonMatchedIds(limit).toSet()
            else emptySet()

            val withoutKeywordIds: Set<String> = if (objectsEnabled)
                metadataRepository.findWithoutKeywordIds(limit).toSet()
            else emptySet()

            var recognitionCount = 0
            val processedIds = mutableListOf<String>()

            for (photo in photos) {
                if (shouldStop != null && shouldStop.get()) break
                val needsFaces = photo.getId() in nonMatchedIds
                val needsObjects = photo.getId() in withoutKeywordIds
                if (!needsFaces && !needsObjects) continue
                recognitionCount += detectAndStoreAll(
                    photo, settings,
                    recognitionLabelRepository, recognitionLabelPhotoRepository,
                    keywordRepository, keywordPhotoRepository, metadataRepository,
                    doFaces = needsFaces, doObjects = needsObjects,
                    threadFile = threadFile, messageSource = messageSource, locale = locale
                )
                processedIds.add(photo.getId())
            }
            return Pair(recognitionCount, processedIds)
        }

    }
}