package com.example.demo.tool;

import org.springframework.stereotype.Service;

@Service
// 纯业务工具：只负责按照标准单位缩写进行换算，不参与大模型通信。
public class UnitConverterService {

    /**
     * 单位换算
     *
     * @param value 数值
     * @param from 原单位
     * @param to   目标单位
     * @return 换算后的数值
     */
    public double convert(
            double value,
            String from,
            String to
    ) {

        // QwenService 已经把中文单位归一化为标准缩写，这里统一转小写后匹配。
        from = from.toLowerCase();
        to = to.toLowerCase();

        // =========================
        // 长度
        // =========================

        if (from.equals("km") && to.equals("m")) {
            return value * 1000;
        }

        if (from.equals("m") && to.equals("km")) {
            return value / 1000;
        }

        if (from.equals("m") && to.equals("cm")) {
            return value * 100;
        }

        if (from.equals("cm") && to.equals("m")) {
            return value / 100;
        }

        // =========================
        // 重量
        // =========================

        if (from.equals("kg") && to.equals("g")) {
            return value * 1000;
        }

        if (from.equals("g") && to.equals("kg")) {
            return value / 1000;
        }

        // =========================
        // 时间
        // =========================

        if (from.equals("h") && to.equals("min")) {
            return value * 60;
        }

        if (from.equals("min") && to.equals("h")) {
            return value / 60;
        }

        if (from.equals("min") && to.equals("s")) {
            return value * 60;
        }

        if (from.equals("s") && to.equals("min")) {
            return value / 60;
        }

        throw new IllegalArgumentException(
                "暂不支持：" + from + " → " + to
        );
    }
}
