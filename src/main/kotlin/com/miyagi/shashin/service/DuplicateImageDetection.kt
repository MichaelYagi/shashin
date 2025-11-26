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
import kotlin.math.abs
import kotlin.math.round

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

    private var bitString: String = ""

    // Average Hash
    fun ahash(imageFile: File?, resolution: Int = 64): BigInteger? {
        checkAndThrowIllegalArgumentException(imageFile, resolution)

        this.resolution = resolution

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
                val pixels = IntArray((hashSize + 1) * hashSize)
                var sum = 0L
                var idx = 0

                for (y in 0 until hashSize) {
                    for (x in 0 until (hashSize + 1)) {
                        val gray = resized.raster.getSample(x, y, 0)
                        pixels[idx++] = gray
                        sum += gray
                    }
                }
                val avg = sum.toDouble() / pixels.size

                result = BigInteger.ZERO
                var bitIndex = 0
                val bitList = mutableListOf<Int>()
                var bitArray = mutableListOf<String>()

                for (pixel in pixels) {
                    val bit = if (pixel >= avg) 1 else 0
                    bitList.add(bitIndex, bit)
                    if (pixel >= avg) {
                        result = result?.setBit(bitIndex)
                        bitArray.add("1")
                    } else {
                        bitArray.add("0")
                    }
                    bitIndex++
                }

                bitArray.reverse()
                this.bitString = bitArray.joinToString("")
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

    // Difference Hash
    fun dhash(imageFile: File?, resolution: Int = 64): BigInteger? {
        checkAndThrowIllegalArgumentException(imageFile, resolution)

        this.resolution = resolution

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
                var bitArray = mutableListOf<String>()

                for (y in 0 until hashSize) {
                    for (x in 0 until hashSize) {
                        val leftPix = resized.raster.getSample(x, y, 0)
                        val rightPix = resized.raster.getSample(x + 1, y, 0)

                        // Compare pixels grey scale from black to white (0-255)
                        // Set bit if current pixel is lighter than neighbor
                        if (leftPix > rightPix) {
                            // returns a new BigInteger with the bit at position bitIndex set to 1
                            result = result?.setBit(bitIndex)
                            bitArray.add("1")
                        } else {
                            bitArray.add("0")
                        }
                        bitIndex++
                    }
                }

                bitArray.reverse()
                this.bitString = bitArray.joinToString("")
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

    fun phash(imageFile: File?, resolution: Int = 64, highfreqFactor: Int = 4): BigInteger? {
        checkAndThrowIllegalArgumentException(imageFile, resolution)

        this.resolution = resolution

        var hashSize = resolution/8
        var image: BufferedImage? = null
        var result: BigInteger? = null

        try {
            image = ImageIO.read(imageFile)

            if (image != null) {
                // Step 1: Resize to larger size (hashSize * highfreqFactor)
                val imgSize = hashSize * highfreqFactor
                val resized = resizeAndGrayscale(image, imgSize, imgSize)

                // Step 2: Convert image to grayscale matrix
                val pixels = Array(imgSize) { DoubleArray(imgSize) }
                for (y in 0 until imgSize) {
                    for (x in 0 until imgSize) {
                        pixels[y][x] = resized.raster.getSample(x, y, 0).toDouble()
                    }
                }

                // Step 3: Apply 2D DCT
                val dct = dctLowFreq(pixels, hashSize)

                // Step 4: Take top-left low-frequency coefficients
                val dctLowFreq = Array(hashSize) { DoubleArray(hashSize) }
                for (y in 0 until hashSize) {
                    for (x in 0 until hashSize) {
                        dctLowFreq[y][x] = dct[y][x]
                    }
                }

                // Step 5: Compute median
                // dctLowFreq is Array<DoubleArray>
                val flat: List<Double> = dctLowFreq.flatMap { row -> row.toList() }.sorted()
                val med = flat[flat.size / 2]
                var bitArray = mutableListOf<String>()

                // Step 6: Build hash
                result = BigInteger.ZERO
                var bitIndex = 0
                for (y in 0 until hashSize) {
                    for (x in 0 until hashSize) {
                        val bit = if (dctLowFreq[y][x] > med) 1 else 0
                        if (bit == 1) {
                            result = result?.setBit(bitIndex)
                            bitArray.add("1")
                        } else {
                            bitArray.add("0")
                        }
                        bitIndex++
                    }
                }

                bitArray.reverse()
                this.bitString = bitArray.joinToString("")
            }
        } catch (e: IOException) {
            logger.log(
                Level.SEVERE,
                "Could not read image ${e.message}"
            )
        }

        return result
    }

    fun getBitString(): String {
        return this.bitString
    }

    fun getResolution(): Int {
        return this.resolution
    }

    private fun checkAndThrowIllegalArgumentException(imageFile: File?, resolution: Int) {
        if (resolution%8 != 0 || imageFile == null || !imageFile.exists()) {
            throw IllegalArgumentException("resolution must be divisible by 8 and image must exist")
        }
    }

    private fun dctLowFreq(input: Array<DoubleArray>, blockSize: Int): Array<DoubleArray> {
        val n = input.size
        val m = input[0].size
        val output = Array(blockSize) { DoubleArray(blockSize) }

        for (u in 0 until blockSize) {
            for (v in 0 until blockSize) {
                var sum = 0.0
                for (x in 0 until n) {
                    for (y in 0 until m) {
                        sum += input[x][y] *
                                kotlin.math.cos(((2 * x + 1) * u * Math.PI) / (2 * n)) *
                                kotlin.math.cos(((2 * y + 1) * v * Math.PI) / (2 * m))
                    }
                }
                val alphaU = if (u == 0) kotlin.math.sqrt(1.0 / n) else kotlin.math.sqrt(2.0 / n)
                val alphaV = if (v == 0) kotlin.math.sqrt(1.0 / m) else kotlin.math.sqrt(2.0 / m)
                output[u][v] = alphaU * alphaV * sum
            }
        }
        return output
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

        fun isDuplicate(filename1: String?, filename2: String?, algorithm: String = "dhash"): Boolean {
            var isDuplicate = false
            if (filename1 != null && filename2 != null) {
                val file1 = File(filename1)
                val file2 = File(filename2)

                val i = DuplicateImageDetection()
                var hash1: BigInteger? = null
                var hash2: BigInteger? = null
                if (algorithm == "dhash") {
                    hash1 = i.dhash(file1)
                    hash2 = i.dhash(file2)
                } else {
                    hash1 = i.ahash(file1)
                    hash2 = i.ahash(file2)
                }

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

            // Count the number of set bits (Hamming weight) between the 2 hashes
            return xorResult.bitCount()
        }

        fun normalizedHammingDistance(hash1: BigInteger?, hash2: BigInteger?, resolution: Int = 64): Double {
            if (resolution%8 != 0 || hash1 == null || hash2 == null) {
                throw IllegalArgumentException("resolution must be divisible by 8 and hash can't be null")
            }

            return 1.0 - similarity(hash1, hash2, resolution)
        }

        fun similarity(hash1: BigInteger?, hash2: BigInteger?, resolution: Int = 64): Double {
            if (resolution%8 != 0 || hash1 == null || hash2 == null) {
                throw IllegalArgumentException("resolution must be divisible by 8 and image can't be null")
            }

            val hashSize = resolution/8
            val totalBits = hashSize * hashSize
            val distance = hammingDistance(hash1, hash2)!!.toInt()
            return (totalBits - distance).toDouble() / totalBits.toDouble()
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

        fun findAndStoreDuplicates(duplicatesRepository: DuplicatesRepository, threshold: Int = 5, page: Int = 0, size: Int = 3000): Int {
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
                        val duplicates = tree.search(query, threshold = threshold)

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