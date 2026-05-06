package com.lenzbeyer.hathor.media

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin wrapper around FFmpegKit. Per SPEC §10.1: bestaudio → MP3 320 CBR. */
@Singleton
class FFmpegTranscoder @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    suspend fun toMp3CBR320(input: File): File = withContext(Dispatchers.IO) {
        val outDir = File(ctx.cacheDir, "mp3").apply { mkdirs() }
        val out = File(outDir, "${input.nameWithoutExtension}.mp3")
        if (out.exists()) out.delete()

        val cmd = "-i \"${input.absolutePath}\" -vn -c:a libmp3lame -b:a 320k -id3v2_version 3 -write_xing 0 \"${out.absolutePath}\""
        val session = FFmpegKit.execute(cmd)
        val rc = session.returnCode
        if (!ReturnCode.isSuccess(rc)) {
            error("FFmpeg failed (rc=$rc): ${session.allLogsAsString.takeLast(2000)}")
        }
        out
    }
}
