package com.example.demo.intent;

// 意图识别结果：replyType 决定回复方式，userQuestion 保存真正要处理的问题。
public class IntentResult {

    private String replyType;
    private String userQuestion;

    public String getReplyType() {
        return replyType;
    }

    public void setReplyType(String replyType) {
        this.replyType = replyType;
    }

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }
}
