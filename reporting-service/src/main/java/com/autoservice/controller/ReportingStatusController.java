package com.autoservice.controller;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ReportingStatusController {
    private final Scheduler scheduler;

    public ReportingStatusController(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @GetMapping("/status")
    public Map<String, String> status() throws SchedulerException {
        return Map.of("service", "reporting-service",
                "scheduler", scheduler.isStarted() && !scheduler.isInStandbyMode() ? "running" : "standby");
    }
}
