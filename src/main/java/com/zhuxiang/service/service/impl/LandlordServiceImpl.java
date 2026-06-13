package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.mapper.LandlordMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【landlord(房东与平台管家资料表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:34
*/
@Service
public class LandlordServiceImpl extends ServiceImpl<LandlordMapper, Landlord>
    implements LandlordService{

    /**
     * 查询并组装指定房东资料。
     */
    @Override
    public HouseDtos.LandlordView getLandlordDetail(String landlordId) {
        Landlord landlord = requireLandlord(landlordId);
        return new HouseDtos.LandlordView(
                landlord.getId(),
                landlord.getName(),
                landlord.getAvatarUrl(),
                Integer.valueOf(1).equals(landlord.getIsVerified()),
                landlord.getRating(),
                landlord.getRentedCount(),
                landlord.getResponseDescription()
        );
    }

    /**
     * 查询并校验指定房东是否存在。
     */
    @Override
    public Landlord requireLandlord(String landlordId) {
        Landlord landlord = getById(landlordId);
        if (landlord == null) {
            throw BusinessException.notFound("房东不存在");
        }
        return landlord;
    }
}




