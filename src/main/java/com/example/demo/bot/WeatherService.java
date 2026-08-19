package com.example.demo.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 天气服务：用和风天气（QWeather）API 查实时天气。
 * 流程：城市名 -> 城市 ID（geo 接口）-> 实时天气（weather/now 接口）-> 拼成中文回复。
 * 注意：2025-06 起公共域名已停用，必须用控制台分配的专属 API Host
 * （形如 xxx.re.qweatherapi.com），配置在 weather.api-host；鉴权用 X-QW-Api-Key 请求头。
 */
@Service
public class WeatherService {

    private static final String GEO_PATH = "/geo/v2/city/lookup";
    private static final String NOW_PATH = "/v7/weather/now";

    private final String apiHost;
    private final String apiKey;
    private final String defaultCity;
    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(@Value("${weather.api-host}") String apiHost,
                          @Value("${weather.api-key}") String apiKey,
                          @Value("${weather.default-city}") String defaultCity) {
        this.apiHost = apiHost;
        this.apiKey = apiKey;
        this.defaultCity = defaultCity;
    }

    /** 查某个城市的当前天气，返回可直接发给用户的文本；city 为空时用配置的默认城市 */
    public String getWeather(String city) {
        String targetCity = (city == null || city.isBlank()) ? defaultCity : city;

        // 1. 城市名 -> 和风天气城市 ID
        String geoBody = restClient.get()
                .uri("https://" + apiHost + GEO_PATH + "?location={city}", targetCity)
                .header("X-QW-Api-Key", apiKey)
                .retrieve()
                .body(String.class);
        JsonNode location = objectMapper.readTree(geoBody).path("location").path(0);
        if (location.isMissingNode()) {
            throw new IllegalStateException("找不到城市：" + targetCity);
        }
        String locationId = location.path("id").asText();
        String cityName = location.path("name").asText();

        // 2. 查实时天气
        String weatherBody = restClient.get()
                .uri("https://" + apiHost + NOW_PATH + "?location={id}", locationId)
                .header("X-QW-Api-Key", apiKey)
                .retrieve()
                .body(String.class);
        JsonNode now = objectMapper.readTree(weatherBody).path("now");

        // 3. 拼成一句中文回复
        return String.format("%s 当前天气：%s，%s℃，体感%s℃，%s%s级，湿度%s%%",
                cityName,
                now.path("text").asText(),
                now.path("temp").asText(),
                now.path("feelsLike").asText(),
                now.path("windDir").asText(),
                now.path("windScale").asText(),
                now.path("humidity").asText());
    }
}
