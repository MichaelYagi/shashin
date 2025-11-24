package com.miyagi.shashin.service

import com.miyagi.shashin.model.Duplicates
import com.miyagi.shashin.repository.DuplicatesRepository
import com.miyagi.shashin.util.BKTree
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.util.Base64
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

// Example usage:
//val file1 = File(filenameOne)
//val file2 = File(filenameTwo)
//
//val i = DuplicateImageDetection()
//
//var hash1 = i.dhash(file1, resolution)
//var hash2 = i.dhash(file2, resolution)
//
//val isDuplicate = DuplicateImageDetection.isDuplicate(hash1, hash2)
class DuplicateImageDetection {

    private var logger: Logger = Logger.getLogger(DuplicateImageDetection::class.simpleName)

    // Default resolution
    private var resolution: Int = 64

    // Difference Hash
    fun dhash(imageFile: File?, resolution: Int = 64): BigInteger? {
        if (resolution%8 != 0 || imageFile == null || !imageFile.exists()) {
            throw IllegalArgumentException("resolution must be divisible by 8 and image must exist")
        }

        var hashSize = resolution/8
        var image: BufferedImage? = null
        var result: BigInteger? = null

        try {
            image = ImageIO.read(imageFile)

            if (image != null) {
                // 1) Grayscale normalization and tiny resize
                val resized = resizeAndGrayscale(image, hashSize + 1, hashSize)

                // Download
                // saveBufferedImageToFile(resized, "C:/Users/Michael/Downloads/image_${TextUtils.getCurrentTimestampMS().replace(":","").replace("-","").replace(" ","")}_${resolution}.jpg", "jpg")

                // 2) Generate differences left-to-right for each row
                result = BigInteger.ZERO
                var bitIndex = 0

                for (y in 0 until hashSize) {
                    for (x in 0 until hashSize) {
                        val leftPix = resized.raster.getSample(x, y, 0)
                        val rightPix = resized.raster.getSample(x + 1, y, 0)

                        // Compare pixels grey scale from black to white (0-255)
                        // Set bit if current pixel is lighter than neighbor
                        if (leftPix > rightPix) {
                            // returns a new BigInteger with the bit at position bitIndex set to 1
                            result = result?.setBit(bitIndex)
                        }
                        bitIndex++
                    }
                }
            } else {
                logger.log(
                    Level.SEVERE,
                    "Could not read image"
                )
            }
        } catch (e: IOException) {
            logger.log(
                Level.SEVERE,
                "Could not read image ${e.message}"
            )
        }

        return result
    }

    fun getResolution(): Int {
        return resolution
    }

    fun getBase64(file: File?): String? {
        try {
            if (file != null) {
                val os = ByteArrayOutputStream()
                ImageIO.write(ImageIO.read(file), "jpg", os)
                return Base64.getEncoder().encodeToString(os.toByteArray())
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error getBase64: ${e.message}")
        }

        return null
    }

    private fun resizeAndGrayscale(img: BufferedImage, width: Int, height: Int): BufferedImage {
        val gray = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        val g2d = gray.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.drawImage(img, 0, 0, width, height, null)
        g2d.dispose()
        return gray
    }

    private fun saveBufferedImageToFile(image: BufferedImage, filePath: String, formatName: String) {
        try {
            val outputFile = File(filePath)
            if (outputFile.createNewFile()) {
                ImageIO.write(image, formatName, outputFile)
                println("BufferedImage successfully saved to: ${outputFile.absolutePath}")
            } else {
                println("File already exists: ${outputFile.absolutePath}")
            }

        } catch (e: Exception) {
            println("Error saving BufferedImage: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        private var logger: Logger = Logger.getLogger(DuplicateImageDetection::class.simpleName)

        fun isDuplicate(hash1: BigInteger, hash2: BigInteger, threshold: Int = 5): Boolean? {
            val distance = hammingDistance(hash1, hash2)
            if (distance != null) {
                return distance <= threshold
            }

            return null
        }

        fun isDuplicate(filename1: String?, filename2: String?): Boolean {
            var isDuplicate = false
            if (filename1 != null && filename2 != null) {
                val file1 = File(filename1)
                val file2 = File(filename2)

                val i = DuplicateImageDetection()
                val hash1 = i.dhash(file1)
                val hash2 = i.dhash(file2)
                if (hash1 != null && hash2 != null) {
                    isDuplicate = isDuplicate(hash1, hash2) == true
                } else {
                    isDuplicate = false
                }
            }
            return isDuplicate
        }

        fun hammingDistance(hash1: BigInteger?, hash2: BigInteger?): Int? {
            if (hash1 == null || hash2 == null) {
                throw IllegalArgumentException("resolution must be divisible by 8")
            }

            // XOR the two BigIntegers
            val xorResult = hash1.xor(hash2)

            // Count the number of set bits (Hamming weight)
            return xorResult.bitCount()
        }

        fun similarityPercentage(hash1: BigInteger?, hash2: BigInteger?, resolution: Int = 64): Double {
            if (resolution%8 != 0 || hash1 == null || hash2 == null) {
                throw IllegalArgumentException("resolution must be divisible by 8 and image can't be null")
            }

            val hashSize = resolution/8
            val totalBits = hashSize * hashSize
            val distance = hammingDistance(hash1, hash2)!!.toInt()
            return (totalBits - distance).toDouble() / totalBits.toDouble()
        }

        fun findAndStoreDuplicates(duplicatesRepository: DuplicatesRepository, page: Int = 1, size: Int = 2500): Int {
            // Implement as part of dupe module
            // Distance function using Hamming distance
            val tree = BKTree { h1, h2 -> hammingDistance(h1, h2)!! }

            val metadataList = duplicatesRepository.findDuplicateImageHash(page, size)
            logger.log(
                Level.INFO,
                "metadataList query size: "+metadataList?.size
            )

            val duplicateList = mutableListOf<Duplicates>()

            if (metadataList != null) {
                // Insert hashes
                for (metadata in metadataList) {
                    if (metadata.getDuplicateHash() != null) {
                        logger.log(
                            Level.INFO,
                            "Adding entry into BKTree ${metadata.getId()}"
                        )

                        val entry = BKTree.HashEntry(metadata.getId(), BigInteger(metadata.getDuplicateHash().toString()))
                        tree.insert(entry)
                    }
                }

                // Search hashes
                for (metadata in metadataList) {
                    if (metadata.getDuplicateHash() != null) {
                        logger.log(
                            Level.INFO,
                            "Searching duplicate hash ${metadata.getDuplicateHash()} for ${metadata.getId()}"
                        )

                        val query = BigInteger(metadata.getDuplicateHash().toString())
                        val duplicates = tree.search(query, threshold = 5)

                        for (dupe in duplicates) {
                            if (dupe.id != metadata.getId()) { // ignore self
                                val dupCount = duplicatesRepository.findDuplicateMetadataId(metadata.getId(), dupe.id)
                                if (dupCount == 0) {
                                    logger.log(
                                        Level.INFO,
                                        "Duplicate ${metadata.getId()} found: id=${dupe.id}, distance=${hammingDistance(query, dupe.hash)}"
                                    )

                                    val duplicate = Duplicates()
                                    duplicate.setImageIdOne(metadata.getId())
                                    duplicate.setImageIdTwo(dupe.id)
                                    duplicate.setDistance(hammingDistance(query, dupe.hash))
                                    duplicate.setCreatedAt(getCurrentTimestamp())
                                    duplicateList.add(duplicate)
                                }
                            }
                        }
                    }
                }

                if (duplicateList.isNotEmpty()) {
                    duplicatesRepository.saveAll(duplicateList)
                }
            }

            return duplicateList.size
        }
    }
}