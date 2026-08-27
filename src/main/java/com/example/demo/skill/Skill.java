package com.example.demo.skill;

import java.util.List;
import java.util.Map;

public interface Skill {

    String getName();

    String getDescription();

    Map<String, Object> getParameters();

    List<String> getRequiredParameters();

    String execute(String argumentsJson);
}
