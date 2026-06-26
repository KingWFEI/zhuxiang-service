package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.RentBill;
import com.zhuxiang.service.mapper.RentBillMapper;
import com.zhuxiang.service.service.RentBillService;
import org.springframework.stereotype.Service;

@Service
public class RentBillServiceImpl extends ServiceImpl<RentBillMapper, RentBill>
        implements RentBillService {
}
