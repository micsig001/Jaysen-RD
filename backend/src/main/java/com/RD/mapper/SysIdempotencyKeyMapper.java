package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.SysIdempotencyKey;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 幂等性 Key Mapper
 */
@Mapper
public interface SysIdempotencyKeyMapper extends BaseMapper<SysIdempotencyKey> {

    /**
     * 根据幂等 Key 查询
     */
    @Select("SELECT * FROM sys_idempotency_key WHERE idempotency_key = #{idempotencyKey} LIMIT 1")
    SysIdempotencyKey selectByKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * 更新响应数据 + 延长过期时间
     */
    @Update("UPDATE sys_idempotency_key SET response_data = #{responseData}, " +
            "expires_at = #{expiresAt}, status = 'COMPLETED' " +
            "WHERE idempotency_key = #{idempotencyKey}")
    int updateResponseData(@Param("idempotencyKey") String idempotencyKey,
                           @Param("responseData") String responseData,
                           @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * 根据幂等 Key 删除
     */
    @Delete("DELETE FROM sys_idempotency_key WHERE idempotency_key = #{idempotencyKey}")
    int deleteByKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * 清理过期记录
     */
    @Delete("DELETE FROM sys_idempotency_key WHERE expires_at < #{now}")
    int deleteExpired(@Param("now") LocalDateTime now);
}
