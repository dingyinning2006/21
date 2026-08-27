package com.example.demo.audio.codec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
// SILK 音频转换服务：通过项目中的外部编码器/解码器完成格式转换。
public class SilkConverterService {

    private final String encoderPath;
    private final String decoderPath;

    public SilkConverterService(
            @Value("${silk.encoder-path:tools/silk/silk_v3_encoder.exe}") String encoderPath,
            @Value("${silk.decoder-path:tools/silk/silk_v3_decoder.exe}") String decoderPath
    ) {
        this.encoderPath = encoderPath;
        this.decoderPath = decoderPath;
    }

    public byte[] convertPcmToSilk(byte[] pcmBytes, int sampleRate) {
        // PCM 已经是原始采样数据，可以直接交给 SILK 编码器。
        return runSilkEncoder(pcmBytes, sampleRate, "PCM 转 SILK 失败");
    }

    public byte[] convertWavToSilk(byte[] wavBytes, int sampleRate) {
        // WAV 包含文件头，编码前先提取其中的 PCM data chunk。
        byte[] pcmBytes = extractPcmBytesFromWav(wavBytes);
        return runSilkEncoder(pcmBytes, sampleRate, "WAV 转 SILK 失败");
    }

    public byte[] convertSilkToWav(byte[] silkBytes, int sampleRate) {
        // 微信收到的 SILK 语音先解码为 PCM，再补 WAV 文件头供其他服务使用。
        byte[] pcmBytes = runSilkDecoder(silkBytes, sampleRate);
        return pcmToWav(pcmBytes, sampleRate);
    }

    private byte[] runSilkEncoder(byte[] pcmBytes, int sampleRate, String errorPrefix) {
        Path tempDir = null;

        try {
            // 外部程序通过临时文件读写，因此每次转换创建独立目录避免文件名冲突。
            tempDir = Files.createTempDirectory("wechat-voice-");
            Path pcmPath = tempDir.resolve("input.pcm");
            Path silkPath = tempDir.resolve("output.silk");

            Files.write(pcmPath, pcmBytes);

            Process process = new ProcessBuilder(
                    encoderPath,
                    pcmPath.toString(),
                    silkPath.toString(),
                    "-Fs_API", String.valueOf(sampleRate),
                    "-Fs_maxInternal", String.valueOf(sampleRate),
                    "-packetlength", "20",
                    "-rate", "25000",
                    "-tencent",
                    "-quiet"
            ).redirectErrorStream(true).start();

            boolean finished = process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                // 编码器卡住时强制结束，避免阻塞微信消息主循环。
                process.destroyForcibly();
                throw new RuntimeException("SILK 转换超时");
            }

            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new RuntimeException("SILK 转换失败：" + output);
            }

            return Files.readAllBytes(silkPath);
        } catch (Exception e) {
            throw new RuntimeException(errorPrefix + "：" + e.getMessage(), e);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    private byte[] runSilkDecoder(byte[] silkBytes, int sampleRate) {
        Path tempDir = null;

        try {
            // 解码流程与编码类似，只是输入输出文件的方向相反。
            tempDir = Files.createTempDirectory("wechat-voice-");
            Path silkPath = tempDir.resolve("input.silk");
            Path pcmPath = tempDir.resolve("output.pcm");

            Files.write(silkPath, silkBytes);

            Process process = new ProcessBuilder(
                    decoderPath,
                    silkPath.toString(),
                    pcmPath.toString(),
                    "-Fs_API", String.valueOf(sampleRate),
                    "-quiet"
            ).redirectErrorStream(true).start();

            boolean finished = process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("SILK 解码超时");
            }

            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new RuntimeException("SILK 解码失败：" + output);
            }

            return Files.readAllBytes(pcmPath);
        } catch (Exception e) {
            throw new RuntimeException("SILK 转 PCM 失败：" + e.getMessage(), e);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    private byte[] pcmToWav(byte[] pcmBytes, int sampleRate) {
        try {
            // Java Sound 根据 PCM 参数生成标准 WAV 头和音频数据。
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    1,
                    2,
                    sampleRate,
                    false
            );

            try (ByteArrayInputStream input = new ByteArrayInputStream(pcmBytes);
                 AudioInputStream audioInputStream = new AudioInputStream(input, format, pcmBytes.length / 2);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                AudioSystem.write(audioInputStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, output);
                return output.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("PCM 转 WAV 失败：" + e.getMessage(), e);
        }
    }

    private byte[] extractPcmBytesFromWav(byte[] wavBytes) {
        // WAV 可能包含可变长度的其他 chunk，不能假设 PCM 数据固定从某个字节开始。
        int dataChunkOffset = findDataChunkOffset(wavBytes);
        if (dataChunkOffset < 0) {
            throw new RuntimeException("WAV 文件中没有找到 data chunk");
        }

        int dataStart = dataChunkOffset + 8;
        int declaredSize = readLittleEndianInt(wavBytes, dataChunkOffset + 4);
        int remainingSize = wavBytes.length - dataStart;
        int dataSize = declaredSize > 0 && declaredSize <= remainingSize ? declaredSize : remainingSize;

        return Arrays.copyOfRange(wavBytes, dataStart, dataStart + dataSize);
    }

    private int findDataChunkOffset(byte[] wavBytes) {
        // 查找 ASCII 的 data 标记，并返回该 chunk 的起始位置。
        for (int i = 12; i <= wavBytes.length - 8; i++) {
            if (wavBytes[i] == 'd'
                    && wavBytes[i + 1] == 'a'
                    && wavBytes[i + 2] == 't'
                    && wavBytes[i + 3] == 'a') {
                return i;
            }
        }
        return -1;
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
    }

    private void deleteTempDir(Path tempDir) {
        // 无论转换成功还是失败，都清理临时目录和中间文件。
        if (tempDir == null) {
            return;
        }

        try {
            Files.walk(tempDir)
                    .sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
