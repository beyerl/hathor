package com.lenzbeyer.hathor.media

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **STUB.** SPEC §10.1 specifies bestaudio → MP3 320 CBR via FFmpegKit.
 *
 * The original `com.arthenica:ffmpeg-kit-*` artifacts were delisted from Maven Central
 * when the repo was archived in early 2025. Until a maintained fork is pinned, this
 * transcoder copies the input file verbatim and renames it to `.mp3` so the rest of
 * the pipeline can run end-to-end against valid file paths during development. The
 * resulting "MP3" is whatever container yt-dlp produced (Opus / M4A) — *not* a real
 * MP3. Do not ship a release until this is replaced.
 *
 * Replacement options to evaluate:
 *  - Community fork on JitPack (e.g. `com.github.<fork>/ffmpeg-kit:6.0-2.LTS`)
 *  - A new pin from a fresh fork that publishes to Maven Central
 *  - Self-built AAR from the upstream archived sources
 *  - Native MediaCodec → LAME-via-JNI alternative (large refactor)
 */
@Singleton
class FFmpegTranscoder @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    suspend fun toMp3CBR320(input: File): File = withContext(Dispatchers.IO) {
        val outDir = File(ctx.cacheDir, "mp3").apply { mkdirs() }
        val out = File(outDir, "${input.nameWithoutExtension}.mp3")
        if (out.exists()) out.delete()

        Log.w(
            "FFmpegTranscoder",
            "STUB: copying ${input.name} → ${out.name} without transcoding. " +
                "See SPEC §10.1 / §15 risk #1.",
        )
        input.copyTo(out, overwrite = true)
        out
    }
}
