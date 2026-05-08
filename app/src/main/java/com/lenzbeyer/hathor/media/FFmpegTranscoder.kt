package com.lenzbeyer.hathor.media

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Transcodes any container yt-dlp produced (Opus/M4A/WebM) to MP3 320 kbps CBR with the
 * exact ffmpeg flag set called out in SPEC §10.1:
 *
 *   ffmpeg -y -i <raw> -vn -c:a libmp3lame -b:a 320k -id3v2_version 3 -write_xing 0 <out>
 *
 * Implementation: FFmpegKit (audio package). Replaces the earlier MediaCodec + jump3r
 * pipeline, which couldn't run on Android because jump3r references the reserved
 * `javax.sound.sampled` namespace, which the ART class loader refuses to resolve.
 */
@Singleton
class FFmpegTranscoder @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    suspend fun toMp3CBR320(input: File): File = withContext(Dispatchers.IO) {
        val outDir = File(ctx.cacheDir, "mp3").apply { mkdirs() }
        val out = File(outDir, "${input.nameWithoutExtension}.mp3")
        if (out.exists()) out.delete()

        val args = arrayOf(
            "-y",
            "-i", input.absolutePath,
            "-vn",
            "-c:a", "libmp3lame",
            "-b:a", "320k",
            "-id3v2_version", "3",
            "-write_xing", "0",
            out.absolutePath,
        )

        Log.d(LOG, "ffmpeg ${args.joinToString(" ")}")
        val session = FFmpegKit.executeWithArguments(args)
        val rc = session.returnCode

        if (!ReturnCode.isSuccess(rc)) {
            val tail = session.allLogsAsString
                ?.lineSequence()
                ?.toList()
                ?.takeLast(20)
                ?.joinToString("\n")
                .orEmpty()
            error("ffmpeg exit ${rc.value}: $tail")
        }

        if (!out.exists() || out.length() == 0L) {
            error("ffmpeg returned success but output is missing or empty: ${out.absolutePath}")
        }

        Log.d(LOG, "ffmpeg done: ${out.length()} B → ${out.name}")
        out
    }

    private companion object {
        const val LOG = "Hathor/FFmpeg"
    }
}
