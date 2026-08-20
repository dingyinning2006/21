package com.example.demo.tool;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
// 健康业务工具：负责 BMI 计算、体型分类和基础锻炼/饮食建议。
public class HealthToolService {

    private static final double UNDERWEIGHT_THRESHOLD = 18.5;
    private static final double NORMAL_THRESHOLD = 24.0;
    private static final double OVERWEIGHT_THRESHOLD = 28.0;

    public BmiResult calculateBmi(double heightCm, double weightKg) {
        // 先校验输入，避免身高为 0 导致除零或产生无效 BMI。
        if (heightCm <= 0) {
            throw new IllegalArgumentException("身高必须大于 0");
        }
        if (weightKg <= 0) {
            throw new IllegalArgumentException("体重必须大于 0");
        }

        double heightM = heightCm / 100.0;
        // BMI = 体重(kg) / 身高(m)的平方。
        double bmi = weightKg / (heightM * heightM);
        double roundedBmi = roundTwoDecimals(bmi);
        String category = classifyBmi(roundedBmi);
        String advice = switch (category) {
            case "偏瘦" -> "优先增加热量和蛋白质摄入，配合力量训练。";
            case "正常" -> "保持均衡饮食和规律运动。";
            case "超重" -> "适当控制总热量，增加有氧和力量训练。";
            case "肥胖" -> "先从可持续的饮食管理和低冲击运动开始。";
            default -> "建议结合自身情况制定健康计划。";
        };

        return new BmiResult(heightCm, weightKg, roundedBmi, category, advice);
    }

    public HealthPlanResult buildPlan(double bmi, String category, String goal) {
        // 这里使用上一步的 BMI 和分类生成确定性的建议，结果不会交给模型自行计算。
        String normalizedCategory = normalizeCategory(category);
        String normalizedGoal = normalizeGoal(goal);

        List<String> workout = switch (normalizedCategory) {
            case "偏瘦" -> List.of(
                    "每周 3 到 4 次力量训练，重点练腿、背、胸。",
                    "每次 40 到 60 分钟，控制强度，逐步增加重量。"
            );
            case "正常" -> List.of(
                    "每周 3 次力量训练加 2 次中等强度有氧。",
                    "保持每次 30 到 45 分钟，长期坚持。"
            );
            case "超重" -> List.of(
                    "每周 4 到 5 次快走、骑车或椭圆机。",
                    "搭配每周 2 到 3 次力量训练，提升基础代谢。"
            );
            case "肥胖" -> List.of(
                    "先从快走、游泳、骑车等低冲击运动开始。",
                    "每次 20 到 30 分钟，循序渐进增加时长。"
            );
            default -> List.of(
                    "每周保持 3 到 5 次规律运动。",
                    "结合有氧与力量训练，维持身体活力。"
            );
        };

        List<String> diet = switch (normalizedCategory) {
            case "偏瘦" -> List.of(
                    "每餐保证优质蛋白和主食摄入。",
                    "可以适当增加坚果、奶制品和高热量健康食物。"
            );
            case "正常" -> List.of(
                    "以高蛋白、适量主食和足量蔬菜为主。",
                    "少吃高糖零食，保持饮食稳定。"
            );
            case "超重" -> List.of(
                    "减少含糖饮料、油炸食品和夜宵。",
                    "主食适量，增加蛋白质和蔬菜比例。"
            );
            case "肥胖" -> List.of(
                    "优先控制总热量，少油少糖少酒。",
                    "多吃高纤维蔬菜和高蛋白食物，减少精制碳水。"
            );
            default -> List.of(
                    "三餐规律，优先保证蛋白质和蔬菜。",
                    "根据训练量微调碳水和总热量。"
            );
        };

        List<String> notes = List.of(
                "目标：" + normalizedGoal,
                "BMI：" + roundTwoDecimals(bmi),
                "建议每周至少保证 150 分钟中等强度活动。"
        );

        return new HealthPlanResult(
                roundTwoDecimals(bmi),
                normalizedCategory,
                normalizedGoal,
                workout,
                diet,
                notes
        );
    }

    private String classifyBmi(double bmi) {
        // 按当前项目设定的阈值把 BMI 映射成中文分类。
        if (bmi < UNDERWEIGHT_THRESHOLD) {
            return "偏瘦";
        }
        if (bmi < NORMAL_THRESHOLD) {
            return "正常";
        }
        if (bmi < OVERWEIGHT_THRESHOLD) {
            return "超重";
        }
        return "肥胖";
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "正常";
        }

        String value = category.trim();
        return switch (value) {
            case "偏瘦", "瘦" -> "偏瘦";
            case "正常", "标准" -> "正常";
            case "超重", "偏胖" -> "超重";
            case "肥胖", "重度肥胖" -> "肥胖";
            default -> "正常";
        };
    }

    private String normalizeGoal(String goal) {
        if (goal == null || goal.isBlank()) {
            return "保持健康";
        }

        String value = goal.trim();
        return value.toLowerCase(Locale.ROOT);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record BmiResult(
            double heightCm,
            double weightKg,
            double bmi,
            String category,
            String advice
    ) {
    }

    public record HealthPlanResult(
            double bmi,
            String category,
            String goal,
            List<String> workout,
            List<String> diet,
            List<String> notes
    ) {
    }
}
