package com.bbu.ibot.mapper;

import com.bbu.ibot.model.entity.UserAccount;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserAccountMapper {

    @Select("""
            SELECT id, email, password_hash, display_name, role, enabled, created_at, updated_at, last_login_at
            FROM user_account
            WHERE id = #{id}
            """)
    UserAccount findById(Long id);

    @Select("""
            SELECT id, email, password_hash, display_name, role, enabled, created_at, updated_at, last_login_at
            FROM user_account
            WHERE email = #{email}
            """)
    UserAccount findByEmail(String email);

    @Select("""
            SELECT id, email, password_hash, display_name, role, enabled, created_at, updated_at, last_login_at
            FROM user_account
            ORDER BY created_at DESC
            """)
    List<UserAccount> findAll();

    @Select("SELECT COUNT(*) FROM user_account WHERE role = 'ADMIN'")
    int countAdmins();

    @Insert("""
            INSERT INTO user_account(email, password_hash, display_name, role, enabled, created_at, updated_at, last_login_at)
            VALUES(#{email}, #{passwordHash}, #{displayName}, #{role}, #{enabled}, #{createdAt}, #{updatedAt}, #{lastLoginAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount userAccount);

    @Update("""
            UPDATE user_account
            SET display_name = #{displayName},
                role = #{role},
                enabled = #{enabled},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateProfile(UserAccount userAccount);

    @Update("""
            UPDATE user_account
            SET password_hash = #{passwordHash},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePassword(@Param("id") Long id,
                       @Param("passwordHash") String passwordHash,
                       @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE user_account
            SET last_login_at = #{lastLoginAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateLoginInfo(@Param("id") Long id,
                        @Param("lastLoginAt") LocalDateTime lastLoginAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Delete("""
            DELETE FROM user_account
            WHERE id = #{id}
            """)
    int deleteById(Long id);
}
