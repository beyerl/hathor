package com.lenzbeyer.hathor.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.sciss.jump3r.lowlevel.LameEncoder
import java.io.BufferedOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import javax.sound.sampled.AudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Class is still named FFmpegTranscoder for callsite compatibility, but the implementation
// is MediaCodec (decode → 16-bit PCM) + jump3r LameEncoder (PCM → MP3 320 CBR). Original
// com.arthenica:ffmpeg-kit was delisted from Maven Central early 2025.
@Singleton
class FFmpegTranscoder @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    suspend fun toMp3CBR320(input: File): File = withContext(Dispatchers.IO) {
        val outDir = File(ctx.cacheDir, "mp3").apply { mkdirs() }
        val out = File(outDir, "${input.nameWithoutExtension}.mp3")
        if (out.exists()) out.delete()

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(input.absolutePath)
            val (trackIndex, inputFormat) = findAudioTrack(extractor)
                ?: error("No audio track in ${input.name}")
            extractor.selectTrack(trackIndex)

            val mime         = inputFormat.getString(MediaFormat.KEY_MIME) ?: error("Audio track missing MIME")
            val sampleRate   = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            try {
                BufferedOutputStream(out.outputStream()).use { sink ->
                    encode(decoder, extractor, sink, sampleRate, channelCount)
                }
            } finally {
                runCatching { decoder.stop() }
                decoder.release()
            }
        } finally {
            extractor.release()
        }

        out
    }

    private fun findAudioTrack(extractor: MediaExtractor): Pair<Int, MediaFormat>? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i to format
        }
        return null
    }

    private fun encode(
        decoder: MediaCodec,
        extractor: MediaExtractor,
        sink: BufferedOutputStream,
        sampleRate: Int,
        channelCount: Int,
    ) {
        // MediaCodec on Android decodes to interleaved 16-bit signed little-endian PCM by default.
        val pcmFormat = AudioFormat(
            sampleRate.toFloat(),
            16,
            channelCount,
            true,   // signed
            false,  // little-endian
        )
        val channelMode = if (channelCount == 1)
            LameEncoder.CHANNEL_MODE_MONO
        else
            LameEncoder.CHANNEL_MODE_STEREO
        val encoder = LameEncoder(
            pcmFormat,
            320,                          // bitRate kbps
            channelMode,
            LameEncoder.QUALITY_HIGHEST,  // 2 — slowest, best
            false,                        // VBR off → CBR
        )
        try {
            val mp3Buf = ByteArray(encoder.mp3BufferSize)
            val info = MediaCodec.BufferInfo()
            val timeoutUs = 10_000L
            var inputDone  = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)
                            ?: error("Decoder returned null input buffer")
                        val read = extractor.readSampleData(inBuf, 0)
                        if (read < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, read, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIdx = decoder.dequeueOutputBuffer(info, timeoutUs)
                while (outIdx >= 0) {
                    if (info.size > 0) {
                        val outBuf = decoder.getOutputBuffer(outIdx)
                            ?: error("Decoder returned null output buffer")
                        val pcmBytes = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(pcmBytes, 0, info.size)
                        val mp3Bytes = encoder.encodeBuffer(pcmBytes, 0, info.size, mp3Buf)
                        if (mp3Bytes > 0) sink.write(mp3Buf, 0, mp3Bytes)
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                        break
                    }
                    outIdx = decoder.dequeueOutputBuffer(info, 0)
                }
            }

            val tail = encoder.encodeFinish(mp3Buf)
            if (tail > 0) sink.write(mp3Buf, 0, tail)
        } finally {
            encoder.close()
        }
    }
}
