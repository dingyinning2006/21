package com.example.demo.speech;

import com.example.demo.util.ConfigUtil;
import okhttp3.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class SpeechRecognitionService {

    private final String secretId;
    private final String secretKey;
    private static final String SERVICE = "asr";
    private static final String HOST = "asr.tencentcloudapi.com";
    private static final String REGION = "ap-guangzhou";
    private static final String VERSION = "2019-06-14";
    private static final String ACTION = "SentenceRecognition";

    private final OkHttpClient httpClient = new OkHttpClient();

    public SpeechRecognitionService() {
        this.secretId = ConfigUtil.get("tencent.asr.secret-id");
        this.secretKey = ConfigUtil.get("tencent.asr.secret-key");
    }

    public String recognize(byte[] silkBytes) {
        try {
            String dataBase64 = Base64.getEncoder().encodeToString(silkBytes);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("EngSerViceType", "16k_zh");
            body.put("SourceType", 1);
            body.put("VoiceFormat", "silk");
            body.put("Data", dataBase64);
            body.put("DataLen", silkBytes.length);

            String bodyJson = toJson(body);
            long timestamp = System.currentTimeMillis() / 1000;
            String authorization = buildAuthorization(bodyJson, timestamp);

            Request request = new Request.Builder()
                    .url("https://" + HOST + "/")
                    .post(RequestBody.create(bodyJson, null))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json")
                    .header("Host", HOST)
                    .header("X-TC-Action", ACTION)
                    .header("X-TC-Version", VERSION)
                    .header("X-TC-Region", REGION)
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String result = response.body().string();
                System.out.println("[腾讯ASR返回] " + result);

                if (result.contains("\"Result\":\"")) {
                    int start = result.indexOf("\"Result\":\"") + 10;
                    int end = result.indexOf("\"", start);
                    return result.substring(start, end);
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildAuthorization(String bodyJson, long timestamp) throws Exception {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp * 1000));

        // 步骤1：规范请求串
        String canonicalHeaders = "content-type:application/json\n"
                + "host:" + HOST + "\n"
                + "x-tc-action:" + ACTION.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String hashedRequestPayload = sha256Hex(bodyJson);
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;

        // 步骤2：待签名字符串
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;

        // 步骤3：计算签名
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, SERVICE);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = bytesToHex(hmac256(secretSigning, stringToSign));

        // 调试输出
        System.out.println("=== 签名调试 ===");
        System.out.println("timestamp=" + timestamp);
        System.out.println("date=" + date);
        System.out.println("secretId=" + secretId);
        System.out.println("secretKey长度=" + (secretKey == null ? "null" : secretKey.length()));
        System.out.println("canonicalRequest=\n" + canonicalRequest);
        System.out.println("stringToSign=\n" + stringToSign);
        System.out.println("signature=" + signature);
        System.out.println("================");

        return "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] hmac256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String) sb.append("\"").append(v).append("\"");
            else sb.append(v);
        }
        return sb.append("}").toString();
    }
}