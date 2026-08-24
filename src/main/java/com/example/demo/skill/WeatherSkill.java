package com.example.demo.skill;

import com.example.demo.weather.WeatherClient;
import com.example.demo.weather.WeatherResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 天气查询 Skill：关键词命中后直接查询天气并返回
 * 不经过 LLM，确定性执行
 */
@Component
public class WeatherSkill implements Skill {

    private final WeatherClient weatherClient = new WeatherClient();

    // 触发关键词
    private static final List<String> KEYWORDS = Arrays.asList(
            "天气", "气温", "温度", "多少度", "几度",
            "下雨", "雨天", "晴天", "阴天", "多云",
            "刮风", "大风", "下雪", "雪天", "天气预报"
    );

    // 已知城市列表（用于从句子中提取城市）
    private static final Set<String> KNOWN_CITIES = Set.of(
            "北京", "上海", "广州", "深圳", "杭州", "南京",
            "成都", "武汉", "长沙", "重庆", "天津", "西安",
            "苏州", "青岛", "大连", "厦门", "宁波", "合肥",
            "福州", "济南", "郑州", "沈阳", "哈尔滨", "长春",
            "石家庄", "太原", "南昌", "南宁", "昆明", "贵阳"
    );

    @Override
    public List<String> getKeywords() {
        return KEYWORDS;
    }

    @Override
    public String execute(String userMessage) {
        // 1. 从用户消息中提取城市，提取不到默认长沙
        String city = extractCity(userMessage);
        if (city == null) {
            city = "长沙";
        }

        // 2. 城市名 → locationId
        String locationId = cityToLocationId(city);

        // 3. 调用天气 API
        try {
            WeatherResponse resp = weatherClient.getNowWeather(locationId);
            if ("200".equals(resp.getCode()) && resp.getNow() != null) {
                WeatherResponse.Now n = resp.getNow();
                return city + "天气：" + n.getText()
                        + "，温度 " + n.getTemp() + "℃"
                        + "，体感 " + n.getFeelsLike() + "℃"
                        + "，" + n.getWindDir() + n.getWindScale() + "级"
                        + "，湿度 " + n.getHumidity() + "%";
            }
            return "天气查询失败，请稍后再试。";
        } catch (Exception e) {
            return "天气查询异常：" + e.getMessage();
        }
    }

    /**
     * 从文本中提取城市名
     */
    private String extractCity(String text) {
        if (text == null) return null;
        for (String city : KNOWN_CITIES) {
            if (text.contains(city)) {
                return city;
            }
        }
        return null;
    }

    /**
     * 城市名转 locationId
     */
    private String cityToLocationId(String city) {
        return switch (city) {
            case "北京" -> "101010100";
            case "上海" -> "101020100";
            case "南京" -> "101190101";
            case "广州" -> "101280101";
            case "深圳" -> "101280601";
            case "杭州" -> "101210101";
            case "成都" -> "101270101";
            case "武汉" -> "101200101";
            case "重庆" -> "101040100";
            case "天津" -> "101030100";
            case "西安" -> "101110101";
            default -> "101250101"; // 长沙
        };
    }
}
