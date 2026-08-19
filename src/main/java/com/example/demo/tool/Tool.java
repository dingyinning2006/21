package com.example.demo.tool;

import java.util.List;
import java.util.Map;

public interface Tool {
    String getName();
    String getDescription();
    Map<String, Object> getParameters();
    List<String> getRequired();
    String execute(Map<String, Object> arguments);
}
