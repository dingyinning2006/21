package com.example.demo.wechat;

import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;


import java.util.List;

public class SimpleMessageHandler implements MessageHandler {

    private final WeChatClient client;

    public SimpleMessageHandler(WeChatClient client) {
        this.client = client;
    }

    @Override
    public void handle(List<WeixinMessage> messages) {
        for (WeixinMessage msg : messages) {
            String fromUserId = msg.getFrom_user_id();
            System.out.println("收到消息 from = " + fromUserId);

            if (msg.getItem_list() == null) continue;

            for (MessageItem item : msg.getItem_list()) {
                System.out.println("item = " + item);

                if (item.getText_item() != null)  {
                    String text = item.getText_item().getText();
                    System.out.println("文本内容: " + text);
                    try{
                  client.sendText(fromUserId, "收到你的消息: " + text);}
                    catch (java.io.IOException e){
                        e.printStackTrace();}
                    }
            }
        }
    }
}
