package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.ApiMessageResponse;
import com.bbu.ibot.model.dto.DataSourceRequest;
import com.bbu.ibot.model.dto.DataSourceResponse;
import com.bbu.ibot.model.dto.DataSourceTestResponse;
import com.bbu.ibot.service.DataSourceAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/datasources")
public class AdminDataSourceController {

    private final DataSourceAdminService dataSourceAdminService;

    public AdminDataSourceController(DataSourceAdminService dataSourceAdminService) {
        this.dataSourceAdminService = dataSourceAdminService;
    }

    @GetMapping
    public List<DataSourceResponse> listDataSources() {
        return dataSourceAdminService.listDataSources();
    }

    @PostMapping
    public DataSourceResponse createDataSource(@Valid @RequestBody DataSourceRequest request) {
        return dataSourceAdminService.createDataSource(request);
    }

    @PutMapping("/{id}")
    public DataSourceResponse updateDataSource(@PathVariable Long id,
                                               @Valid @RequestBody DataSourceRequest request) {
        return dataSourceAdminService.updateDataSource(id, request);
    }

    @DeleteMapping("/{id}")
    public ApiMessageResponse deleteDataSource(@PathVariable Long id) {
        dataSourceAdminService.deleteDataSource(id);
        return new ApiMessageResponse("data source deleted");
    }

    @PostMapping("/{id}/test")
    public DataSourceTestResponse testConnection(@PathVariable Long id) {
        return dataSourceAdminService.testConnection(id);
    }
}
