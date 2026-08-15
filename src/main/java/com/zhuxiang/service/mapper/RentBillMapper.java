package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.RentBill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.dto.AdminBillDtos;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RentBillMapper extends BaseMapper<RentBill> {

    @Select("""
            SELECT
                COUNT(*) AS total_bill_count,
                COALESCE(SUM(bill.status = 'scheduled'), 0) AS scheduled_count,
                COALESCE(SUM(bill.status = 'pending'), 0) AS pending_count,
                COALESCE(SUM(bill.status = 'paid'), 0) AS paid_count,
                COALESCE(SUM(bill.status = 'overdue'), 0) AS overdue_count,
                COALESCE(SUM(bill.status = 'cancelled'), 0) AS cancelled_count,
                COALESCE(SUM(CASE WHEN bill.status <> 'cancelled'
                    THEN bill.amount_due + COALESCE(bill.overdue_amount, 0) ELSE 0 END), 0) AS receivable_amount,
                COALESCE(SUM(bill.amount_paid), 0) AS received_amount,
                COALESCE(SUM(CASE WHEN bill.status IN ('scheduled', 'pending', 'overdue')
                    THEN GREATEST(bill.amount_due + COALESCE(bill.overdue_amount, 0) - bill.amount_paid, 0) ELSE 0 END), 0)
                    AS outstanding_amount,
                COALESCE(SUM(CASE WHEN bill.status = 'overdue'
                    THEN GREATEST(bill.amount_due + COALESCE(bill.overdue_amount, 0) - bill.amount_paid, 0) ELSE 0 END), 0)
                    AS overdue_outstanding_amount
            FROM rent_bill bill
            LEFT JOIN lease lease_record ON lease_record.id = bill.lease_id
            LEFT JOIN house ON house.id = lease_record.house_id
            WHERE (#{landlordId} IS NULL OR house.landlord_id = #{landlordId})
            """)
    @ConstructorArgs({
            @Arg(column = "total_bill_count", javaType = long.class),
            @Arg(column = "scheduled_count", javaType = long.class),
            @Arg(column = "pending_count", javaType = long.class),
            @Arg(column = "paid_count", javaType = long.class),
            @Arg(column = "overdue_count", javaType = long.class),
            @Arg(column = "cancelled_count", javaType = long.class),
            @Arg(column = "receivable_amount", javaType = long.class),
            @Arg(column = "received_amount", javaType = long.class),
            @Arg(column = "outstanding_amount", javaType = long.class),
            @Arg(column = "overdue_outstanding_amount", javaType = long.class)
    })
    AdminBillDtos.BillSummary selectAdminSummary(@Param("landlordId") String landlordId);
}
