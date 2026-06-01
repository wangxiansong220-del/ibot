package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.DataSourceRequest;
import com.bbu.ibot.model.dto.DataSourceResponse;
import com.bbu.ibot.model.dto.DataSourceTestResponse;

import java.util.List;

public interface DataSourceAdminService {
    List<DataSourceResponse> listDataSources();
    DataSourceResponse createDataSource(DataSourceRequest request);
    DataSourceResponse updateDataSource(Long id, DataSourceRequest request);
    void deleteDataSource(Long id);
    DataSourceTestResponse testConnection(Long id);
}
