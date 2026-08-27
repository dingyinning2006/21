package com.example.demo.skill;

public interface Keyword extends Skill {

    boolean matches(String userText);

    String executeText(String userText);
}
