package com.example.demo.wechat;


import com.github.wechat.ilink.sdk.core.model.WeixinMessage;


import java.util.List;

public interface MessageHandler {
    // 注入

    // 然后把 reply 通过微信发回去
    void handle(List<WeixinMessage> messages);
}
