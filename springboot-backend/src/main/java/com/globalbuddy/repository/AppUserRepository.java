package com.globalbuddy.repository;

import com.globalbuddy.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
public interface AppUserRepository extends JpaRepository<AppUser, String> {
    
    /**
     * 根据邮箱查找用户
     */
    Optional<AppUser> findByEmail(String email);
    
    /**
     * 根据用户名查找用户
     */
    Optional<AppUser> findByUsername(String username);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据角色查找用户
     */
    List<AppUser> findByRole(AppUser.Role role);
}

