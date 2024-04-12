package com.miyagi.shashin.util

import ai.djl.modality.Classifications
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelZoo
import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.javascript.jscomp.jarjar.com.google.common.io.Files
import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.component.DjlFaceRecognizer
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.twelvemonkeys.image.ConvolveWithEdgeOp
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.bytedeco.javacv.FFmpegFrameGrabber
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
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.reflect.jvm.internal.impl.types.checker.ClassicTypeSystemContext.DefaultImpls.original


@Suppress("UNCHECKED_CAST")
class ImageProcessing(private var apiVersion: String?, private var file: File, private var sidecarDir: String, private var metadataObj: Metadata?) {

    private var logger: Logger = Logger.getLogger(ImageProcessing::class.simpleName)

    fun createThumbnails(): Metadata? {
        var _metadataObj = metadataObj

        // Check rotation
        var rotation = 0
        var fileMetadata: com.drew.metadata.Metadata
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
                                if (TextUtils.isInteger(digit)) {
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
        val fileRootDir: String = FileUtils.getRootDir(file)
        val supportedImageFormats = FileUtils.allowableImageFiles()
        val supportedVideoFormats = FileUtils.allowableVideoFiles()
        var extension = "jpg"

        var img: BufferedImage? = null
        if (FileUtils.isRaw(file.extension.lowercase())) {
            try {
                img = ImageIO.read(file)
                if (rotation > 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedImageFormats.contains(file.extension.lowercase())) {
            extension = file.extension.lowercase()
            try {
                img = ImageIO.read(file)
                if (rotation > 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedVideoFormats.contains(file.extension.lowercase())) {
            // Grab screenshot
            val videoProcessing = VideoProcessing(file)
            img = videoProcessing.getVideoScreenshot()
//            _metadataObj?.setVideoUrl("/api/$apiVersion/original/video$fileRootDir/" + file.name)
        }

        if (supportedVideoFormats.contains(file.extension.lowercase()) && !rotationFromExif && rotation == 0) {
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
                (FileUtils.isRaw(file.extension.lowercase()) || supportedVideoFormats.contains(file.extension.lowercase())),
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
            val thumbnailDirectory = sidecarDir.dropLast(1) + "/thumbnails"
            val fileRootDir: String = FileUtils.getRootDir(file)

            // Raw file to image conversion
            var thumbnailFileStr: String
            var tnFile: File?
            if (isRawOrVideo) {
                thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_original." + extension
                tnFile = FileUtils.createFile(
                    thumbnailDirectory + fileRootDir,
                    thumbnailFileStr,
                    "Thumbnail",
                    overwriteThumbnails
                )
                if (tnFile != null) {
                    val imgCopy = img
                    Thumbnails.of(imgCopy)
                        .size(metadataObj.getOriginalImageWidth()!!, metadataObj.getOriginalImageHeight()!!)
                        .outputQuality(0.4)
                        .toFile(tnFile)
                }
                metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_original." + extension)
            }

            // Gallery small thumbnails
            thumbnailFileStr =
                thumbnailDirectory + fileRootDir + "/" + file.name + "_" + FileUtils.thumbnailHeight() + "." + extension
            tnFile = FileUtils.createFile(
                thumbnailDirectory + fileRootDir,
                thumbnailFileStr,
                "Thumbnail",
                overwriteThumbnails
            )

            if (tnFile != null) {
                val tempFile = File(System.getProperty("java.io.tmpdir") + "/temp.jpg")

                val thumbnails = Thumbnails.of(img)
                    .outputQuality(1.0)
                if (file.extension.lowercase() == "gif") {
                    thumbnails
                        .imageType(BufferedImage.TYPE_INT_ARGB)
                }
                // If panorama dimensions
                if (img.width > img.height * 2) {
                    thumbnails
                        .crop(Positions.CENTER)
                        .size(FileUtils.thumbnailHeight(), FileUtils.thumbnailHeight())
                } else {
                    thumbnails
                        .height(FileUtils.thumbnailHeight())
                }
                thumbnails.toFile(tempFile)

                val scaledImage: BufferedImage?
                try {
                    scaledImage = sharpenAndBrightenImage(ImageIO.read(tempFile))
                    tempFile.delete()
                    ImageIO.write(scaledImage, "jpg", tnFile)
                    metadataObj.setThumbnailSmallHeight(scaledImage.height)
                    metadataObj.setThumbnailSmallWidth(scaledImage.width)
                    metadataObj.setThumbnailPathSmall(thumbnailFileStr)
                    metadataObj.setThumbnailUrlSmall("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_" + FileUtils.thumbnailHeight() + "." + extension)
                    logger.log(Level.INFO, "Small thumbnail created: " + file.path)
                } catch (e: IOException) {
                    logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                }
            }

            // Gallery x-small thumbnail
            val xsHeight = 112
            thumbnailFileStr =
                thumbnailDirectory + fileRootDir + "/" + file.name + "_" + xsHeight + "." + extension
            tnFile = FileUtils.createFile(
                thumbnailDirectory + fileRootDir,
                thumbnailFileStr,
                "XS Thumbnail",
                overwriteThumbnails
            )

            if (tnFile != null) {
                val tempFile = File(System.getProperty("java.io.tmpdir") + "/temp.jpg")

                val thumbnails = Thumbnails.of(img)
                    .outputQuality(1.0)
                if (file.extension.lowercase() == "gif") {
                    thumbnails
                        .imageType(BufferedImage.TYPE_INT_ARGB)
                }
                // If panorama dimensions
                if (img.width > img.height * 2) {
                    thumbnails
                        .crop(Positions.CENTER)
                        .size(xsHeight, xsHeight)
                } else {
                    thumbnails
                        .height(xsHeight)
                }
                thumbnails.toFile(tempFile)

                val scaledImage: BufferedImage?
                try {
//                    scaledImage = sharpenAndBrightenImage(ImageIO.read(tempFile))
                    scaledImage = blurImage(ImageIO.read(tempFile))
                    tempFile.delete()
                    ImageIO.write(scaledImage, "jpg", tnFile)
                    metadataObj.setThumbnailPathExtraSmall(thumbnailFileStr)
                    metadataObj.setThumbnailUrlExtraSmall("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_" + xsHeight + "." + extension)
                    logger.log(Level.INFO, "X-Small thumbnail created: " + file.path)
                } catch (e: IOException) {
                    logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                }
            }

            // Square image thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_centered." + extension
            tnFile = FileUtils.createFile(
                thumbnailDirectory + fileRootDir,
                thumbnailFileStr,
                "Thumbnail",
                overwriteThumbnails
            )
            if (tnFile != null) {
                val tempFile = File(System.getProperty("java.io.tmpdir") + "/temp.jpg")
                Thumbnails.of(img)
                    .crop(Positions.CENTER)
                    .size(209, 209)
                    .outputQuality(1.0)
                    .toFile(tempFile)

                val scaledImage: BufferedImage?
                try {
                    scaledImage = sharpenAndBrightenImage(ImageIO.read(tempFile))
                    tempFile.delete()
                    ImageIO.write(scaledImage, "jpg", tnFile)
                    metadataObj.setThumbnailPathCentered(thumbnailFileStr)
                    metadataObj.setThumbnailUrlCentered("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_centered." + extension)
                    logger.log(Level.INFO, "Centered thumbnail created: " + file.path)
                } catch (e: IOException) {
                    logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                }
            }

            // Map marker thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_mapmarker." + extension
            tnFile = FileUtils.createFile(
                thumbnailDirectory + fileRootDir,
                thumbnailFileStr,
                "Thumbnail",
                overwriteThumbnails
            )
            if (tnFile != null) {
                val tempFile = File(System.getProperty("java.io.tmpdir") + "/temp.jpg")
                Thumbnails.of(img)
                    .crop(Positions.CENTER)
                    .size(45, 45)
                    .outputQuality(1.0)
                    .toFile(tempFile)

                var scaledImage: BufferedImage?
                try {
                    scaledImage = ImageIO.read(tempFile)
                    scaledImage = sharpenAndBrightenImage(scaledImage)
                    scaledImage = borderImage(scaledImage)
                    tempFile.delete()
                    ImageIO.write(scaledImage, "jpg", tnFile)
                    metadataObj.setMapMarkerPath(thumbnailFileStr)
                    metadataObj.setMapMarkerUrl("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_mapmarker." + extension)
                    logger.log(Level.INFO, "Map thumbnail created: " + file.path)
                } catch (e: IOException) {
                    logger.log(Level.WARNING, "Could not read file: " + tnFile.path)
                }
            }
        } else {
            logger.log(Level.WARNING, "File " + file.path + " does not exist.")
        }

        return metadataObj
    }

    private fun getVideoRotation(file: File): Double? {
        try {
            val frameGrabber = FFmpegFrameGrabber(file.path)
            frameGrabber.start()

            val rotationStr = frameGrabber.getVideoMetadata("rotate")
            frameGrabber.stop()

            if (!rotationStr.isNullOrBlank()) {
                return rotationStr.toDouble()
            }
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Could not get rotation for video " + file.name + ": " + e.message)
            return null
        }

        return null
    }

    private fun scaleImageByRatio(source: BufferedImage, ratio: Double): BufferedImage {
        val w = (source.width * ratio).toInt()
        val h = (source.height * ratio).toInt()
        return scaleImage(source, w, h)
    }

    private fun scaleImageByWidth(source: BufferedImage, w: Int): BufferedImage {
        val wScale: Float = (w.toFloat() / source.width.toFloat())
        val h = source.height.toFloat() * wScale //3705*(50/6000)
        return scaleImage(source, w, h.toInt())
    }

    private fun scaleImageByHeight(source: BufferedImage, h: Int): BufferedImage {
        val hScale: Float = (h.toFloat() / source.height.toFloat())
        val w = source.width.toFloat() * hScale
        return scaleImage(source, w.toInt(), h)
    }

    private fun scaleImage(source: BufferedImage, w: Int, h: Int): BufferedImage {
        val bi = getCompatibleImage(w, h)
        val g2d = bi.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val xScale = w.toDouble() / source.width
        val yScale = h.toDouble() / source.height
        val at = AffineTransform.getScaleInstance(xScale, yScale)
        g2d.drawRenderedImage(source, at)
        g2d.dispose()
        return bi
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

    private fun getSquareThumbnail(source: BufferedImage): BufferedImage {
        // Get a square thumbnail
        val side = source.width.coerceAtMost(source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return source.getSubimage(x, y, side, side)
    }

    private fun getCompatibleImage(w: Int, h: Int): BufferedImage {
        return BufferedImage(
            w, h,
            BufferedImage.TYPE_INT_RGB
        )
    }

    companion object {
        private var logger: Logger = Logger.getLogger(FileUtils::class.simpleName)

        fun createVideoGif(metadataId: String, metadataRepository: MetadataRepository?) {
            val metadataObj = metadataRepository?.findByMetadataId(metadataId)

            if (metadataObj != null && metadataObj.getType()!!.contains("video", ignoreCase = true)) {
                val gifFilePath = metadataObj.getThumbnailPathSmall()!!.replace("_225.jpg", "_225.gif")
                val gifFile = File(gifFilePath)

                if (!gifFile.exists()) {
                    val videoProcessing = VideoProcessing(File(metadataObj.getPath()))
                    val processedGifFile = videoProcessing.getVideoGifFile()

                    if (processedGifFile != null) {
                        Files.copy(processedGifFile, gifFile)
                    }
                }
            }
        }

        fun rotateImage(buffImage: BufferedImage, angle: Double): BufferedImage {
            val radian = Math.toRadians(angle)
            val sin = Math.abs(Math.sin(radian))
            val cos = Math.abs(Math.cos(radian))
            val width = buffImage.width
            val height = buffImage.height
            val nWidth = Math.floor(width.toDouble() * cos + height.toDouble() * sin).toInt()
            val nHeight = Math.floor(height.toDouble() * cos + width.toDouble() * sin).toInt()
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
                keywordObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                keywordObj.setModifiedAt(TextUtils.getCurrentTimestamp())
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
                keywordPhotoObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                keywordPhotoObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                keywordPhotoRepository.save(keywordPhotoObj)
                metadataObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                metadataRepository.save(metadataObj)
            }
        }

        fun objectRecognizer(keywordRepository: KeywordRepository, keywordPhotoRepository: KeywordPhotoRepository, metadataRepository: MetadataRepository, metadataObj: Metadata, criteria: Criteria<Image, DetectedObjects>, settings: Settings, threadFile: File?, shouldStop: Boolean?): List<String> {
            val keywordArray = mutableListOf<String>()
            val unidentifiedStr = "unidentified objects"

            try {
                val file = if (metadataObj.getType()?.contains("video", ignoreCase = true)!!) {
                    File(metadataObj.getThumbnailPathSmall())
                } else {
                    File(metadataObj.getPath())
                }

                // Object recognition
                val img = ImageFactory.getInstance().fromFile(file.toPath())

                ModelZoo.loadModel(criteria).use { objmodel ->
                    objmodel.newPredictor().use { predictor ->
                        try {
                            val detection = predictor.predict(img)
                            val numOfObjects = detection.numberOfObjects
                            if (numOfObjects > 0) {
                                for (i in 0..numOfObjects) {
                                    if (shouldStop != null && shouldStop) {
                                        break
                                    }

                                    val objProbability =
                                        detection.item<Classifications.Classification?>(i).probability
                                    val objSubject =
                                        detection.item<Classifications.Classification?>(i).className

                                    val threshold =
                                        settings.getObjectRecognitionConfidenceThreshold().toString().toDouble()

                                    if (objSubject.trim() != "person" && objProbability >= threshold
                                    ) {
                                        saveObject(
                                            objSubject,
                                            metadataObj,
                                            keywordRepository,
                                            keywordPhotoRepository,
                                            metadataRepository
                                        )
                                        keywordArray.add(objSubject)

                                        if (threadFile != null) {
                                            FileUtils.writeToThreadFileAndLogMessage(
                                                "Objects saved for " + metadataObj.getThumbnailUrlSmall() + ": S-" + objSubject + " P-" + objProbability,
                                                threadFile
                                            )
                                        }

                                        logger.log(
                                            Level.INFO,
                                            "Objects saved for " + metadataObj.getThumbnailUrlSmall() + ": S-" + objSubject + " P-" + objProbability
                                        )
                                    } else {
                                        if (threadFile != null) {
                                            FileUtils.writeToThreadFileAndLogMessage(
                                                "Objects identified for " + metadataObj.getThumbnailUrlSmall() + ": S-" + objSubject + " P-" + objProbability,
                                                threadFile
                                            )
                                        }

                                        logger.log(
                                            Level.INFO,
                                            "Objects not saved but identified for " + metadataObj.getThumbnailUrlSmall() + ": S-" + objSubject + " P-" + objProbability
                                        )
                                    }
                                }

                                if (keywordArray.size == 0 && !keywordArray.contains(unidentifiedStr)) {
                                    keywordArray.add(unidentifiedStr)
                                    saveObject(
                                        unidentifiedStr,
                                        metadataObj,
                                        keywordRepository,
                                        keywordPhotoRepository,
                                        metadataRepository
                                    )
                                }
                            } else {
                                if (keywordArray.size == 0 && !keywordArray.contains(unidentifiedStr)) {
                                    keywordArray.add(unidentifiedStr)
                                    saveObject(
                                        unidentifiedStr,
                                        metadataObj,
                                        keywordRepository,
                                        keywordPhotoRepository,
                                        metadataRepository
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            if (keywordArray.size == 0 && !keywordArray.contains(unidentifiedStr)) {
                                keywordArray.add(unidentifiedStr)
                                saveObject(
                                    unidentifiedStr,
                                    metadataObj,
                                    keywordRepository,
                                    keywordPhotoRepository,
                                    metadataRepository
                                )
                            }

                            logger.log(
                                Level.INFO,
                                "Could not identify objects for " + metadataObj.getThumbnailUrlSmall()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.log(
                    Level.INFO,
                    "Object recognition could not process file for " + metadataObj.getPath()!! + " error " + e.message
                )
            }

            return keywordArray.distinct()
        }

        fun buildPersonUpload(settings: Settings, personName: String?, metadata: Metadata?, compreFaceImageIdMap: MutableMap<String, Any?>): MutableMap<String, Any?> {
            val mapper = ObjectMapper()
            val uploadresponse = mutableMapOf<String, Any?>()
            uploadresponse["responseData"] = mutableMapOf<String, Any?>()
            uploadresponse["similarity"] = 0.0

            uploadresponse["msg"] = ""
            uploadresponse["status"] = ApiResponse.FAIL.status

            if (NetworkUtils.checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())) {
                var response: String?

                if (!personName.isNullOrBlank() && !metadata?.getId().isNullOrBlank()) {

                    val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                    val recognizedObj = mapper.writeValueAsString(buildPersonRecognition(settings, metadata))

                    val jsonRespObj = mapper.readTree(recognizedObj)
                    var subjectObj: JsonNode
                    var subject = ""
                    var similarity = 0.0
                    if (jsonRespObj.has("recognizeData") && jsonRespObj["recognizeData"].has(0)) {
                        subjectObj = jsonRespObj["recognizeData"].get(0)
                        if (subjectObj.has("subject")) {
                            subject = subjectObj["subject"].textValue();
                            similarity = subjectObj["similarity"].asDouble()
                        }
                    }
                    uploadresponse["similarity"] = similarity

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
                                "Error deleting CompreFace ID ${compreFaceImageId} for ${metadata?.getId()}: " + e.localizedMessage
                            )
//                            val errorResponse =
//                                e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                        }
                    }

                    // Uploaded faces
                    if (similarity != 1.0 && (similarity <= 0.0 || similarity >= settings.getRecognitionConfidenceThreshold().toString().toDouble())) {
                        try {
                            if (metadata != null) {
                                val builder = MultipartBodyBuilder()
                                builder.part("file", FileSystemResource(metadata.getThumbnailPathSmall()!!))

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

                                uploadresponse["responseData"] = jsonObj

                                uploadresponse["msg"] = ""
                                uploadresponse["status"] = ApiResponse.SUCCESS.status
                            } else {
                                response = "Metadata not found."
                                uploadresponse["responseData"] = response
                            }
                        } catch (e: Exception) {
                            logger.log(
                                Level.WARNING,
                                "Error uploading face $personName for ${metadata?.getId()}: " + e.localizedMessage
                            )
                            val errorResponse =
                                e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                            uploadresponse["responseData"] = errorResponse
                        }
                    } else {
                        logger.log(
                            Level.INFO,
                            "Not processed - Similarity: $similarity, Threshold: ${settings.getRecognitionConfidenceThreshold().toString().toDouble()}"
                        )
                        uploadresponse["msg"] = "Not processed - Similarity: $similarity, Threshold: ${settings.getRecognitionConfidenceThreshold().toString().toDouble()}"
                        uploadresponse["status"] = ApiResponse.FAIL.status
                    }
                } else {
                    logger.log(
                        Level.WARNING,
                        "Person name or metadata ID blank"
                    )
                    uploadresponse["msg"] = "Person name or metadata ID blank"
                    uploadresponse["status"] = ApiResponse.FAIL.status
                }
            }

            return uploadresponse
        }

        fun buildPersonRecognition(settings: Settings, metadata: Metadata?): MutableMap<String, Any?> {
            val mapper = ObjectMapper()
            val recogresponse = mutableMapOf<String, Any?>()

            recogresponse["recognizeData"] = mutableMapOf<String, Any?>()
            recogresponse["msg"] = ""
            recogresponse["status"] = ApiResponse.FAIL.status

            if (NetworkUtils.checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())) {

                var response: String?

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
                        recogresponse["recognizeData"] = subjects
                        recogresponse["msg"] = ""
                        recogresponse["status"] = ApiResponse.SUCCESS.status

                    } catch (e: Exception) {
                        val errorResponse =
                            e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                        recogresponse["recognizeData"] = errorResponse
                    }
                } else {
                    recogresponse["msg"] = "Metadata ID blank"
                    recogresponse["status"] = ApiResponse.FAIL.status
                }
            }

            return recogresponse
        }

        fun subjectRecognizer(metadataRepository: MetadataRepository?, recognitionLabelRepository: RecognitionLabelRepository?, recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?, relativeSidecarDir: String, settings: Settings, threadFile: File?, shouldStop: AtomicBoolean?): Int {
            // Scan records of photos that haven't been scanned in a separate thread
            val testImages = metadataRepository?.findNonMatched(settings.getMatchScanLimit()!!)
            val distinctLabelRecords = recognitionLabelPhotoRepository?.findGroupByRecognitionLabelId()
            var recognitionCount = 0

            if (testImages != null && distinctLabelRecords != null && distinctLabelRecords.count() > 0) {
                if (NetworkUtils.checkCompreFaceConnection(
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
                                recognitionLabelRepository?.findByNameIgnoreCase("shashinobject")
                            var recognitionLabelObj = RecognitionLabel()
                            if (recognitionLabelRecord == null) {
                                recognitionLabelObj.setName("shashinobject")
                                recognitionLabelObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                                recognitionLabelObj.setModifiedAt(TextUtils.getCurrentTimestamp())
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
                                                FileUtils.writeToThreadFileAndLogMessage(
                                                    "Analyzing subject " + subject + " for " + metadataObj.getPath(),
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

                                                        metadataObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                                                        metadataRepository.save(metadataObj)

                                                        if (threadFile != null) {
                                                            FileUtils.writeToThreadFileAndLogMessage(
                                                                "Processed subject " + subject + " for " + metadataObj.getPath() + " with similarity " + similarity.toString(),
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
                    val retinafaceFileExists = classLoader.getResource("lib/retinaface.zip") != null
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
                        recognitionCount = faceRecognizer.startPredict()
                    } else {
                        logger.log(
                            Level.WARNING,
                            "Missing lib files for DJL face scan"
                        )
                        if (threadFile != null) {
                            FileUtils.writeToThreadFileAndLogMessage(
                        "Missing lib files for DJL face scan",
                                threadFile
                            )
                        }
                    }
                }
            }

            return recognitionCount
        }
    }
}