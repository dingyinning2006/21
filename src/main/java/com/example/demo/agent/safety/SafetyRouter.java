package com.example.demo.agent.safety;

import com.example.demo.agent.contract.SafetyDecision;
import com.example.demo.agent.contract.SafetyLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责高危信号的第一层安全路由。
 *
 * 这里只做可审计的信号识别，不做精神疾病诊断，也不替代人工风险评估。
 */
@Service
public class SafetyRouter {

    private final Set<String> activeSafetyFlows = ConcurrentHashMap.newKeySet();

    /** 由部署环境提供，不在代码或模型中写死热线号码。 */
    @Value("${support.safety.crisis-hotline:}")
    private String configuredCrisisHotline;

    private static final List<String> URGENT_SIGNALS = List.of(
            "想自杀",
            "要自杀",
            "自杀计划",
            "结束生命",
            "不想活了",
            "不想活",
            "活着没意义",
            "伤害自己",
            "自伤",
            "割腕",
            "跳楼",
            "准备去死"
    );

    /**
     * 根据用户当前消息生成安全决策。
     * 不记录或返回用户原始高危表达，避免敏感原文在日志和下游模块中扩散。
     */
    public SafetyDecision evaluate(String userId, String userMessage) {
        if (activeSafetyFlows.contains(userId)) {
            return urgentDecision(userId, "用户已处于安全流程，继续保持安全引导");
        }

        List<String> matchedSignals = findUrgentSignals(userMessage);

        if (!matchedSignals.isEmpty()) {
            activeSafetyFlows.add(userId);
            return urgentDecision(userId, "命中高危表达信号数量：" + matchedSignals.size());
        }

        return new SafetyDecision(
                userId,
                SafetyLevel.NORMAL,
                false,
                List.of(),
                "我会继续陪你了解目前的压力和状态。",
                "当前消息未命中固定高危表达信号"
        );
    }

    /** 仅供人工确认安全后使用，普通聊天不能自行清除安全流程状态。 */
    public void clearSafetyFlow(String userId) {
        if (userId != null && !userId.isBlank()) {
            activeSafetyFlows.remove(userId);
        }
    }

    private SafetyDecision urgentDecision(String userId, String rationale) {
        return new SafetyDecision(
                userId,
                SafetyLevel.URGENT,
                true,
                safetyActions(),
                "我很重视你现在的安全。请先停止普通任务，立即联系身边可信任的人，并寻求当地急救或危机支持。",
                rationale
        );
    }

    private List<String> safetyActions() {
        List<String> actions = new ArrayList<>();
        actions.add("请立即联系身边可信任的人，并尽量不要独处");

        if (configuredCrisisHotline == null || configuredCrisisHotline.isBlank()) {
            actions.add("当地急救或危机热线尚未配置，请由人工补充当地可靠号码");
        } else {
            actions.add("请联系当地急救或危机热线：" + configuredCrisisHotline.trim());
        }

        actions.add("请尽快联系专业心理咨询师、学校心理中心或医院");
        return List.copyOf(actions);
    }

    private List<String> findUrgentSignals(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }

        List<String> matchedSignals = new ArrayList<>();
        for (String signal : URGENT_SIGNALS) {
            if (userMessage.contains(signal)) {
                matchedSignals.add(signal);
            }
        }
        return matchedSignals;
    }
}
