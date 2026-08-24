package com.example.demo.tool;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 穿衣建议工具
 * 输入：温度(temp)、天气状况(text)
 * 输出：穿衣建议
 *
 * 这个工具通常与 get_weather 链式配合使用：
 * 第一步：get_weather(city) → 得到 temp 和 text
 * 第二步：clothing_advice(temp, text) → 得到穿衣建议
 */
@Component
public class ClothingAdviceTool implements Tool {

    @Override
    public String getName() {
        return "clothing_advice";
    }

    @Override
    public String getDescription() {
        return "根据温度和天气状况给出穿衣建议。当用户询问穿什么衣服、怎么穿搭、是否需要带伞、冷不冷时调用此工具。" +
                "注意：此工具需要温度和天气状况作为输入，通常需要先调用 get_weather 获取天气信息后再调用本工具。";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> properties = new LinkedHashMap<>();

        // temp 参数：温度（摄氏度）
        Map<String, Object> temp = new LinkedHashMap<>();
        temp.put("type", "number");
        temp.put("description", "当前温度，单位摄氏度，例如：28、15、5");
        temp.put("minimum", -50);
        temp.put("maximum", 60);
        properties.put("temp", temp);

        // text 参数：天气状况
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("type", "string");
        text.put("description", "天气状况，例如：晴、多云、阴、小雨、中雨、大雨、雷阵雨、雪");
        properties.put("text", text);

        // windScale 参数：风力（可选）
        Map<String, Object> windScale = new LinkedHashMap<>();
        windScale.put("type", "string");
        windScale.put("description", "风力等级，例如：3级、5级");
        properties.put("windScale", windScale);

        return properties;
    }

    @Override
    public List<String> getRequired() {
        // temp 和 text 是必填的，windScale 可选
        return Arrays.asList("temp", "text");
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        double temp = ((Number) arguments.getOrDefault("temp", 25)).doubleValue();
        String text = (String) arguments.getOrDefault("text", "晴");
        String windScale = (String) arguments.getOrDefault("windScale", "");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("temp", temp);
        result.put("weather", text);

        try {
            // 1. 根据温度给出基础穿衣建议
            String baseAdvice = getAdviceByTemp(temp);

            // 2. 根据天气状况给出附加建议
            String weatherAdvice = getAdviceByWeather(text);

            // 3. 根据风力给出附加建议
            String windAdvice = getAdviceByWind(windScale);

            // 4. 综合建议
            StringBuilder fullAdvice = new StringBuilder(baseAdvice);
            if (weatherAdvice != null && !weatherAdvice.isEmpty()) {
                fullAdvice.append("；").append(weatherAdvice);
            }
            if (windAdvice != null && !windAdvice.isEmpty()) {
                fullAdvice.append("；").append(windAdvice);
            }
            fullAdvice.append("。");

            result.put("advice", fullAdvice.toString());
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "生成穿衣建议失败：" + e.getMessage());
        }

        return toJson(result);
    }

    /**
     * 根据温度区间给出基础穿衣建议
     */
    private String getAdviceByTemp(double temp) {
        if (temp >= 35) {
            return "天气酷热，建议穿短袖、短裤或短裙，尽量选择透气吸汗的面料，避免长时间户外活动，注意防暑降温，多补充水分";
        } else if (temp >= 30) {
            return "天气炎热，建议穿短袖、短裤或裙装，选择轻薄透气的衣物，注意防晒，出门可戴帽子或打伞";
        } else if (temp >= 25) {
            return "天气温暖，建议穿短袖T恤或薄款长袖，搭配长裤或半身裙，体感舒适，适合户外活动";
        } else if (temp >= 20) {
            return "天气舒适，建议穿长袖衬衫或薄款卫衣，搭配长裤，早晚可能微凉，可备一件薄外套";
        } else if (temp >= 15) {
            return "天气微凉，建议穿薄外套、卫衣或风衣，内搭长袖，下身穿长裤，注意早晚保暖";
        } else if (temp >= 10) {
            return "天气较冷，建议穿厚外套、毛衣或夹克，内搭保暖衣物，下身穿厚长裤，可搭配围巾";
        } else if (temp >= 5) {
            return "天气寒冷，建议穿羽绒服或棉服，内搭毛衣和保暖内衣，下身穿加绒长裤，注意防寒保暖";
        } else if (temp >= 0) {
            return "天气严寒，建议穿加厚羽绒服，内搭毛衣和保暖内衣，佩戴帽子、围巾、手套，尽量减少户外活动时间";
        } else {
            return "天气极度寒冷，建议穿最厚的防寒装备，多层穿搭，佩戴帽子、围巾、手套、口罩，注意防冻伤，非必要不外出";
        }
    }

    /**
     * 根据天气状况给出附加建议
     */
    private String getAdviceByWeather(String text) {
        if (text == null) return "";
        String t = text.toLowerCase();

        if (t.contains("雨") || t.contains("rain")) {
            if (t.contains("大") || t.contains("暴")) {
                return "有大雨，务必携带雨伞，建议穿防水鞋，避免在低洼处行走，注意交通安全";
            } else if (t.contains("雷") || t.contains("阵")) {
                return "有雷阵雨，建议携带雨伞，避免在空旷地带或大树下停留，注意防雷";
            } else {
                return "有降雨，建议携带雨伞，穿防滑鞋，路面湿滑注意安全";
            }
        } else if (t.contains("雪") || t.contains("snow")) {
            return "有降雪，建议穿防滑保暖的鞋子，注意路面结冰，出行注意安全，可佩戴帽子和手套";
        } else if (t.contains("雾") || t.contains("霾") || t.contains("fog") || t.contains("haze")) {
            return "有雾霾，建议佩戴口罩，减少户外活动时间，注意交通安全";
        } else if (t.contains("晴") || t.contains("sunny")) {
            return "天气晴朗，紫外线较强，注意防晒，可涂抹防晒霜或佩戴墨镜";
        } else if (t.contains("多云") || t.contains("cloud")) {
            return "多云天气，体感舒适，适合户外活动";
        } else if (t.contains("阴") || t.contains("overcast")) {
            return "阴天，气温可能偏低，可适当增添衣物";
        } else if (t.contains("风") || t.contains("wind")) {
            return "风力较大，建议穿防风外套，注意防风保暖";
        }
        return "";
    }

    /**
     * 根据风力给出附加建议
     */
    private String getAdviceByWind(String windScale) {
        if (windScale == null || windScale.isEmpty()) return "";
        try {
            // 提取数字
            String numStr = windScale.replaceAll("[^0-9]", "");
            if (numStr.isEmpty()) return "";
            int level = Integer.parseInt(numStr);

            if (level >= 6) {
                return "风力达到" + level + "级，属于强风，建议避免在广告牌、大树下停留，注意高空坠物";
            } else if (level >= 4) {
                return "风力" + level + "级，建议穿防风衣物，注意保暖";
            }
        } catch (NumberFormatException ignored) {
        }
        return "";
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(((String) val).replace("\"", "\\\"")).append("\"");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else {
                sb.append("\"").append(val).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
