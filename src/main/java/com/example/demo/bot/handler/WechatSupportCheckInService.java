package com.example.demo.bot.handler;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.SafetyLevel;
import com.example.demo.agent.contract.SupportPhase;
import com.example.demo.agent.contract.UserProfile;
import com.example.demo.storage.SupportStateStore;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 微信文字入口的最小 M4 适配：创建档案、保存打卡、查询近 7 天记录。 */
@Component
public class WechatSupportCheckInService {

    private static final Pattern FIELD = Pattern.compile("(压力|睡眠|心情|完成率)\\s*[=:：]\\s*([^\\s]+)");

    private final SupportStateStore store;

    public WechatSupportCheckInService(SupportStateStore store) {
        this.store = store;
    }

    public Optional<String> handle(String userId, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        String text = rawText.trim();
        String command = text.startsWith("/") ? text.substring(1).trim() : text;
        if (command.equals("开始打卡") || command.equals("打卡帮助")) {
            ensureProfile(userId);
            return Optional.of(help());
        }
        if (command.equals("查看打卡") || command.equals("我的打卡")) {
            return Optional.of(history(userId));
        }
        if (!command.equals("打卡") && !command.startsWith("打卡 ")) {
            return Optional.empty();
        }
        if (command.equals("打卡")) {
            ensureProfile(userId);
            return Optional.of(help());
        }

        return Optional.of(saveCheckIn(userId, command.substring(2).trim()));
    }

    private String saveCheckIn(String userId, String fieldsText) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = FIELD.matcher(fieldsText);
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }

        List<String> missing = new ArrayList<>();
        for (String required : List.of("压力", "睡眠", "心情", "完成率")) {
            if (!fields.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            return "缺少：" + String.join("、", missing) + "。示例：打卡 压力=6 睡眠=7 心情=6 完成率=0.5 备注=完成了最小任务";
        }

        try {
            int stress = Integer.parseInt(fields.get("压力"));
            double sleep = Double.parseDouble(fields.get("睡眠"));
            int mood = Integer.parseInt(fields.get("心情"));
            double completion = Double.parseDouble(fields.get("完成率"));
            String note = extractNote(fieldsText);
            ensureProfile(userId);
            store.saveCheckIn(new CheckInRecord(
                    userId,
                    LocalDate.now(),
                    stress,
                    sleep,
                    mood,
                    completion,
                    List.of(),
                    List.of(),
                    note,
                    SupportPhase.CHECK_IN
            ));
            return "今日打卡已保存：压力 " + stress + "/10，睡眠 " + sleep + " 小时，心情 " + mood
                    + "/10，任务完成率 " + completion + "。可发送“查看打卡”读取近 7 天记录。";
        } catch (IllegalArgumentException exception) {
            return "打卡数据格式不正确。压力/心情填 0-10，睡眠填 0-24，完成率填 0-1。";
        }
    }

    private String history(String userId) {
        List<CheckInRecord> records = store.findCheckIns(userId, LocalDate.now().minusDays(6), LocalDate.now());
        if (records.isEmpty()) {
            return "还没有打卡记录。发送“开始打卡”查看格式。";
        }
        StringBuilder reply = new StringBuilder("最近 7 天打卡：");
        for (CheckInRecord record : records) {
            reply.append("\\n").append(record.date())
                    .append(" 压力 ").append(record.stressLevel()).append("/10")
                    .append(" 睡眠 ").append(record.sleepHours()).append(" 小时")
                    .append(" 完成率 ").append(record.completionRate());
        }
        return reply.toString();
    }

    private void ensureProfile(String userId) {
        if (store.findUserProfile(userId).isEmpty()) {
            store.saveUserProfile(new UserProfile(
                    userId,
                    userId,
                    List.of(),
                    LocalDate.now(),
                    SupportPhase.CHECK_IN,
                    SafetyLevel.NORMAL
            ));
        }
    }

    private static String extractNote(String text) {
        int index = text.indexOf("备注");
        if (index < 0) {
            return null;
        }
        String note = text.substring(index + 2).replaceFirst("^[=:：]\\s*", "").trim();
        return note.isBlank() ? null : note;
    }

    private static String help() {
        return "发送以下格式完成今日打卡：\n"
                + "打卡 压力=6 睡眠=7 心情=6 完成率=0.5 备注=完成了最小任务\n"
                + "范围：压力/心情 0-10，睡眠 0-24 小时，完成率 0-1。\n"
                + "发送“查看打卡”读取近 7 天记录。";
    }
}
