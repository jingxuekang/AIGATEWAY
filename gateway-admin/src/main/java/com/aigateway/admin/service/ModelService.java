package com.aigateway.admin.service;

import com.aigateway.admin.entity.Model;
import com.aigateway.admin.mapper.ModelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型服务
 */
@Slf4j
@Service
public class ModelService extends ServiceImpl<ModelMapper, Model> {
    
    public List<Model> listAvailableModels() {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getStatus, 1);
        wrapper.orderByDesc(Model::getCreateTime);
        return list(wrapper);
    }
    
    public Model getByName(String modelName) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getModelName, modelName);
        wrapper.eq(Model::getStatus, 1);
        return getOne(wrapper);
    }
}
