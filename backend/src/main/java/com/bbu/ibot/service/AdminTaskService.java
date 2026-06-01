package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.AdminTaskResponse;
import com.bbu.ibot.model.entity.AdminTaskRecord;

import java.util.List;

public interface AdminTaskService {
    AdminTaskRecord createTask(String taskType, String targetType, String targetName, String message);
    void updateTask(Long taskId, String status, int progress, String message, String detailsJson);
    List<AdminTaskResponse> listTasks();
    AdminTaskResponse getTask(Long taskId);
}
