package com.example.demo.tool;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ToolRegistry implements InitializingBean {

    private final Map<String, Tool> toolMap = new LinkedHashMap<>();

    @Autowired
    private List<Tool> tools;

    @Override
    public void afterPropertiesSet() {
        for (Tool tool : tools) {
            toolMap.put(tool.getName(), tool);
            System.out.println("[ToolRegistry] 已注册工具：" + tool.getName());
        }
        System.out.println("[ToolRegistry] 共注册 " + toolMap.size() + " 个工具");
    }

    public Tool getTool(String name) {
        return toolMap.get(name);
    }

    public List<Map<String, Object>> getAllToolSchemas() {
        List<Map<String, Object>> schemas = new ArrayList<>();
        for (Tool tool : toolMap.values()) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", tool.getParameters());
            parameters.put("required", tool.getRequired());
            parameters.put("additionalProperties", false);
            function.put("parameters", parameters);

            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "function");
            schema.put("function", function);
            schemas.add(schema);
        }
        return schemas;
    }

    public Set<String> getToolNames() {
        return toolMap.keySet();
    }
}