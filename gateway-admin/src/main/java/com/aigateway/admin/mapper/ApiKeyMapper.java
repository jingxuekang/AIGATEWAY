package com.aigateway.admin.mapper;

import com.aigateway.admin.entity.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    @Select("SELECT COUNT(*) FROM api_key WHERE deleted = 0 AND user_id = #{userId}")
    Long countByUser(@Param("userId") String userId);

    /**
     * 原子性递增 used_quota，同时检查不超过 total_quota
     * total_quota = 0 表示不限额，直接累加
     * 返回影响行数：1=成功，0=配额不足
     */
    @Update("UPDATE api_key SET used_quota = used_quota + #{tokens} " +
            "WHERE id = #{keyId} AND deleted = 0 " +
            "AND (total_quota = 0 OR used_quota + #{tokens} <= total_quota)")
    int incrementUsedQuota(@Param("keyId") Long keyId, @Param("tokens") long tokens);
}
