package com.miyagi.shashin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import dev.kiroku.ExtractionStatus
import dev.kiroku.MediaMetadata
import dev.kiroku.MediaMetadataExtractor
import dev.kiroku.MetadataResult
import dev.kiroku.model.MediaFormat
import dev.kiroku.model.NormalizedValue
import dev.kiroku.model.Rational
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.ceil

/**
 * Media metadata through Kiroku (`com.github.MichaelYagi.kiroku:kiroku-core` from JitPack), replacing metadata-extractor.
 *
 * One extractor serves the whole application. It is thread-safe and uses only Kiroku's own parsers,
 * so no ExifTool or ffprobe process is started. Its worker threads are daemon threads, so it needs no shutdown hook.
 *
 * The helpers keep the values Shashin stores in the same format as before (exposure "1/250", duration "m:ss",
 * metadata-extractor style expected extensions, and so on), so existing records and new ones stay comparable.
 */
object KirokuMetadata {
    private val logger: Logger = Logger.getLogger(KirokuMetadata::class.simpleName)

    private val extractor: MediaMetadataExtractor by lazy { MediaMetadata.create() }

    /** Reads [file], or returns `null` when nothing could be read. Never throws. */
    fun read(file: File): MetadataResult? {
        val result =
            try {
                extractor.extract(file.toPath())
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read metadata for " + file.name + ": " + e.message)
                return null
            }
        when (result.status) {
            ExtractionStatus.COMPLETE -> {}
            ExtractionStatus.PARTIAL ->
                logger.log(Level.INFO, "Partial metadata for " + file.name + ": " + diagnosticSummary(result))
            ExtractionStatus.UNSUPPORTED, ExtractionStatus.FAILED -> {
                logger.log(Level.WARNING, "Could not read metadata for " + file.name + " (" + result.status + "): " + diagnosticSummary(result))
                return null
            }
        }
        return result
    }

    private fun diagnosticSummary(result: MetadataResult): String =
        result.diagnostics.take(3).joinToString("; ") { it.code.toString() + " " + it.message }

    /** The clockwise rotation, in degrees, to apply to the stored pixels (0, 90, 180 or 270). */
    fun rotation(result: MetadataResult?): Int = result?.gallery?.orientation?.rotationDegrees ?: 0

    /** `true` when the file states its orientation (EXIF Orientation, HEIF irot, or a video display matrix). */
    fun hasOrientation(result: MetadataResult?): Boolean = result?.gallery?.orientation?.rotationDegrees != null

    /** `true` when the pixels must be mirrored left to right *before* [rotation] is applied (EXIF orientations 2, 4, 5 and 7). */
    fun mirrored(result: MetadataResult?): Boolean = result?.gallery?.orientation?.mirrored == true

    /** The stored pixel size, with width and height swapped for 90° and 270° rotations. */
    fun orientedSize(result: MetadataResult?): Pair<Int, Int>? {
        val d = result?.gallery?.dimensions ?: return null
        val w = d.storedWidth ?: return null
        val h = d.storedHeight ?: return null
        return if (rotation(result) % 180 != 0) h to w else w to h
    }

    /**
     * The capture time as a wall-clock time. Times recorded with a time zone keep the local time at the place of capture
     * (the same convention as EXIF, so photos and videos taken together sort together). UTC-only sources such as the
     * QuickTime creation time are shown in the server's time zone.
     *
     * Without a capture time, the EXIF DateTime (the modify date) is used, as Shashin always did. `null` when the file
     * has neither.
     */
    fun capturedAtLocal(result: MetadataResult?): LocalDateTime? {
        if (result == null) return null
        result.gallery.capturedAt?.let { c ->
            return c.localDateTime ?: c.instant?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
        }
        val text = result.full.find("EXIF:IFD0:ModifyDate").firstOrNull()?.value?.toString()?.trim() ?: return null
        val parsed = runCatching { LocalDateTime.parse(text.take(19), EXIF_DATE_TIME) }.getOrNull() ?: return null
        // "0000:00:00 00:00:00" does not parse; also skip the epoch defaults that cameras write when the clock is unset.
        return parsed.takeIf { it.year >= 1900 && it != EPOCH_1970 && it != EPOCH_1904 }
    }

    private val EXIF_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    private val EPOCH_1970: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0)
    private val EPOCH_1904: LocalDateTime = LocalDateTime.of(1904, 1, 1, 0, 0)

    /** The detected MIME type, for example `image/jpeg` or `video/quicktime`. */
    fun mimeType(result: MetadataResult?): String? = result?.gallery?.mimeType

    /**
     * The extension that matches the detected content, spelled as metadata-extractor did, because Shashin compares it
     * with [FileUtils.allowableRawImageFiles] and uses it for downloads (for example `tiff`, not `tif`).
     */
    fun expectedExtension(result: MetadataResult?): String? {
        val format = MediaFormat.entries.firstOrNull { it.id == result?.gallery?.format } ?: return null
        return when (format) {
            MediaFormat.UNKNOWN -> null
            MediaFormat.TIFF, MediaFormat.BIGTIFF, MediaFormat.TIFF_RAW -> "tiff"
            MediaFormat.HEIF, MediaFormat.HEIC -> "heif"
            MediaFormat.APNG -> "png"
            else -> format.extensions.firstOrNull()
        }
    }

    /** "Make Model", as Shashin has always stored the camera. */
    fun camera(result: MetadataResult?): String? {
        val c = result?.gallery?.camera ?: return null
        return listOfNotNull(c.make?.takeIf { it.isNotBlank() }, c.model?.takeIf { it.isNotBlank() }).joinToString(" ").trim().ifBlank { null }
    }

    /** "LensMake LensModel", as Shashin has always stored the lens. */
    fun lens(result: MetadataResult?): String? {
        val c = result?.gallery?.camera ?: return null
        return listOfNotNull(c.lensMake?.takeIf { it.isNotBlank() }, c.lensModel?.takeIf { it.isNotBlank() }).joinToString(" ").trim().ifBlank { null }
    }

    fun iso(result: MetadataResult?): Int? = result?.gallery?.exposure?.iso

    /** The exposure time as Shashin stores it: "1/250", "5/1067", or "2" for whole seconds. */
    fun exposure(result: MetadataResult?): String? {
        val e = result?.gallery?.exposure ?: return null
        e.exposureTime?.let { formatRational(it) }?.let { return it }
        return e.exposureTimeSeconds?.takeIf { it > 0 }?.let { TextUtils.convertDecimalToFraction(it) }
    }

    private fun formatRational(r: Rational): String? {
        if (r.denominator == 0L || r.numerator <= 0L) return null
        val s = r.reduced()
        return if (s.numerator % s.denominator == 0L) (s.numerator / s.denominator).toString() else s.numerator.toString() + "/" + s.denominator
    }

    /** The f-number, rounded to one decimal place (f/2.8). */
    fun fNumber(result: MetadataResult?): Double? = result?.gallery?.exposure?.apertureFNumber?.let { round1(it) }

    /** The focal length in millimetres, rounded to one decimal place. */
    fun focalLength(result: MetadataResult?): Double? = result?.gallery?.exposure?.focalLengthMm?.let { round1(it) }

    private fun round1(value: Double): Double = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_EVEN).toDouble()

    fun latitude(result: MetadataResult?): Double? = result?.gallery?.location?.latitude

    fun longitude(result: MetadataResult?): Double? = result?.gallery?.location?.longitude

    /** The video or audio duration as "m:ss" or "h:m:ss", with seconds rounded up as before. */
    fun duration(result: MetadataResult?): String? {
        val g = result?.gallery ?: return null
        val d = g.video?.duration ?: g.audio?.duration ?: return null
        val total = ceil(d.toNanos() / 1_000_000_000.0).toLong()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        val ss = seconds.toString().padStart(2, '0')
        return if (hours > 0) "$hours:$minutes:$ss" else "$minutes:$ss"
    }

    /**
     * The value Shashin keeps in `compressionType`:
     * - for video, the encoder name written in the video track (for example "H.264", "HEVC" or "Lavc61 libx264"),
     *   or the codec name when the track has none, or "Unknown";
     * - for JPEG, the encoding process (for example "Baseline"); for PNG, "Deflate".
     *
     * `MediaServiceController` uses it to decide whether a MOV must be converted to MP4.
     */
    fun compressionType(result: MetadataResult?): String? {
        if (result == null) return null
        val observations = result.full.observations
        val videoGroup =
            observations.firstOrNull { it.tagName == "HandlerType" && it.value.toString() == "vide" }?.groupPath
        if (videoGroup != null) {
            val inTrack = observations.filter { it.groupPath == videoGroup }
            inTrack.firstOrNull { it.tagName == "CompressorName" }?.value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
            val fourCc = inTrack.firstOrNull { it.tagName == "CompressorID" }?.value?.toString()?.trim()
            return when (fourCc) {
                "avc1", "avc3" -> "H.264"
                "hvc1", "hev1" -> "HEVC"
                "jpeg" -> "JPEG"
                "mp4v" -> "MPEG-4"
                null -> null
                else -> "Unknown"
            }
        }
        val encoding = observations.firstOrNull { it.tagName == "EncodingProcess" }?.value
        if (encoding is NormalizedValue.Integer) {
            JPEG_PROCESS[encoding.value.toInt()]?.let { return it }
        }
        if (result.gallery.format == MediaFormat.PNG.id || result.gallery.format == MediaFormat.APNG.id) return "Deflate"
        return null
    }

    // The names metadata-extractor used for the JPEG start-of-frame markers.
    private val JPEG_PROCESS =
        mapOf(
            0 to "Baseline",
            1 to "Extended sequential, Huffman",
            2 to "Progressive, Huffman",
            3 to "Lossless, Huffman",
            5 to "Differential sequential, Huffman",
            6 to "Differential progressive, Huffman",
            7 to "Differential lossless, Huffman",
            9 to "Extended sequential, arithmetic",
            10 to "Progressive, arithmetic",
            11 to "Lossless, arithmetic",
            13 to "Differential sequential, arithmetic",
            14 to "Differential progressive, arithmetic",
            15 to "Differential lossless, arithmetic",
        )

    /** The camera's image quality setting from its maker notes when it is stored as text (for example "FINE" or "NORMAL"). */
    fun quality(result: MetadataResult?): String? =
        result?.full?.observations
            ?.firstOrNull { it.groupPath.firstOrNull() == "MakerNotes" && it.tagName == "Quality" && it.value is NormalizedValue.Text }
            ?.value?.toString()?.trim()?.ifBlank { null }

    // Major brands whose metadata-extractor description contained "MPEG", which MediaServiceController checks.
    private val MPEG_MAJOR_BRANDS =
        setOf("mmp4", "mp21", "mp71", "MSNV", "NDSC", "NDSH", "NDSM", "NDSP", "NDSS", "NDXC", "NDXH", "NDXM", "NDXP", "NDXS", "dvt1", "da0a")

    /** `true` for MP4 major brands that metadata-extractor described as MPEG (for example `mp21` or `MSNV`). */
    fun isMpegMajorBrand(code: String?): Boolean = code != null && code.trim() in MPEG_MAJOR_BRANDS

    /** The group in the EXIF sidecar that holds `MajorBrand`. */
    const val SIDECAR_FILE_TYPE_GROUP = "QuickTime:FileType"

    /**
     * Every tag Kiroku read, grouped by directory (for example `EXIF:IFD0`, `MakerNotes:Nikon`, `QuickTime:Track1`),
     * as display text. This is what the EXIF tab shows. Repeated tags get a " (2)", " (3)" suffix and long values are cut.
     */
    fun exifMap(result: MetadataResult?): Map<String, Map<String, String>> {
        val out = LinkedHashMap<String, LinkedHashMap<String, String>>()
        for (o in result?.full?.observations.orEmpty()) {
            val tags = out.getOrPut(o.groupPath.joinToString(":")) { LinkedHashMap() }
            val text = (o.displayValue ?: o.value.toString()).let { if (it.length > MAX_SIDECAR_VALUE) it.take(MAX_SIDECAR_VALUE) + "…" else it }
            var key = o.tagName
            var n = 2
            while (key in tags) key = o.tagName + " (" + n++ + ")"
            tags[key] = text
        }
        return out
    }

    private const val MAX_SIDECAR_VALUE = 4096

    /** Writes [exifMap] to `<sidecar>/metadata/<folder>/<file>.exif.yaml`, the file the EXIF tab and transcoding read. */
    fun writeExifSidecar(exifMap: Map<String, Map<String, String>>, sidecarDir: String, mediaFile: File) {
        if (exifMap.isEmpty()) return
        val metadataDirectory = sidecarDir.dropLast(1) + "/metadata"
        val fileRootDir: String = FileUtils.getRootDir(mediaFile)
        val exifFile = FileUtils.createFile("$metadataDirectory/$fileRootDir/" + mediaFile.name + ".exif.yaml") ?: return
        writeExifSidecar(exifMap, exifFile)
    }

    /** Writes [exifMap] to [exifFile] as YAML. */
    fun writeExifSidecar(exifMap: Map<String, Map<String, String>>, exifFile: File) {
        if (exifMap.isEmpty()) return
        val yamlFactory: YAMLFactory =
            YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build()
        ObjectMapper(yamlFactory).writeValue(exifFile, exifMap)
    }
}
