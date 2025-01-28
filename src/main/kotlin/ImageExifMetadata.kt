package org.hildan.orfeus.zwanger

import io.ktor.utils.io.core.*
import kotlinx.datetime.*
import kotlinx.datetime.format.*
import org.apache.commons.imaging.*
import org.apache.commons.imaging.formats.jpeg.*
import org.apache.commons.imaging.formats.jpeg.exif.*
import org.apache.commons.imaging.formats.tiff.constants.*
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii
import org.apache.commons.imaging.formats.tiff.write.*
import java.nio.file.*
import kotlin.io.path.*

@OptIn(FormatStringsInDatetimeFormats::class)
private val exifDateTimeFormat = LocalDateTime.Format {
    byUnicodePattern("yyyy:MM:dd HH:mm:ss")
}

fun Path.setJpegDateTaken(newDateTaken: LocalDateTime) {
    val initialJpeg = toFile()
    val metadata = Imaging.getMetadata(initialJpeg) ?: JpegImageMetadata(null, null)
    val jpegMetadata = metadata as? JpegImageMetadata ?: error("Not a JPEG image")
    val outputSet = jpegMetadata.exif?.outputSet ?: TiffOutputSet()
    outputSet.getOrCreateExifDirectory()
        .addOrReplaceField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, newDateTaken.format(exifDateTimeFormat))

    val temp = resolveSibling("$name~temp")
    temp.outputStream().use {
        ExifRewriter().updateExifMetadataLossless(initialJpeg, it, outputSet)
    }
    temp.moveTo(this, overwrite = true)
}

private fun TiffOutputDirectory.addOrReplaceField(tagInfo: TagInfoAscii, value: String) {
    removeField(tagInfo)
    add(tagInfo, value)
}
