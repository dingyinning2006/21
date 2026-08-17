package com.example.demo.util;

import java.io.InputStream;
import java.util.Properties;

public class ConfigUtil {
    private static Properties prop = new Properties();

    static {
        try (InputStream is = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("找不到config.properties，请复制config‑template.properties重命名");
            }
            prop.load(is);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String get(String key) {
        return prop.getProperty(key).trim();
    }
}