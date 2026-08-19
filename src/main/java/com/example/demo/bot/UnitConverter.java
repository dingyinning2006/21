package com.example.demo.bot;

import java.util.List;
import java.util.Map;

/**
 * 单位换算器：支持长度、重量、温度、时间、容量五类，各类单位先折算到基准单位再换算。
 * 温度是线性偏移类换算（摄氏/华氏/开尔文），单独用公式处理。
 * 单位支持别名（公里=千米、公斤=千克）；单位不认识或跨类别时抛 IllegalArgumentException。
 */
public final class UnitConverter {

    /** 各类单位到基准单位的倍率（基准：米 / 千克 / 秒 / 升） */
    private static final Map<String, Double> LENGTH = Map.of(
            "米", 1.0, "千米", 1000.0, "公里", 1000.0, "厘米", 0.01, "毫米", 0.001,
            "英里", 1609.344, "英尺", 0.3048, "英寸", 0.0254);
    private static final Map<String, Double> WEIGHT = Map.of(
            "千克", 1.0, "公斤", 1.0, "克", 0.001, "吨", 1000.0, "斤", 0.5,
            "磅", 0.45359237, "盎司", 0.028349523125);
    private static final Map<String, Double> TIME = Map.of(
            "秒", 1.0, "分钟", 60.0, "小时", 3600.0, "天", 86400.0);
    private static final Map<String, Double> VOLUME = Map.of(
            "升", 1.0, "毫升", 0.001, "立方米", 1000.0);

    /** 温度单位（走公式，不走倍率） */
    private static final Map<String, String> TEMPERATURE = Map.of(
            "摄氏度", "C", "℃", "C", "华氏度", "F", "℉", "F", "开尔文", "K", "K", "K");

    /** 类别名 -> 单位表（倍率换算的表） */
    private static final List<Map.Entry<String, Map<String, Double>>> TABLES = List.of(
            Map.entry("长度", LENGTH), Map.entry("重量", WEIGHT),
            Map.entry("时间", TIME), Map.entry("容量", VOLUME));

    private UnitConverter() {
    }

    /** 把 value 从 fromUnit 换算成 toUnit；单位不认识或跨类别时抛异常 */
    public static double convert(double value, String fromUnit, String toUnit) {
        if (fromUnit == null || toUnit == null || fromUnit.isBlank() || toUnit.isBlank()) {
            throw new IllegalArgumentException("单位不能为空");
        }
        if (fromUnit.equals(toUnit)) {
            return value;
        }

        // 温度：先转到摄氏度，再转到目标
        if (TEMPERATURE.containsKey(fromUnit) || TEMPERATURE.containsKey(toUnit)) {
            String fromT = TEMPERATURE.get(fromUnit);
            String toT = TEMPERATURE.get(toUnit);
            if (fromT == null || toT == null) {
                throw new IllegalArgumentException("不支持的单位：" + fromUnit + "/" + toUnit + "（温度只能与温度互相换算）");
            }
            return fromCelsius(toT, toCelsius(value, fromT));
        }

        // 其余类别：fromUnit 和 toUnit 必须在同一类别里（仅靠倍率无法发现跨类别）
        UnitRate from = rateOf(fromUnit);
        UnitRate to = rateOf(toUnit);
        if (!from.category().equals(to.category())) {
            throw new IllegalArgumentException("不能跨类别换算：" + fromUnit + "（" + from.category()
                    + "）和 " + toUnit + "（" + to.category() + "）");
        }
        return value * from.rate() / to.rate();
    }

    /** 查单位所属类别和倍率 */
    private static UnitRate rateOf(String unit) {
        for (Map.Entry<String, Map<String, Double>> table : TABLES) {
            Double rate = table.getValue().get(unit);
            if (rate != null) {
                return new UnitRate(table.getKey(), rate);
            }
        }
        throw new IllegalArgumentException("不认识这个单位：" + unit + "（支持长度/重量/温度/时间/容量）");
    }

    /** 单位在倍率表里的身份：所属类别 + 到基准单位的倍率 */
    private record UnitRate(String category, double rate) {
    }

    /** 任意温标 -> 摄氏度 */
    private static double toCelsius(double v, String unit) {
        return switch (unit) {
            case "C" -> v;
            case "F" -> (v - 32) * 5 / 9;
            case "K" -> v - 273.15;
            default -> throw new IllegalArgumentException("未知温标：" + unit);
        };
    }

    /** 摄氏度 -> 任意温标 */
    private static double fromCelsius(String unit, double celsius) {
        return switch (unit) {
            case "C" -> celsius;
            case "F" -> celsius * 9 / 5 + 32;
            case "K" -> celsius + 273.15;
            default -> throw new IllegalArgumentException("未知温标：" + unit);
        };
    }
}
