package com.miyagi.shashin.service

import ai.djl.Application
import ai.djl.engine.Engine
import ai.djl.modality.Classifications
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelZoo
import ai.djl.training.util.ProgressBar
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
import java.awt.image.Kernel
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin


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
                        .size(metadataObj.getOriginalImageWidth()!!, metadataObj.getOriginalImageHeight()!!)
                        .outputQuality(0.4)
                        .toFile(tnFile)
                }
                metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_original." + extension)
            }

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
                        metadataObj.setThumbnailUrlSmall("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_" + FileUtils.Companion.thumbnailHeight() + "." + extension)
                        logger.log(Level.INFO, "Small thumbnail created: " + file.path)
                    } catch (e: IOException) {
                        logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                    }
                } else {
                    logger.log(Level.WARNING, "File DNE: " + tnFile.path)
                }
            }

            // Gallery x-small thumbnail
//            val xsHeight = 112
//            thumbnailFileStr =
//                thumbnailDirectory + fileRootDir + "/" + file.name + "_" + xsHeight + "." + extension
//            tnFile = FileUtils.createFile(
//                thumbnailFileStr,
//                overwriteThumbnails
//            )
//
//            if (tnFile != null) {
//                val tempFile = File(System.getProperty("java.io.tmpdir") + "/temp.jpg")
//
//                val thumbnails = Thumbnails.of(img)
//                    .outputQuality(1.0)
//                if (file.extension.lowercase() == "gif") {
//                    thumbnails
//                        .imageType(BufferedImage.TYPE_INT_ARGB)
//                }
//                // If panorama dimensions
//                if (img.width > img.height * 2) {
//                    thumbnails
//                        .crop(Positions.CENTER)
//                        .size(xsHeight, xsHeight)
//                } else {
//                    thumbnails
//                        .height(xsHeight)
//                }
//                thumbnails.toFile(tempFile)
//
//                val scaledImage: BufferedImage?
//                try {
//                    scaledImage = blurImage(ImageIO.read(tempFile))
//                    tempFile.delete()
//                    ImageIO.write(scaledImage, "jpg", tnFile)
//                    metadataObj.setThumbnailPathExtraSmall(thumbnailFileStr)
//                    metadataObj.setThumbnailUrlExtraSmall("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_" + xsHeight + "." + extension)
//                    logger.log(Level.INFO, "X-Small thumbnail created: " + file.path)
//                } catch (e: IOException) {
//                    logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
//                }
//            }

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
                        metadataObj.setThumbnailUrlCentered("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_centered." + extension)
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
                        metadataObj.setMapMarkerUrl("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_mapmarker." + extension)
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
            val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)

            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val color = Color(image.getRGB(x, y), true) // 'true' preserves alpha
                    val r = (color.red * brightness).toInt().coerceIn(0, 255)
                    val g = (color.green * brightness).toInt().coerceIn(0, 255)
                    val b = (color.blue * brightness).toInt().coerceIn(0, 255)
                    val a = color.alpha // preserve original alpha

                    result.setRGB(x, y, Color(r, g, b, a).rgb)
                }
            }

            return result
        }

        fun adjustContrast(image: BufferedImage, contrast: Double): BufferedImage {
            val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)

            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val color = Color(image.getRGB(x, y), true) // preserve alpha
                    val r = ((color.red - 128) * contrast + 128).toInt().coerceIn(0, 255)
                    val g = ((color.green - 128) * contrast + 128).toInt().coerceIn(0, 255)
                    val b = ((color.blue - 128) * contrast + 128).toInt().coerceIn(0, 255)
                    val a = color.alpha

                    result.setRGB(x, y, Color(r, g, b, a).rgb)
                }
            }

            return result
        }

        fun adjustBrightnessContrast(
            image: BufferedImage,
            brightness: Double,
            contrast: Double,
            gamma: Double = 2.2
        ): BufferedImage {
            val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)

            fun gammaDecode(v: Int): Double = Math.pow(v / 255.0, gamma)
            fun gammaEncode(v: Double): Int = (Math.pow(v, 1.0 / gamma) * 255.0).toInt().coerceIn(0, 255)

            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val color = Color(image.getRGB(x, y), true)

                    // Decode gamma
                    val rLin = gammaDecode(color.red)
                    val gLin = gammaDecode(color.green)
                    val bLin = gammaDecode(color.blue)

                    // Apply contrast centered around 0.5 (linear space)
                    val rContrast = ((rLin - 0.5) * contrast + 0.5)
                    val gContrast = ((gLin - 0.5) * contrast + 0.5)
                    val bContrast = ((bLin - 0.5) * contrast + 0.5)

                    // Apply brightness
                    val rBright = rContrast * brightness
                    val gBright = gContrast * brightness
                    val bBright = bContrast * brightness

                    // Encode gamma
                    val r = gammaEncode(rBright)
                    val g = gammaEncode(gBright)
                    val b = gammaEncode(bBright)

                    result.setRGB(x, y, Color(r, g, b, color.alpha).rgb)
                }
            }

            return result
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

        fun objectRecognizer(metadataObj: Metadata, criteria: Criteria<Image, DetectedObjects>, threshold: Double? = null, threadFile: File? = null, shouldStop: Boolean? = null, messageSource: MessageSource? = null, locale: Locale = Locale("en")): MutableMap<String, Double> {
            val keywordMap = mutableMapOf<String, Double>()
            val unidentifiedStr = "unidentified objects"

            try {
                val file = if (metadataObj.getType() != null && metadataObj.getType()?.contains("video", ignoreCase = true)!! && File(metadataObj.getThumbnailPathSmall()!!).exists()) {
                    File(metadataObj.getThumbnailPathSmall()!!)
                } else {
                    File(metadataObj.getPath()!!)
                }

                // Object recognition
                val img = ImageFactory.getInstance().fromFile(file.toPath())

                ModelZoo.loadModel(criteria).use { objmodel ->
                    objmodel.newPredictor().use { predictor ->
                        try {
                            val detection = predictor.predict(img)
                            val numOfObjects = detection.numberOfObjects
                            if (numOfObjects > 0) {
                                for (i in 0 until numOfObjects) {
                                    if (shouldStop != null && shouldStop) {
                                        break
                                    }

                                    val objProbability =
                                        detection.item<Classifications.Classification?>(i).probability
                                    val objSubject =
                                        detection.item<Classifications.Classification?>(i).className

                                    if (threshold != null) {
                                        if (objSubject.trim() != "person" && objProbability >= threshold
                                        ) {
                                            keywordMap[objSubject] = objProbability

                                            if (threadFile != null) {
                                                FileUtils.Companion.writeToThreadFileAndLogMessage(
                                                    if (messageSource == null) "Objects identified for " + metadataObj.getPath() else messageSource.getMessage("main.pages.matching.identified", arrayOf(metadataObj.getPath()), locale).toString() + ": S-" + objSubject + " P-" + objProbability,
                                                    threadFile
                                                )
                                            }

                                            logger.log(
                                                Level.INFO,
                                                "Objects identified for " + metadataObj.getPath() + ": S-" + objSubject + " P-" + objProbability
                                            )
                                        } else {
                                            if (threadFile != null) {
                                                FileUtils.Companion.writeToThreadFileAndLogMessage(
                                                    if (messageSource == null) "Objects identified for " + metadataObj.getPath() else messageSource.getMessage("main.pages.matching.identified", arrayOf(metadataObj.getPath()), locale).toString() + ": S-" + objSubject + " P-" + objProbability,
                                                    threadFile
                                                )
                                            }

                                            logger.log(
                                                Level.INFO,
                                                "Objects identified for " + metadataObj.getPath() + ": S-" + objSubject + " P-" + objProbability
                                            )
                                        }
                                    } else {
                                        keywordMap[objSubject] = objProbability
                                    }
                                }

                                if (keywordMap.isEmpty() && !keywordMap.keys.toTypedArray().contains(unidentifiedStr)) {
                                    keywordMap[unidentifiedStr] = -1.0
                                }
                            } else {
                                if (!keywordMap.keys.toTypedArray().contains(unidentifiedStr)) {
                                    keywordMap[unidentifiedStr] = -1.0
                                }
                            }
                        } catch (_: Exception) {
                            if (keywordMap.isEmpty() && !keywordMap.keys.toTypedArray().contains(unidentifiedStr)) {
                                keywordMap[unidentifiedStr] = -1.0
                            }

                            logger.log(
                                Level.INFO,
                                "Could not identify objects for " + metadataObj.getPath()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.log(
                    Level.WARNING,
                    "Object recognition could not process file for " + metadataObj.getPath()!! + " error " + e.message
                )
            }

            return keywordMap
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

        fun buildPersonUpload(settings: Settings, personName: String?, metadata: Metadata?, compreFaceImageIdMap: MutableMap<String, Any?>): MutableMap<String, Any?> {
            val mapper = ObjectMapper()
            val uploadResponse = mutableMapOf<String, Any?>()
            uploadResponse["responseData"] = mutableMapOf<String, Any?>()
            uploadResponse["similarity"] = 0.0

            uploadResponse["msg"] = ""
            uploadResponse["status"] = ApiResponse.FAIL.status

            if (settings.getFacialDetection() == true && NetworkUtils.Companion.checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())) {
                val response: String?

                if (!personName.isNullOrBlank() && !metadata?.getId().isNullOrBlank()) {

                    val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                    val recognizedObj = mapper.writeValueAsString(buildPersonRecognition(settings, metadata))

                    val jsonRespObj = mapper.readTree(recognizedObj)
                    val subjectObj: JsonNode
                    var subject = ""
                    var similarity = 0.0
                    if (jsonRespObj.has("recognizeData") && jsonRespObj["recognizeData"].has(0)) {
                        subjectObj = jsonRespObj["recognizeData"].get(0)
                        if (subjectObj.has("subject")) {
                            subject = subjectObj["subject"].textValue()
                            similarity = subjectObj["similarity"].asDouble()
                        }
                    }
                    uploadResponse["similarity"] = similarity

                    // This means the person has been relabeled
                    if (subject != "" && personName != subject) {
                        similarity = 0.0

                        val compreFaceImageId =
                            compreFaceImageIdMap[subject.filterNot { it.isWhitespace() } + "-" + metadata?.getId()]

                        try {
                            if (compreFaceImageId != null && compreFaceImageId.toString().isNotEmpty()) {
                                webClient.delete()
                                    .uri("api/v1/recognition/faces/$compreFaceImageId")
                                    .header("x-api-key", settings.getCompreFaceKey())
                                    .retrieve()
                                    .bodyToMono(String::class.java)
                                    .block()
                            }
                        } catch (e: Exception) {
                            logger.log(
                                Level.WARNING,
                                "Error deleting CompreFace ID $compreFaceImageId for ${metadata?.getId()}: " + e.localizedMessage
                            )
//                            val errorResponse =
//                                e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                        }
                    }

                    // Uploaded faces
                    if (similarity != 1.0 && (similarity <= 0.0 || similarity >= settings.getRecognitionConfidenceThreshold().toString().toDouble())) {
                        try {
                            val builder = MultipartBodyBuilder()
                            builder.part("file", FileSystemResource(metadata?.getThumbnailPathSmall()!!))

                            response = webClient.post()
                                .uri("api/v1/recognition/faces?subject=${personName}")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                                .header("x-api-key", settings.getCompreFaceKey())
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .retrieve()
                                .bodyToMono(String::class.java)
                                .block()

                            val jsonObj = mapper.readTree(response)

                            logger.log(
                                Level.INFO,
                                "Face $personName for ${metadata.getId()} uploaded: " + response
                            )

                            uploadResponse["responseData"] = jsonObj

                            uploadResponse["msg"] = ""
                            uploadResponse["status"] = ApiResponse.SUCCESS.status
                        } catch (e: Exception) {
                            logger.log(
                                Level.WARNING,
                                "Error uploading face $personName for ${metadata?.getId()}: " + e.localizedMessage
                            )
                            val errorResponse =
                                e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                            uploadResponse["responseData"] = errorResponse
                        }
                    } else {
                        logger.log(
                            Level.INFO,
                            "Not processed - Similarity: $similarity, Threshold: ${settings.getRecognitionConfidenceThreshold().toString().toDouble()}"
                        )
                        uploadResponse["msg"] = "Not processed - Similarity: $similarity, Threshold: ${settings.getRecognitionConfidenceThreshold().toString().toDouble()}"
                        uploadResponse["status"] = ApiResponse.FAIL.status
                    }
                } else {
                    logger.log(
                        Level.WARNING,
                        "Person name or metadata ID blank"
                    )
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

            if (settings.getFacialDetection() == true && NetworkUtils.Companion.checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())) {

                val response: String?

                if (metadata !== null) {
                    // Recognizing faces

                    try {
                        val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                        val builder = MultipartBodyBuilder()
                        builder.part("file", FileSystemResource(metadata.getThumbnailPathSmall()!!))

                        response = webClient.post()
                            .uri("api/v1/recognition/recognize")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                            .header("x-api-key", settings.getCompreFaceKey())
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(String::class.java)
                            .block()

                        val jsonObj = mapper.readTree(response)
                        val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, ArrayList<Map<String, Any>>>>() {})
                        val resultList = resultMap["result"] as ArrayList<Map<String, Any>>
                        var subjects: ArrayList<Map<String, Any>>? = null
                        if (resultList.isNotEmpty() && resultList[0].containsKey("subjects")) {
                            subjects = resultList[0]["subjects"] as ArrayList<Map<String, Any>>
                        }
                        recognitionResponse["recognizeData"] = subjects
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

        fun subjectRecognizer(metadataRepository: MetadataRepository?, recognitionLabelRepository: RecognitionLabelRepository?, recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?, relativeSidecarDir: String, settings: Settings, threadFile: File?, shouldStop: AtomicBoolean?, messageSource: MessageSource?, locale: Locale = Locale("en")): Int {
            // Scan records of photos that haven't been scanned in a separate thread
            val testImages = metadataRepository?.findNonMatched(settings.getMatchScanLimit()!!)
            val distinctLabelRecords = recognitionLabelPhotoRepository?.findGroupByRecognitionLabelId()
            var recognitionCount = 0

            if (testImages != null && distinctLabelRecords != null && distinctLabelRecords.count() > 0) {
                if (settings.getFacialDetection() == true &&
                    NetworkUtils.Companion.checkCompreFaceConnection(
                        settings.getCompreFaceServer(),
                        settings.getCompreFaceKey()
                    )
                ) {
                    val mapper = ObjectMapper()
                    val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                    for (testImage in testImages) {

                        if (shouldStop != null && shouldStop.get()) {
                            break
                        }

                        val metadataObj = metadataRepository.findById(testImage.getId()).get()

                        // Facial recognition
                        val faceFsr = FileSystemResource(metadataObj.getThumbnailPathSmall()!!)
                        var builder = MultipartBodyBuilder()
                        builder.part(
                            "file",
                            faceFsr
                        )

                        var response: String? = null

                        try {
                            response = webClient.post()
                                .uri("api/v1/recognition/recognize")
                                .header(
                                    HttpHeaders.CONTENT_TYPE,
                                    MediaType.MULTIPART_FORM_DATA.toString()
                                )
                                .header("x-api-key", settings.getCompreFaceKey())
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .retrieve()
                                .bodyToMono(String::class.java)
                                .block()

                            logger.log(
                                Level.INFO,
                                "Recognizing face for " + metadataObj.getId() + " - " + metadataObj.getPath() + ": " + response
                            )
                        } catch (e: Exception) {
                            val recognitionLabelRecord =
                                recognitionLabelRepository?.findByNameIgnoreCase(TextUtils.Companion.getObjectName())
                            var recognitionLabelObj = RecognitionLabel()
                            if (recognitionLabelRecord == null) {
                                recognitionLabelObj.setName(TextUtils.Companion.getObjectName())
                                recognitionLabelObj.setCreatedAt(TextUtils.Companion.getCurrentTimestamp())
                                recognitionLabelObj.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
                                recognitionLabelRepository?.save(recognitionLabelObj)
                            } else {
                                recognitionLabelObj = recognitionLabelRecord
                            }

                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataObj.getId())
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setConfidence("-0.1")
                            recognitionLabelPhotoRepository.save(recognitionLabelPhotoObj)

                            logger.log(
                                Level.WARNING,
                                "Error recognizing face for " + metadataObj.getId() + " - " + metadataObj.getPath() + ": " + e.localizedMessage
                            )
                        }

                        if (response != null) {

                            var jsonObj = mapper.readTree(response)
                            val resultMap = mapper.convertValue(
                                jsonObj,
                                object :
                                    TypeReference<Map<String, ArrayList<Map<String, Any>>>>() {})

                            var resultList: ArrayList<Map<String, Any>>? = null

                            if (resultMap.containsKey("result")) {
                                resultList =
                                    resultMap["result"] as ArrayList<Map<String, Any>>
                            }

                            if (resultList != null) {
                                if (resultList.isNotEmpty() && resultList[0].containsKey("subjects")) {
                                    for (singleResult in resultList) {
                                        val subjects =
                                            singleResult["subjects"] as ArrayList<Map<String, Any>>

                                        for (subjectObj in subjects) {
                                            var subject = ""
                                            var similarity = 0.0

                                            if (subjectObj.isNotEmpty()) {
                                                subject = subjectObj["subject"].toString()
                                                similarity =
                                                    subjectObj["similarity"].toString().toDouble()
                                            }

                                            if (threadFile != null) {
                                                FileUtils.Companion.writeToThreadFileAndLogMessage(
                                                    messageSource?.getMessage("main.pages.matching.analyzing", arrayOf(subject,metadataObj.getPath()), locale).toString(),
                                                    threadFile
                                                )
                                            }

                                            if (similarity != 1.0 && (similarity <= 0.0 || similarity >= settings.getRecognitionConfidenceThreshold()
                                                    .toString().toDouble())
                                            ) {

                                                response = null

                                                try {
                                                    builder = MultipartBodyBuilder()
                                                    builder.part(
                                                        "file",
                                                        faceFsr
                                                    )

                                                    response = webClient.post()
                                                        .uri("api/v1/recognition/faces?subject=${subject}")
                                                        .header(
                                                            HttpHeaders.CONTENT_TYPE,
                                                            MediaType.MULTIPART_FORM_DATA.toString()
                                                        )
                                                        .header(
                                                            "x-api-key",
                                                            settings.getCompreFaceKey()
                                                        )
                                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                                        .retrieve()
                                                        .bodyToMono(String::class.java)
                                                        .block()
                                                } catch (e: Exception) {
                                                    logger.log(
                                                        Level.WARNING,
                                                        "Error uploading face for " + subject + " for " + metadataObj.getId() + " - " + " image " + metadataObj.getPath() + ": " + e.localizedMessage
                                                    )
                                                }

                                                var compreFaceImageId: String? = null

                                                if (response != null) {
                                                    jsonObj = mapper.readTree(response)

                                                    if (jsonObj.has("image_id")) {
                                                        compreFaceImageId =
                                                            jsonObj["image_id"].toString()
                                                        compreFaceImageId =
                                                            compreFaceImageId.drop(1).dropLast(1)
                                                    }
                                                }

                                                logger.log(
                                                    Level.INFO,
                                                    "Uploaded subject for " + metadataObj.getId() + " - " + metadataObj.getPath() + " for subject " + subject + ": " + response
                                                )

                                                val recognitionLabelObj =
                                                    recognitionLabelRepository?.findByNameIgnoreCase(
                                                        subject
                                                    )

                                                if (recognitionLabelObj != null) {
                                                    val recognitionLabelPhoto =
                                                        recognitionLabelPhotoRepository.countByRecognitionLabelIdAndMetadataId(
                                                            recognitionLabelObj.getId(),
                                                            metadataObj.getId()
                                                        )

                                                    if (recognitionLabelPhoto == 0) {
                                                        val recognitionLabelPhotoObj =
                                                            RecognitionLabelPhoto()
                                                        recognitionLabelPhotoObj.setMetadataId(
                                                            metadataObj.getId()
                                                        )
                                                        recognitionLabelPhotoObj.setRecognitionLabelId(
                                                            recognitionLabelObj.getId()
                                                        )
                                                        recognitionLabelPhotoObj.setConfidence(
                                                            similarity.toString()
                                                        )
                                                        if (compreFaceImageId != null) {
                                                            recognitionLabelPhotoObj.setCompreFaceImageId(
                                                                compreFaceImageId
                                                            )
                                                        }
                                                        recognitionLabelPhotoRepository.save(
                                                            recognitionLabelPhotoObj
                                                        )

                                                        metadataObj.setModifiedAt(TextUtils.Companion.getCurrentTimestamp())
                                                        metadataRepository.save(metadataObj)

                                                        if (threadFile != null) {
                                                            FileUtils.Companion.writeToThreadFileAndLogMessage(
                                                                messageSource?.getMessage("main.pages.matching.processing", arrayOf(subject,metadataObj.getPath(),similarity.toString()), locale).toString(),
                                                                threadFile
                                                            )
                                                        }

                                                        recognitionCount++
                                                    }
                                                } else {
                                                    logger.log(
                                                        Level.INFO,
                                                        "Did not process subject " + subject + " for " + metadataObj.getId() + " - " + metadataObj.getPath() + " with similarity " + similarity.toString()
                                                    )
                                                }
                                            } else {
                                                logger.log(
                                                    Level.INFO,
                                                    "Did not upload subject " + subject + " for " + metadataObj.getId() + " - " + metadataObj.getPath() + " with similarity " + similarity.toString()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val classLoader: ClassLoader = ShashinApplication::class.java.classLoader
                    val vggfaceFileExists = classLoader.getResource("lib/vggface2.pt") != null
                    val retinafaceFileExists = classLoader.getResource("lib/retinaface.pt") != null
                    if (vggfaceFileExists && retinafaceFileExists) {
                        val trainingData = metadataRepository.findTrainingData(
                            settings.getRecognitionConfidenceThreshold()!!,
                            settings.getTrainingDataLimit()!!
                        )

                        var stop = AtomicBoolean(false)
                        if (shouldStop != null) {
                            stop = shouldStop
                        }
                        val faceRecognizer = DjlFaceRecognizer(
                            testImages,
                            trainingData,
                            recognitionLabelPhotoRepository,
                            recognitionLabelRepository,
                            settings,
                            relativeSidecarDir,
                            threadFile!!,
                            stop
                        )
                        recognitionCount = faceRecognizer.startPredict(messageSource,locale)
                    } else {
                        logger.log(
                            Level.WARNING,
                            "Missing lib files for DJL face scan"
                        )
                        if (threadFile != null) {
                            FileUtils.Companion.writeToThreadFileAndLogMessage(
                                messageSource?.getMessage("main.notification.people.missing", null, locale).toString(),
                                threadFile
                            )
                        }
                    }
                }
            }

            return recognitionCount
        }

        fun buildObjectRecognitionCriteria(): Criteria<Image, DetectedObjects>? {
            var criteria: Criteria<Image, DetectedObjects>? = null
            try {
                criteria = Criteria.builder()
                    .optApplication(Application.CV.OBJECT_DETECTION)
                    .setTypes(Image::class.java, DetectedObjects::class.java)
                    .optEngine(Engine.getDefaultEngineName())
                    .optFilter("backbone", "resnet50")
                    .optProgress(ProgressBar())
                    .build()
            } catch (e: Exception) {
                logger.log(
                    Level.WARNING,
                    "Could not initialize criteria for object recognizer: ${e.message}"
                )
            }

            return criteria
        }

        fun isDuplicate(filename1: String?, filename2: String?): Boolean {
            var isDuplicate = false
            if (filename1 != null && filename2 != null) {
                val i = DuplicateImageChecker()
                i.setFirstImage(filename1)
                i.setSecondImage(filename2)
                isDuplicate = i.isDuplicate()
            }
            return isDuplicate
        }
    }
}