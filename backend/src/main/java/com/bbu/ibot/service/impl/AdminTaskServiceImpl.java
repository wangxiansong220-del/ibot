package com.bbu.ibot.service.impl;

import com.bbu.ibot.mapper.AdminTaskMapper;
import com.bbu.ibot.model.dto.AdminTaskResponse;
import com.bbu.ibot.model.entity.AdminTaskRecord;
import com.bbu.ibot.security.SecurityUtil;
import com.bbu.ibot.service.AdminTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminTaskServiceImpl implements AdminTaskService {

    private final AdminTaskMapper adminTaskMapper;

    public AdminTaskServiceImpl(AdminTaskMapper adminTaskMapper) {
        this.adminTaskMapper = adminTaskMapper;
    }

    @Override
    public AdminTaskRecord createTask(String taskType, String targetType, String targetName, String message) {
        LocalDateTime now = LocalDateTime.now();
        AdminTaskRecord record = AdminTaskRecord.builder()
                .taskType(taskType)
                .targetType(targetType)
                .targetName(targetName)
                .status("PENDING")
                .progress(0)
                .message(message)
                .createdBy(SecurityUtil.currentOperator())
                .startedAt(now)
                .updatedAt(now)
                .build();
        adminTaskMapper.insert(record);
        return record;
    }

    @Override
    public void updateTask(Long taskId, String status, int progress, String message, String detailsJson) {
        AdminTaskRecord record = adminTaskMapper.findById(taskId);
        if (record == null) {
            return;
        }
        record.setStatus(status);
        record.setProgress(progress);
        record.setMessage(message);
        record.setDetailsJson(detailsJson);
        record.setUpdatedAt(LocalDateTime.now());
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            record.setCompletedAt(LocalDateTime.now());
        }
        adminTaskMapper.update(record);
    }

    @Override
    public List<AdminTaskResponse> listTasks() {
        return adminTaskMapper.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public AdminTaskResponse getTask(Long taskId) {
        AdminTaskRecord record = adminTaskMapper.findById(taskId);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found");
        }
        return toResponse(record);
    }

    private AdminTaskResponse toResponse(AdminTaskRecord record) {
        return AdminTaskResponse.builder()
                .id(record.getId())
                .taskType(record.getTaskType())
                .targetType(record.getTargetType())
                .targetName(record.getTargetName())
                .status(record.getStatus())
                .progress(record.getProgress())
                .message(record.getMessage())
                .detailsJson(record.getDetailsJson())
                .createdBy(record.getCreatedBy())
                .startedAt(record.getStartedAt())
                .completedAt(record.getCompletedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
