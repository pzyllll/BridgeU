package com.globalbuddy.repository;

import com.globalbuddy.model.VerificationCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 验证码Repository
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, String> {

    /**
     * 查找最新的有效验证码（按创建时间降序，只返回第一个）
     * 使用 Pageable 限制只返回一条记录
     */
    @Query("SELECT v FROM VerificationCode v " +
           "WHERE v.identifier = :identifier " +
           "AND v.type = :type " +
           "AND v.purpose = :purpose " +
           "AND v.used = false " +
           "AND v.expiresAt > :now " +
           "ORDER BY v.createdAt DESC")
    List<VerificationCode> findLatestValidCodes(
        String identifier,
        VerificationCode.CodeType type,
        VerificationCode.CodePurpose purpose,
        LocalDateTime now,
        Pageable pageable
    );

    /**
     * 查找最新的有效验证码（便捷方法，只返回第一条）
     */
    default Optional<VerificationCode> findLatestValidCode(
        String identifier,
        VerificationCode.CodeType type,
        VerificationCode.CodePurpose purpose,
        LocalDateTime now
    ) {
        List<VerificationCode> codes = findLatestValidCodes(
            identifier, type, purpose, now, 
            org.springframework.data.domain.PageRequest.of(0, 1)
        );
        return codes.isEmpty() ? Optional.empty() : Optional.of(codes.get(0));
    }

    /**
     * 标记验证码为已使用
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationCode v SET v.used = true WHERE v.id = :id")
    void markAsUsed(String id);

    /**
     * 删除过期的验证码
     */
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :now")
    void deleteExpiredCodes(LocalDateTime now);

    /**
     * 检查指定标识符在指定时间内是否已发送过验证码
     */
    @Query("SELECT COUNT(v) > 0 FROM VerificationCode v " +
           "WHERE v.identifier = :identifier " +
           "AND v.type = :type " +
           "AND v.createdAt > :since")
    boolean existsRecentCode(String identifier, VerificationCode.CodeType type, LocalDateTime since);
}

