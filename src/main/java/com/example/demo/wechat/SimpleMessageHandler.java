package com.example.demo.wechat;

import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;


import java.util.List;
import com.example.demo.weather.WeatherClient;
import com.example.demo.weather.WeatherResponse;

public class SimpleMessageHandler implements MessageHandler {

    private final WeChatClient client;

    private final WeatherClient weatherClient = new WeatherClient();   // 新增

    public SimpleMessageHandler(WeChatClient client) {  // 加参数
        this.client = client;

    }



    @Override
    public void handle(List<WeixinMessage> messages) {
        for (WeixinMessage msg : messages) {
            String fromUserId = msg.getFrom_user_id();
            System.out.println("收到消息 from = " + fromUserId);

            if (msg.getItem_list() == null) continue;

            for (MessageItem item : msg.getItem_list()) {
                System.out.println("item = " + item);

                if (item.getText_item() != null)  {
                    String text = item.getText_item().getText();
                    System.out.println("文本内容: " + text);
                    try {
                        // ===== 新增：天气查询判断 =====
                        if (text.contains("天气")) {
                            String reply = handleWeatherQuery(text);
                            client.sendText(fromUserId, reply);
                        } else {
                            // 原来的逻辑不变
                            client.sendText(fromUserId, "收到你的消息: " + text);
                        }
                    }catch (java.io.IOException e){
                        e.printStackTrace();}
                }
            }
        }
    }
    private String handleWeatherQuery(String text) {
        // 1. 从文本中提取城市名（简单关键词匹配）
        String city = "长沙"; // 默认长沙
        if (text.contains("北京")) city = "北京";
        else if (text.contains("上海")) city = "上海";
        else if (text.contains("南京")) city = "南京";
        else if (text.contains("广州")) city = "广州";
        else if (text.contains("深圳")) city = "深圳";
        else if (text.contains("杭州")) city = "杭州";
        else if (text.contains("成都")) city = "成都";

        // 2. 城市名 → LocationID
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

        // 3. 调天气 API
        try {
            WeatherResponse resp = weatherClient.getNowWeather(locationId);
            if ("200".equals(resp.getCode()) && resp.getNow() != null) {
                WeatherResponse.Now n = resp.getNow();
                return city + "天气：" + n.getText()
                        + "，" + n.getTemp() + "°C（体感" + n.getFeelsLike() + "°C），"
                        + n.getWindDir() + n.getWindScale() + "级，湿度" + n.getHumidity() + "%";
            } else {
                return "天气查询失败，code=" + resp.getCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "天气查询异常：" + e.getMessage();
        }
    }
}
