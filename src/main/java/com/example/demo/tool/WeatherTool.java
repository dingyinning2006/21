package com.example.demo.tool;

import com.example.demo.weather.WeatherClient;
import com.example.demo.weather.WeatherResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WeatherTool implements Tool {

    // 直接 new，不需要注入（和你原来 SimpleMessageHandler 里的写法一致）
    private final WeatherClient weatherClient = new WeatherClient();

    @Override
    public String getName() { return "get_weather"; }

    @Override
    public String getDescription() {
        return "查询指定城市的实时天气信息，包括温度、天气状况、风力、湿度。当用户询问天气、气温、是否下雨时调用。";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> city = new LinkedHashMap<>();
        city.put("type", "string");
        city.put("description", "要查询的城市名称，例如：北京、上海、长沙、南京");
        properties.put("city", city);
        return properties;
    }

    @Override
    public List<String> getRequired() {
        return Collections.singletonList("city");
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String city = (String) arguments.getOrDefault("city", "长沙");

        // 城市名 → locationId
        String locationId = switch (city) {
            case "北京" -> "101010100";
            case "上海" -> "101020100";
            case "南京" -> "101190101";
            case "广州" -> "101280101";
            case "深圳" -> "101280601";
            case "杭州" -> "101210101";
            case "成都" -> "101270101";
            default -> "101250101"; // 长沙
        };

        try {
            WeatherResponse resp = weatherClient.getNowWeather(locationId);
            if ("200".equals(resp.getCode()) && resp.getNow() != null) {
                WeatherResponse.Now n = resp.getNow();
                // 返回 JSON 字符串给 LLM
                return "{\"city\":\"" + city + "\",\"text\":\"" + n.getText()
                        + "\",\"temp\":" + n.getTemp() + ",\"feelsLike\":" + n.getFeelsLike()
                        + ",\"windDir\":\"" + n.getWindDir() + "\",\"windScale\":\"" + n.getWindScale()
                        + "级\",\"humidity\":\"" + n.getHumidity() + "%\"}";
            }
            return "{\"error\":\"天气查询失败，code=" + resp.getCode() + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"天气查询异常：" + e.getMessage() + "\"}";
        }
    }
}