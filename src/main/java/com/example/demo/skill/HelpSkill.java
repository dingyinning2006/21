package com.example.demo.skill;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 帮助技能
 * 命中"帮助"、"功能"、"你会什么"等关键词时，直接返回机器人功能列表
 * 不需要走LLM，响应快且内容确定
 */
@Component
public class HelpSkill implements Skill {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "查看机器人功能列表和使用帮助";
    }

    @Override
    public List<String> getKeywords() {
        return List.of("帮助", "功能", "你会什么", "能做什么", "使用说明", "怎么用", "help", "命令");
    }

    @Override
    public String execute(String userMessage) {
        return """
                🤖 我是你的微信AI助手，支持以下功能：

                📝 智能对话
                直接发文字，我会用AI回复你

                🎨 图片生成
                发送"画xxx"，例如"画一只可爱的猫咪"

                🎙️ 语音回复
                发送"说xxx"，例如"说你好世界"，会回复语音文件

                🌤️ 天气查询
                发送"南京天气"、"北京气温"等，查询实时天气

                🔧 单位换算
                直接问"5公里等于多少英里"、"100斤是多少公斤"

                💪 健康计算
                发送"BMI 身高170 体重60"计算BMI和健康建议

                📚 知识问答
                问我项目相关的知识，我会从知识库检索后回答

                💡 每日一句
                发送"每日一句"、"名言"、"鸡汤"获取励志语录

                有什么想试的直接发消息就行！""";
    }
}
