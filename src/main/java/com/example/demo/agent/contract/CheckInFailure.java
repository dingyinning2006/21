package com.example.demo.agent.contract;

import java.time.LocalDate;

/** 打卡失败的可重试结果，避免语音或消息异常丢失当前会话。 */
public record CheckInFailure(
        String userId,
        LocalDate date,
        CheckInFailureCode code,
        String userMessage,
        boolean retryable
) {

    public CheckInFailure {
        userId = SupportContractChecks.requireNonBlank(userId, "userId");
        date = SupportContractChecks.requireNonNull(date, "date");
        code = SupportContractChecks.requireNonNull(code, "code");
        userMessage = SupportContractChecks.requireNonBlank(userMessage, "userMessage");
    }
}
