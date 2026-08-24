package com.example.demo.skill;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 每日一句技能
 * 命中"每日一句"、"名言"、"鸡汤"等关键词时，随机返回一句励志名言
 * 不需要走LLM，本地随机选择，响应快
 */
@Component
public class DailyQuoteSkill implements Skill {

    private final Random random = new Random();

    private static final List<String> QUOTES = List.of(
            "千里之行，始于足下。—— 老子",
            "天行健，君子以自强不息。—— 《周易》",
            "不积跬步，无以至千里；不积小流，无以成江海。—— 荀子",
            "业精于勤，荒于嬉；行成于思，毁于随。—— 韩愈",
            "宝剑锋从磨砺出，梅花香自苦寒来。—— 《警世贤文》",
            "路漫漫其修远兮，吾将上下而求索。—— 屈原",
            "会当凌绝顶，一览众山小。—— 杜甫",
            "长风破浪会有时，直挂云帆济沧海。—— 李白",
            "穷则独善其身，达则兼济天下。—— 孟子",
            "知之为知之，不知为不知，是知也。—— 孔子",
            "三人行，必有我师焉。—— 孔子",
            "生于忧患，死于安乐。—— 孟子",
            "老骥伏枥，志在千里；烈士暮年，壮心不已。—— 曹操",
            "非淡泊无以明志，非宁静无以致远。—— 诸葛亮",
            "鞠躬尽瘁，死而后已。—— 诸葛亮",
            "采菊东篱下，悠然见南山。—— 陶渊明",
            "海内存知己，天涯若比邻。—— 王勃",
            "大漠孤烟直，长河落日圆。—— 王维",
            "忽如一夜春风来，千树万树梨花开。—— 岑参",
            "天生我材必有用，千金散尽还复来。—— 李白"
    );

    @Override
    public String getName() {
        return "daily_quote";
    }

    @Override
    public String getDescription() {
        return "每日一句励志名言";
    }

    @Override
    public List<String> getKeywords() {
        return List.of("每日一句", "名言", "鸡汤", "励志", "语录", "quote", "鼓励");
    }

    @Override
    public String execute(String userMessage) {
        String quote = QUOTES.get(random.nextInt(QUOTES.size()));
        return "💡 今日名言\n\n" + quote + "\n\n愿这句话给你带来力量！✨";
    }
}
