package com.bbu.ibot.mapper;

import com.bbu.ibot.model.entity.SystemSettingEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SystemSettingMapper {

    @Results(value = {
            @Result(property = "settingKey", column = "setting_key"),
            @Result(property = "settingValue", column = "setting_value"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "sensitive", column = "is_sensitive"),
            @Result(property = "updatedBy", column = "updated_by"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("""
            SELECT setting_key, setting_value, category_name, is_sensitive, updated_by, updated_at
            FROM system_setting
            ORDER BY setting_key
            """)
    List<SystemSettingEntity> findAll();

    @Results(value = {
            @Result(property = "settingKey", column = "setting_key"),
            @Result(property = "settingValue", column = "setting_value"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "sensitive", column = "is_sensitive"),
            @Result(property = "updatedBy", column = "updated_by"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("""
            SELECT setting_key, setting_value, category_name, is_sensitive, updated_by, updated_at
            FROM system_setting
            WHERE setting_key = #{settingKey}
            """)
    SystemSettingEntity findByKey(String settingKey);

    @Insert("""
            INSERT INTO system_setting(setting_key, setting_value, category_name, is_sensitive, updated_by, updated_at)
            VALUES(#{settingKey}, #{settingValue}, #{categoryName}, #{sensitive}, #{updatedBy}, #{updatedAt})
            """)
    int insert(SystemSettingEntity entity);

    @Update("""
            UPDATE system_setting
            SET setting_value = #{settingValue},
                category_name = #{categoryName},
                is_sensitive = #{sensitive},
                updated_by = #{updatedBy},
                updated_at = #{updatedAt}
            WHERE setting_key = #{settingKey}
            """)
    int update(SystemSettingEntity entity);
}
