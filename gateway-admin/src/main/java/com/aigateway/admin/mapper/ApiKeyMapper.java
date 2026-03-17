package com.aigateway.admin.mapper;

import com.aigateway.admin.entity.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    /**
     * 原子性递增 used_quota，防止并发超用
     */
    @Update("UPDATE api_key SET used_quota = used_quota + #{tokens} WHERE id = #{keyId} AND deleted = 0")
    int incrementUsedQuota(@Param("keyId") Long keyId, @Param("tokens") long tokens);
}
