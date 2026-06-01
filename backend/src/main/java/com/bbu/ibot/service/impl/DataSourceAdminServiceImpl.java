package com.bbu.ibot.service.impl;

import com.bbu.ibot.mapper.DataSourceConfigMapper;
import com.bbu.ibot.model.dto.DataSourceRequest;
import com.bbu.ibot.model.dto.DataSourceResponse;
import com.bbu.ibot.model.dto.DataSourceTestResponse;
import com.bbu.ibot.model.entity.DataSourceConfigEntity;
import com.bbu.ibot.security.SecretCryptoService;
import com.bbu.ibot.service.AdminTaskService;
import com.bbu.ibot.service.DataSourceAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class DataSourceAdminServiceImpl implements DataSourceAdminService {

    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final SecretCryptoService secretCryptoService;
    private final AdminTaskService adminTaskService;

    public DataSourceAdminServiceImpl(DataSourceConfigMapper dataSourceConfigMapper,
                                      SecretCryptoService secretCryptoService,
                                      AdminTaskService adminTaskService) {
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.secretCryptoService = secretCryptoService;
        this.adminTaskService = adminTaskService;
    }

    @Override
    public List<DataSourceResponse> listDataSources() {
        return dataSourceConfigMapper.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public DataSourceResponse createDataSource(DataSourceRequest request) {
        validateUniqueName(request.getName(), null);
        LocalDateTime now = LocalDateTime.now();
        DataSourceConfigEntity entity = DataSourceConfigEntity.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .host(trimToNull(request.getHost()))
                .port(request.getPort())
                .databaseName(trimToNull(request.getDatabaseName()))
                .username(trimToNull(request.getUsername()))
                .secretValue(secretCryptoService.encrypt(trimToNull(request.getPassword())))
                .apiBaseUrl(trimToNull(request.getApiBaseUrl()))
                .notes(trimToNull(request.getNotes()))
                .enabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled())
                .createdAt(now)
                .updatedAt(now)
                .build();
        dataSourceConfigMapper.insert(entity);
        return toResponse(entity);
    }

    @Override
    public DataSourceResponse updateDataSource(Long id, DataSourceRequest request) {
        DataSourceConfigEntity entity = requireDataSource(id);
        validateUniqueName(request.getName(), id);
        entity.setName(request.getName().trim());
        entity.setType(request.getType());
        entity.setHost(trimToNull(request.getHost()));
        entity.setPort(request.getPort());
        entity.setDatabaseName(trimToNull(request.getDatabaseName()));
        entity.setUsername(trimToNull(request.getUsername()));
        if (StringUtils.hasText(request.getPassword())) {
            entity.setSecretValue(secretCryptoService.encrypt(request.getPassword().trim()));
        }
        entity.setApiBaseUrl(trimToNull(request.getApiBaseUrl()));
        entity.setNotes(trimToNull(request.getNotes()));
        entity.setEnabled(request.getEnabled() == null ? entity.getEnabled() : request.getEnabled());
        entity.setUpdatedAt(LocalDateTime.now());
        dataSourceConfigMapper.update(entity);
        return toResponse(entity);
    }

    @Override
    public void deleteDataSource(Long id) {
        requireDataSource(id);
        dataSourceConfigMapper.deleteById(id);
    }

    @Override
    public DataSourceTestResponse testConnection(Long id) {
        DataSourceConfigEntity entity = requireDataSource(id);
        var task = adminTaskService.createTask("DATASOURCE_TEST", "DATASOURCE", entity.getName(), "Testing data source connectivity");
        LocalDateTime testedAt = LocalDateTime.now();
        try {
            String message = testReachability(entity);
            entity.setLastTestStatus("SUCCESS");
            entity.setLastTestMessage(message);
            entity.setLastTestedAt(testedAt);
            entity.setUpdatedAt(testedAt);
            dataSourceConfigMapper.update(entity);
            adminTaskService.updateTask(task.getId(), "COMPLETED", 100, message, null);
            return DataSourceTestResponse.builder()
                    .taskId(task.getId())
                    .dataSourceId(entity.getId())
                    .status("SUCCESS")
                    .message(message)
                    .testedAt(testedAt)
                    .build();
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "connection test failed" : exception.getMessage();
            entity.setLastTestStatus("FAILED");
            entity.setLastTestMessage(message);
            entity.setLastTestedAt(testedAt);
            entity.setUpdatedAt(testedAt);
            dataSourceConfigMapper.update(entity);
            adminTaskService.updateTask(task.getId(), "FAILED", 100, message, null);
            return DataSourceTestResponse.builder()
                    .taskId(task.getId())
                    .dataSourceId(entity.getId())
                    .status("FAILED")
                    .message(message)
                    .testedAt(testedAt)
                    .build();
        }
    }

    private String testReachability(DataSourceConfigEntity entity) throws Exception {
        String type = entity.getType().toUpperCase(Locale.ROOT);
        if ("API_ENDPOINT".equals(type)) {
            if (!StringUtils.hasText(entity.getApiBaseUrl())) {
                throw new IllegalStateException("API endpoint URL is required");
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(entity.getApiBaseUrl()).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            return "HTTP endpoint reachable, status code: " + status;
        }

        if (!StringUtils.hasText(entity.getHost()) || entity.getPort() == null) {
            throw new IllegalStateException("host and port are required for this data source type");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(entity.getHost(), entity.getPort()), 3000);
            return "TCP connection succeeded: " + entity.getHost() + ":" + entity.getPort();
        }
    }

    private void validateUniqueName(String name, Long excludeId) {
        if (dataSourceConfigMapper.countByName(name.trim(), excludeId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "data source name already exists");
        }
    }

    private DataSourceConfigEntity requireDataSource(Long id) {
        DataSourceConfigEntity entity = dataSourceConfigMapper.findById(id);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "data source not found");
        }
        return entity;
    }

    private DataSourceResponse toResponse(DataSourceConfigEntity entity) {
        return DataSourceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .host(entity.getHost())
                .port(entity.getPort())
                .databaseName(entity.getDatabaseName())
                .username(entity.getUsername())
                .apiBaseUrl(entity.getApiBaseUrl())
                .notes(entity.getNotes())
                .enabled(entity.getEnabled())
                .hasPassword(StringUtils.hasText(entity.getSecretValue()))
                .lastTestStatus(entity.getLastTestStatus())
                .lastTestMessage(entity.getLastTestMessage())
                .lastTestedAt(entity.getLastTestedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
