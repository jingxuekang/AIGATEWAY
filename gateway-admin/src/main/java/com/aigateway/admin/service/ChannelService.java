package com.aigateway.admin.service;

import com.aigateway.admin.entity.Channel;
import com.aigateway.admin.mapper.ChannelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChannelService extends ServiceImpl<ChannelMapper, Channel> {

    public List<Channel> listChannels() {
        return list(new LambdaQueryWrapper<Channel>()
                .orderByDesc(Channel::getWeight)
                .orderByAsc(Channel::getId));
    }
}
