package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.AdminTaskResponse;
import com.bbu.ibot.service.AdminTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    public AdminTaskController(AdminTaskService adminTaskService) {
        this.adminTaskService = adminTaskService;
    }

    @GetMapping
    public List<AdminTaskResponse> listTasks() {
        return adminTaskService.listTasks();
    }

    @GetMapping("/{taskId}")
    public AdminTaskResponse getTask(@PathVariable Long taskId) {
        return adminTaskService.getTask(taskId);
    }
}
