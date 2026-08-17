package com.example.demo.wechat;

import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import java.util.List;

public interface MessageHandler {
    void handle(List<WeixinMessage> messages);
}
