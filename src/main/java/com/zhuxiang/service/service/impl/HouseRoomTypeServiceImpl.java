package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.HouseRoomType;
import com.zhuxiang.service.mapper.HouseRoomTypeMapper;
import com.zhuxiang.service.service.HouseRoomTypeService;
import org.springframework.stereotype.Service;

@Service
public class HouseRoomTypeServiceImpl
        extends ServiceImpl<HouseRoomTypeMapper, HouseRoomType>
        implements HouseRoomTypeService {
}
