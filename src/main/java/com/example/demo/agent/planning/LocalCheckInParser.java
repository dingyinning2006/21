package com.example.demo.agent.planning;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.SupportPhase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 首版文字打卡解析器。
 *
 * 微信文字和语音转写最终都进入这里，只有四项数据齐全时才落成正式打卡记录，
 * 避免用模型猜测用户的压力或睡眠数值。
 */
public class LocalCheckInParser {

    private static final Pattern STRESS = Pattern.compile("(?:压力|焦虑程度|焦虑)[^0-9]{0,8}(10|[0-9])(?![0-9])");
    private static final Pattern SLEEP = Pattern.compile("(?:睡眠|睡了|睡)[^0-9]{0,8}([0-9]+(?:\\.[0-9]+)?)\\s*(?:个?小时|h|H)");
    private static final Pattern MOOD = Pattern.compile("(?:心情|情绪)[^0-9]{0,8}(10|[0-9])(?![0-9])");
    private static final Pattern COMPLETION_PERCENT = Pattern.compile("(?:完成率|完成度)[^0-9]{0,8}([0-9]+(?:\\.[0-9]+)?)\\s*%?");
    private static final Pattern COMPLETION_FRACTION = Pattern.compile("完成(?:了)?[^0-9]{0,4}([0-9]+)\\s*/\\s*([0-9]+)");

    public Optional<CheckInRecord> parse(String userId, LocalDate date, String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        Integer stress = findInt(STRESS, message);
        Double sleep = findDouble(SLEEP, message);
        Integer mood = findInt(MOOD, message);
        Double completion = findCompletion(message);

        if (stress == null || sleep == null || mood == null || completion == null
                || stress < 0 || stress > 10
                || sleep < 0 || sleep > 24
                || mood < 0 || mood > 10
                || !Double.isFinite(completion) || completion < 0 || completion > 1) {
            return Optional.empty();
        }

        return Optional.of(new CheckInRecord(
                userId,
                date,
                stress,
                sleep,
                mood,
                completion,
                List.of(),
                List.of(),
                message,
                SupportPhase.CHECK_IN
        ));
    }

    public boolean looksLikeCheckIn(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        return message.contains("打卡")
                || message.contains("今日反馈")
                || message.contains("记录今天")
                || (message.contains("压力") && message.contains("睡"));
    }

    private Integer findInt(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private Double findDouble(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
    }

    private Double findCompletion(String message) {
        Matcher percentMatcher = COMPLETION_PERCENT.matcher(message);
        if (percentMatcher.find()) {
            double percent = Double.parseDouble(percentMatcher.group(1));
            return percent > 1 ? percent / 100 : percent;
        }

        Matcher fractionMatcher = COMPLETION_FRACTION.matcher(message);
        if (fractionMatcher.find()) {
            double completed = Double.parseDouble(fractionMatcher.group(1));
            double total = Double.parseDouble(fractionMatcher.group(2));
            return total == 0 ? null : completed / total;
        }

        if (message.contains("完成全部") || message.contains("全部完成") || message.contains("都完成")) {
            return 1D;
        }
        if (message.contains("都没完成") || message.contains("没有完成任务")) {
            return 0D;
        }
        return null;
    }
}
