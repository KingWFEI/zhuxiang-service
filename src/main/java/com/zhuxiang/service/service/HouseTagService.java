package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.HouseTag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author king-wang
* @description 针对表【house_tag(房源标签字典表)】的数据库操作Service
* @createDate 2026-06-12 19:57:26
*/
public interface HouseTagService extends IService<HouseTag> {

    /**
     * 查询所有已启用的房源标签，按排序值升序排列。
     */
    List<HouseTag> getEnabledTags();

}
