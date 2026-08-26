package com.example.demo.agent.contract;

/** 打卡输入或消息发送失败的稳定错误码，供微信模块和降级提示共同使用。 */
public enum CheckInFailureCode {
    VOICE_TRANSCRIPTION_FAILED,
    REQUIRED_FIELD_MISSING,
    MESSAGE_DELIVERY_FAILED
}
