package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.HouseTag;
import com.zhuxiang.service.service.HouseTagService;
import com.zhuxiang.service.mapper.HouseTagMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author king-wang
* @description 针对表【house_tag(房源标签字典表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:26
*/
@Service
public class HouseTagServiceImpl extends ServiceImpl<HouseTagMapper, HouseTag>
    implements HouseTagService{

    @Override
    public List<HouseTag> getEnabledTags() {
        return list(
                Wrappers.<HouseTag>lambdaQuery()
                        .eq(HouseTag::getEnabled, 1)
                        .orderByAsc(HouseTag::getSortOrder)
        );
    }

}




