package com.aigateway.admin.controller;

import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "Task Management", description = "任务管理")
@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    @Operation(summary = "获取任务列表")
    @GetMapping
    public Result<Map<String, Object>> listTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        List<Map<String, Object>> tasks = new ArrayList<>();
        
        String[] taskTypes = {"quota_reset", "log_cleanup", "model_sync"};
        String[] statuses = {"success", "running", "failed"};
        
        for (int i = 0; i < 10; i++) {
            Map<String, Object> task = new HashMap<>();
            task.put("id", "task-" + UUID.randomUUID().toString());
            task.put("taskName", "定时任务-" + i);
            task.put("taskType", taskTypes[i % 3]);
            task.put("status", statuses[i % 3]);
            task.put("startTime", LocalDateTime.now().minusHours(i));
            task.put("endTime", LocalDateTime.now().minusHours(i).plusMinutes(5));
            task.put("duration", 5000L + i * 1000);
            task.put("result", "执行成功");
            tasks.add(task);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", tasks);
        result.put("total", tasks.size());
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
}
