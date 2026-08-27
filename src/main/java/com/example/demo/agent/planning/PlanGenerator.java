package com.example.demo.agent.planning;

import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.ScreeningResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 7 天计划生成接口。
 * M1 依赖这个接口，不依赖具体的任务模板实现。
 */
public interface PlanGenerator {

    List<PlanDay> generate(ScreeningResult screeningResult, LocalDate startDate);
}
