package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.mapper.HouseMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【house(房源主表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:05
*/
@Service
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House>
    implements HouseService{

}




