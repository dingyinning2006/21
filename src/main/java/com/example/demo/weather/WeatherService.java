package com.example.demo.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
// 天气查询服务：从用户问题提取城市，调用 WeatherAPI 并整理成中文回复。
public class WeatherService {

    private final String apiKey;
    private final String baseUrl;
    private final String defaultCity;
    private final String lang;
    private final String aqi;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WeatherService(
            @Value("${weather.api-key:}") String apiKey,
            @Value("${weather.base-url:https://api.weatherapi.com/v1/current.json}") String baseUrl,
            @Value("${weather.default-city:Nanjing}") String defaultCity,
            @Value("${weather.lang:zh}") String lang,
            @Value("${weather.aqi:no}") String aqi
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultCity = defaultCity;
        this.lang = lang;
        this.aqi = aqi;
    }

    public String query(String userQuestion) {
        // 没有配置密钥时直接返回提示，不发起无效网络请求。
        if (apiKey == null || apiKey.isBlank()) {
            return "天气 API Key 还没有配置，请先设置 WEATHERAPI_KEY 环境变量。";
        }

        String city = normalizeCity(extractCity(userQuestion));

        try {
            // 对查询参数进行 URL 编码，避免中文城市名或特殊字符破坏请求地址。
            String url = baseUrl
                    + "?key=" + encode(apiKey)
                    + "&q=" + encode(city)
                    + "&lang=" + encode(lang)
                    + "&aqi=" + encode(aqi);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "天气查询失败，状态码：" + response.statusCode() + "，响应：" + response.body();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.path("current");
            JsonNode location = root.path("location");

            // 从第三方响应中读取需要展示给用户的字段，并为缺失字段提供默认值。
            String cityName = location.path("name").asText(city);
            String region = location.path("region").asText("");
            String country = location.path("country").asText("");
            String condition = current.path("condition").path("text").asText("未知天气");
            double temperature = current.path("temp_c").asDouble(Double.NaN);
            double feelsLike = current.path("feelslike_c").asDouble(Double.NaN);
            int humidity = current.path("humidity").asInt(-1);
            double windKph = current.path("wind_kph").asDouble(Double.NaN);

            String place = buildPlace(cityName, region, country);
            String tempText = Double.isNaN(temperature) ? "-" : String.valueOf(temperature);
            String feelsText = Double.isNaN(feelsLike) ? "-" : String.valueOf(feelsLike);
            String windText = Double.isNaN(windKph) ? "-" : String.valueOf(windKph);
            String humidityText = humidity < 0 ? "-" : String.valueOf(humidity);

            return place + "当前天气：" + condition
                    + "，气温 " + tempText + "℃"
                    + "，体感 " + feelsText + "℃"
                    + "，湿度 " + humidityText + "%"
                    + "，风速 " + windText + " km/h。";
        } catch (Exception e) {
            return "天气查询异常：" + e.getMessage();
        }
    }

    private String buildPlace(String cityName, String region, String country) {
        StringBuilder builder = new StringBuilder(cityName == null || cityName.isBlank() ? defaultCity : cityName);
        if (region != null && !region.isBlank()) {
            builder.append(region);
        }
        if (country != null && !country.isBlank()) {
            builder.append(country);
        }
        return builder.toString();
    }

    private String extractCity(String question) {
        // 去除常见天气问法，只保留城市名称。
        if (question == null || question.isBlank()) {
            return defaultCity;
        }

        String city = question.trim()
                .replace("今天", "")
                .replace("现在", "")
                .replace("当前", "")
                .replace("天气", "")
                .replace("气温", "")
                .replace("温度", "")
                .replace("怎么样", "")
                .replace("如何", "")
                .replace("多少", "")
                .replace("会不会下雨", "")
                .replace("下雨吗", "")
                .replace("？", "")
                .replace("?", "")
                .trim();

        return city.isBlank() ? defaultCity : city;
    }

    private String normalizeCity(String city) {
        // WeatherAPI 对部分城市名更偏好英文写法，这里做少量常用映射。
        return switch (city) {
            case "北京" -> "Beijing";
            case "上海" -> "Shanghai";
            case "南京" -> "Nanjing";
            case "广州" -> "Guangzhou";
            case "深圳" -> "Shenzhen";
            case "杭州" -> "Hangzhou";
            case "苏州" -> "Suzhou";
            case "成都" -> "Chengdu";
            case "武汉" -> "Wuhan";
            case "西安" -> "Xi'an";
            default -> city;
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
