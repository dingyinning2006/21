package com.example.demo.wechat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class QrCodeUtil {
    /**
     * 把文本/链接生成二维码图片，保存到本地
     */
    public static void generate(String content, String outputPath) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, 300, 300, hints);

        Path path = Paths.get(outputPath);
        MatrixToImageWriter.writeToPath(matrix, "PNG", path);
        System.out.println("二维码已保存到: " + path.toAbsolutePath());
    }
}