package com.bbu.ibot.mapper;

import com.bbu.ibot.model.entity.DataSourceConfigEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DataSourceConfigMapper {

    @Select("""
            SELECT id, name, type, host, port, database_name, username, secret_value, api_base_url, notes,
                   enabled, last_test_status, last_test_message, last_tested_at, created_at, updated_at
            FROM data_source_config
            ORDER BY created_at DESC
            """)
    List<DataSourceConfigEntity> findAll();

    @Select("""
            SELECT id, name, type, host, port, database_name, username, secret_value, api_base_url, notes,
                   enabled, last_test_status, last_test_message, last_tested_at, created_at, updated_at
            FROM data_source_config
            WHERE id = #{id}
            """)
    DataSourceConfigEntity findById(Long id);

    @Select("SELECT COUNT(*) FROM data_source_config WHERE name = #{name} AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO data_source_config(name, type, host, port, database_name, username, secret_value, api_base_url,
                                           notes, enabled, last_test_status, last_test_message, last_tested_at, created_at, updated_at)
            VALUES(#{name}, #{type}, #{host}, #{port}, #{databaseName}, #{username}, #{secretValue}, #{apiBaseUrl},
                   #{notes}, #{enabled}, #{lastTestStatus}, #{lastTestMessage}, #{lastTestedAt}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataSourceConfigEntity entity);

    @Update("""
            UPDATE data_source_config
            SET name = #{name},
                type = #{type},
                host = #{host},
                port = #{port},
                database_name = #{databaseName},
                username = #{username},
                secret_value = #{secretValue},
                api_base_url = #{apiBaseUrl},
                notes = #{notes},
                enabled = #{enabled},
                last_test_status = #{lastTestStatus},
                last_test_message = #{lastTestMessage},
                last_tested_at = #{lastTestedAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(DataSourceConfigEntity entity);

    @Delete("DELETE FROM data_source_config WHERE id = #{id}")
    int deleteById(Long id);
}
