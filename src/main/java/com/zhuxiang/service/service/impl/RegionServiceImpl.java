package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.RegionService;
import com.zhuxiang.service.mapper.RegionMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【region(城市区域商圈表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:59
*/
@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region>
    implements RegionService{

}




