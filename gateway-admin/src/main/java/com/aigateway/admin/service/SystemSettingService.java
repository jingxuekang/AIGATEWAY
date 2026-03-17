package com.aigateway.admin.service;

import com.aigateway.admin.entity.SystemSetting;
import com.aigateway.admin.mapper.SystemSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统设置服务
 */
@Service
public class SystemSettingService extends ServiceImpl<SystemSettingMapper, SystemSetting> {

    public Map<String, String> getAllSettings() {
        List<SystemSetting> list = list();
        Map<String, String> map = new HashMap<>();
        for (SystemSetting setting : list) {
            map.put(setting.getConfigKey(), setting.getConfigValue());
        }
        return map;
    }

    public void saveSettings(Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : null;

            LambdaQueryWrapper<SystemSetting> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemSetting::getConfigKey, key);
            SystemSetting existing = getOne(wrapper);
            if (existing == null) {
                SystemSetting setting = new SystemSetting();
                setting.setConfigKey(key);
                setting.setConfigValue(value);
                save(setting);
            } else {
                existing.setConfigValue(value);
                updateById(existing);
            }
        }
    }
}

