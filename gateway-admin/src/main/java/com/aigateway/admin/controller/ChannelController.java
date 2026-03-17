package com.aigateway.admin.controller;

import com.aigateway.admin.entity.Channel;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "Channel Management", description = "渠道管理")
@RestController
@RequestMapping("/api/admin/channels")
@RequiredArgsConstructor
public class ChannelController {
    
    @Operation(summary = "获取渠道列表")
    @GetMapping
    public Result<List<Map<String, Object>>> listChannels() {
        List<Map<String, Object>> channels = new ArrayList<>();
        
        Map<String, Object> channel1 = new HashMap<>();
        channel1.put("id", 1);
        channel1.put("name", "OpenAI-Primary");
        channel1.put("provider", "openai");
        channel1.put("baseUrl", "https://api.openai.com/v1");
        channel1.put("apiKey", "sk-***");
        channel1.put("status", 1);
        channel1.put("weight", 100);
        channel1.put("maxConcurrency", 100);
        channel1.put("timeout", 30000);
        channel1.put("createTime", LocalDateTime.now());
        channels.add(channel1);
        
        Map<String, Object> channel2 = new HashMap<>();
        channel2.put("id", 2);
        channel2.put("name", "Anthropic-Primary");
        channel2.put("provider", "anthropic");
        channel2.put("baseUrl", "https://api.anthropic.com");
        channel2.put("apiKey", "sk-ant-***");
        channel2.put("status", 1);
        channel2.put("weight", 100);
        channel2.put("maxConcurrency", 50);
        channel2.put("timeout", 30000);
        channel2.put("createTime", LocalDateTime.now());
        channels.add(channel2);
        
        return Result.success(channels);
    }
    
    @Operation(summary = "创建渠道")
    @PostMapping
    public Result<Channel> createChannel(@RequestBody Channel channel) {
        channel.setId(System.currentTimeMillis());
        channel.setCreateTime(LocalDateTime.now());
        channel.setUpdateTime(LocalDateTime.now());
        return Result.success(channel);
    }
    
    @Operation(summary = "更新渠道")
    @PutMapping("/{id}")
    public Result<Boolean> updateChannel(@PathVariable Long id, @RequestBody Channel channel) {
        return Result.success(true);
    }
    
    @Operation(summary = "删除渠道")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteChannel(@PathVariable Long id) {
        return Result.success(true);
    }
}
