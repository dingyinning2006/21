package com.example.demo;

import com.example.demo.intent.IntentRecognizer;
import com.example.demo.weather.WeatherClient;
import com.example.demo.weather.WeatherResponse;
import com.example.demo.wechat.WeChatClient;

public class LoginTest {
    public static void main(String[] args) throws Exception {
        // 随便找个 main 方法或测试类跑一下
        IntentRecognizer recognizer = new IntentRecognizer();
        System.out.println(recognizer.recognize("今天多少度"));     // WEATHER
        System.out.println(recognizer.recognize("明天会下雨吗"));    // WEATHER
        System.out.println(recognizer.recognize("你能做什么"));      // HELP
        System.out.println(recognizer.recognize("你好"));            // CHAT
        System.out.println(recognizer.extractCity("南京天气怎么样")); // 南京
    }
}
