package com.bbu.ibot.mapper;

import com.bbu.ibot.model.entity.AdminTaskRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminTaskMapper {

    @Insert("""
            INSERT INTO admin_task(task_type, target_type, target_name, status, progress, message, details_json,
                                   created_by, started_at, completed_at, updated_at)
            VALUES(#{taskType}, #{targetType}, #{targetName}, #{status}, #{progress}, #{message}, #{detailsJson},
                   #{createdBy}, #{startedAt}, #{completedAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminTaskRecord record);

    @Update("""
            UPDATE admin_task
            SET status = #{status},
                progress = #{progress},
                message = #{message},
                details_json = #{detailsJson},
                completed_at = #{completedAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(AdminTaskRecord record);

    @Select("""
            SELECT id, task_type, target_type, target_name, status, progress, message, details_json,
                   created_by, started_at, completed_at, updated_at
            FROM admin_task
            ORDER BY started_at DESC
            """)
    List<AdminTaskRecord> findAll();

    @Select("""
            SELECT id, task_type, target_type, target_name, status, progress, message, details_json,
                   created_by, started_at, completed_at, updated_at
            FROM admin_task
            WHERE id = #{id}
            """)
    AdminTaskRecord findById(Long id);
}
