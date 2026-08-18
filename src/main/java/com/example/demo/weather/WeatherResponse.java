package com.example.demo.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private String code;
    private Now now;
    private List<Daily> daily;

    // ===== getter / setter =====
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Now getNow() { return now; }
    public void setNow(Now now) { this.now = now; }
    public List<Daily> getDaily() { return daily; }
    public void setDaily(List<Daily> daily) { this.daily = daily; }

    // ===== 内部类：实时天气 =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Now {
        @JsonProperty("obsTime")
        private String obsTime;
        private String temp;
        @JsonProperty("feelsLike")
        private String feelsLike;
        private String text;
        @JsonProperty("windDir")
        private String windDir;
        @JsonProperty("windScale")
        private String windScale;
        private String humidity;
        private String vis;

        public String getObsTime() { return obsTime; }
        public void setObsTime(String obsTime) { this.obsTime = obsTime; }
        public String getTemp() { return temp; }
        public void setTemp(String temp) { this.temp = temp; }
        public String getFeelsLike() { return feelsLike; }
        public void setFeelsLike(String feelsLike) { this.feelsLike = feelsLike; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getWindDir() { return windDir; }
        public void setWindDir(String windDir) { this.windDir = windDir; }
        public String getWindScale() { return windScale; }
        public void setWindScale(String windScale) { this.windScale = windScale; }
        public String getHumidity() { return humidity; }
        public void setHumidity(String humidity) { this.humidity = humidity; }
        public String getVis() { return vis; }
        public void setVis(String vis) { this.vis = vis; }
    }

    // ===== 内部类：逐天预报 =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Daily {
        @JsonProperty("fxDate")
        private String fxDate;
        @JsonProperty("tempMax")
        private String tempMax;
        @JsonProperty("tempMin")
        private String tempMin;
        @JsonProperty("textDay")
        private String textDay;
        @JsonProperty("textNight")
        private String textNight;
        private String humidity;
        @JsonProperty("uvIndex")
        private String uvIndex;

        public String getFxDate() { return fxDate; }
        public void setFxDate(String fxDate) { this.fxDate = fxDate; }
        public String getTempMax() { return tempMax; }
        public void setTempMax(String tempMax) { this.tempMax = tempMax; }
        public String getTempMin() { return tempMin; }
        public void setTempMin(String tempMin) { this.tempMin = tempMin; }
        public String getTextDay() { return textDay; }
        public void setTextDay(String textDay) { this.textDay = textDay; }
        public String getTextNight() { return textNight; }
        public void setTextNight(String textNight) { this.textNight = textNight; }
        public String getHumidity() { return humidity; }
        public void setHumidity(String humidity) { this.humidity = humidity; }
        public String getUvIndex() { return uvIndex; }
        public void setUvIndex(String uvIndex) { this.uvIndex = uvIndex; }
    }
}