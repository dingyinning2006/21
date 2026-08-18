package com.example.demo.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class WeatherClient {

    // 你的专属 API Host
    private static final String BASE_URL = "https://pb5g7eyttt.re.qweatherapi.com/v7";
    // 你的 API Key（后面建议移到配置文件）
    private static final String API_KEY = "e4c8cf73240f45e9a85fc5d4c410f902";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WeatherClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取实时天气
     * @param locationId 城市ID，长沙=101250101
     */
    public WeatherResponse getNowWeather(String locationId) throws IOException {
        String url = BASE_URL + "/weather/now?location=" + locationId + "&key=" + API_KEY;
        return request(url);
    }

    /**
     * 获取3天预报
     */
    public WeatherResponse getDailyForecast(String locationId) throws IOException {
        String url = BASE_URL + "/weather/3d?location=" + locationId + "&key=" + API_KEY;
        return request(url);
    }

    private WeatherResponse request(String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("天气API请求失败: " + response.code());
            }
            String json = response.body().string();
            return objectMapper.readValue(json, WeatherResponse.class);
        }
    }
}
