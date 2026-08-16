package com.zhuxiang.service.mapper;

import com.zhuxiang.service.dto.AdminDashboardDtos.AssetMetrics;
import com.zhuxiang.service.dto.AdminDashboardDtos.RecentHouse;
import com.zhuxiang.service.dto.AdminDashboardDtos.RentalTrendPoint;
import com.zhuxiang.service.dto.AdminDashboardDtos.WorkflowMetrics;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardMapper {

    @Select("""
            SELECT COUNT(*) AS total_count,
                   COALESCE(SUM(lock_bind_status = 'BOUND'), 0) AS smart_lock_bound_count,
                   COALESCE(SUM(view_count), 0) AS total_view_count,
                   COALESCE(ROUND(AVG(price)), 0) AS average_rent
            FROM house
            WHERE (#{landlordId} IS NULL OR BINARY landlord_id = BINARY #{landlordId})
            """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = long.class),
            @Arg(column = "smart_lock_bound_count", javaType = long.class),
            @Arg(column = "total_view_count", javaType = long.class),
            @Arg(column = "average_rent", javaType = long.class)
    })
    AssetMetrics selectAssetMetrics(@Param("landlordId") String landlordId);

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM lease lease_record
                 LEFT JOIN house ON BINARY house.id = BINARY lease_record.house_id
                 WHERE lease_record.status = 'pending'
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId})) AS pending_lease_count,
              (SELECT COUNT(*) FROM rent_bill bill
                 LEFT JOIN lease lease_record ON BINARY lease_record.id = BINARY bill.lease_id
                 LEFT JOIN house ON BINARY house.id = BINARY lease_record.house_id
                 WHERE due_date >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                   AND due_date < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
                   AND bill.status IN ('scheduled', 'pending', 'overdue')
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS current_month_outstanding_bill_count,
              (SELECT COALESCE(SUM(GREATEST(bill.amount_due + COALESCE(bill.overdue_amount, 0) - bill.amount_paid, 0)), 0)
                 FROM rent_bill bill
                 LEFT JOIN lease lease_record ON BINARY lease_record.id = BINARY bill.lease_id
                 LEFT JOIN house ON BINARY house.id = BINARY lease_record.house_id
                 WHERE due_date >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                   AND due_date < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
                   AND bill.status IN ('scheduled', 'pending', 'overdue')
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS current_month_outstanding_amount,
              (SELECT COUNT(*) FROM rent_bill bill
                 LEFT JOIN lease lease_record ON BINARY lease_record.id = BINARY bill.lease_id
                 LEFT JOIN house ON BINARY house.id = BINARY lease_record.house_id
                 WHERE due_date >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                   AND due_date < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
                   AND bill.status <> 'cancelled'
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS current_month_bill_count,
              (SELECT COUNT(*) FROM rent_bill bill
                 LEFT JOIN lease lease_record ON BINARY lease_record.id = BINARY bill.lease_id
                 LEFT JOIN house ON BINARY house.id = BINARY lease_record.house_id
                 WHERE due_date >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                   AND due_date < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
                   AND bill.status = 'paid'
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS current_month_paid_bill_count,
              (SELECT COALESCE(SUM(bill.amount_paid), 0) FROM rent_bill bill
                 LEFT JOIN lease lease_record ON BINARY lease_record.id = BINARY bill.lease_id
                 LEFT JOIN house ON BINARY house.id = BINARY lease_record.house_id
                 WHERE due_date >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                   AND due_date < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
                   AND bill.status <> 'cancelled'
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS current_month_received_amount,
              (SELECT COUNT(*) FROM repair_record repair
                 LEFT JOIN house ON BINARY house.id = BINARY repair.house_id
                 WHERE repair.deleted_at IS NULL
                   AND repair.status IN ('submitted', 'accepted', 'assigned', 'processing')
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS pending_repair_count,
              (SELECT COUNT(*) FROM appointment appointment_record
                 LEFT JOIN house ON BINARY house.id = BINARY appointment_record.house_id
                 WHERE DATE(COALESCE(appointment_record.appointment_start_at, appointment_record.appointment_date)) = CURRENT_DATE
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS today_appointment_count,
              (SELECT COUNT(*) FROM appointment appointment_record
                 LEFT JOIN house ON BINARY house.id = BINARY appointment_record.house_id
                 WHERE DATE(COALESCE(appointment_record.appointment_start_at, appointment_record.appointment_date)) = CURRENT_DATE
                   AND appointment_record.status = 'completed'
                   AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId}))
                 AS today_completed_appointment_count
            """)
    @ConstructorArgs({
            @Arg(column = "pending_lease_count", javaType = long.class),
            @Arg(column = "current_month_outstanding_bill_count", javaType = long.class),
            @Arg(column = "current_month_outstanding_amount", javaType = long.class),
            @Arg(column = "current_month_bill_count", javaType = long.class),
            @Arg(column = "current_month_paid_bill_count", javaType = long.class),
            @Arg(column = "current_month_received_amount", javaType = long.class),
            @Arg(column = "pending_repair_count", javaType = long.class),
            @Arg(column = "today_appointment_count", javaType = long.class),
            @Arg(column = "today_completed_appointment_count", javaType = long.class)
    })
    WorkflowMetrics selectWorkflowMetrics(@Param("landlordId") String landlordId);

    @Select("""
            SELECT DATE_SUB(start_date, INTERVAL WEEKDAY(start_date) DAY) AS week_start,
                   COUNT(*) AS rented_count
            FROM lease lease_record
            LEFT JOIN house ON BINARY house.id = BINARY lease_record.house_id
            WHERE lease_record.start_date >= #{startDate} AND lease_record.start_date < #{endDate}
              AND lease_record.status IN ('active', 'expired', 'terminated')
              AND (#{landlordId} IS NULL OR BINARY house.landlord_id = BINARY #{landlordId})
            GROUP BY DATE_SUB(lease_record.start_date, INTERVAL WEEKDAY(lease_record.start_date) DAY)
            ORDER BY week_start
            """)
    @ConstructorArgs({
            @Arg(column = "week_start", javaType = LocalDate.class),
            @Arg(column = "rented_count", javaType = long.class)
    })
    List<RentalTrendPoint> selectRentalTrend(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("landlordId") String landlordId
    );

    @Select("""
            SELECT id, title, COALESCE(location, address) AS location, room_type, price,
                   (lock_bind_status = 'BOUND') AS smart_lock_bound, created_at
            FROM house
            WHERE (#{landlordId} IS NULL OR BINARY landlord_id = BINARY #{landlordId})
            ORDER BY created_at DESC
            LIMIT 5
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "location", javaType = String.class),
            @Arg(column = "room_type", javaType = String.class),
            @Arg(column = "price", javaType = int.class),
            @Arg(column = "smart_lock_bound", javaType = boolean.class),
            @Arg(column = "created_at", javaType = java.time.LocalDateTime.class)
    })
    List<RecentHouse> selectRecentHouses(@Param("landlordId") String landlordId);
}
