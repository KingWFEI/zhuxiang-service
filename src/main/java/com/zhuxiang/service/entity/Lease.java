package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 租约表
 * @TableName lease
 */
@TableName(value ="lease")
@Data
public class Lease implements Serializable {
    /**
     * 租约ID，主键
     */
    @TableId
    private String id;

    /**
     * 租客用户ID
     */
    private String userId;

    /**
     * 关联房源ID
     */
    private String houseId;

    /**
     * 租约状态：pending待生效，active生效中，expired已到期，terminated已退租
     */
    private String status;

    /**
     * 租约开始日期
     */
    private LocalDate startDate;

    /**
     * 租约结束日期
     */
    private LocalDate endDate;

    /**
     * 租期月数
     */
    private Integer leaseMonths;

    /**
     * 付款方式：monthly/quarterly/semi_annual/annual
     */
    private String paymentMethod;

    /**
     * 每期付款覆盖月数
     */
    private Integer paymentMonths;

    /**
     * 月租金，单位：分
     */
    private Integer monthlyRent;

    /**
     * 押金，单位：分
     */
    private Integer deposit;

    /**
     * 服务费，单位：分
     */
    private Integer serviceFee;

    /**
     * 首期款金额，单位：分
     */
    private Integer firstPaymentAmount;

    /**
     * 合同ID，第二阶段接入合同表后关联
     */
    private String contractId;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Lease other = (Lease) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getHouseId() == null ? other.getHouseId() == null : this.getHouseId().equals(other.getHouseId()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getStartDate() == null ? other.getStartDate() == null : this.getStartDate().equals(other.getStartDate()))
            && (this.getEndDate() == null ? other.getEndDate() == null : this.getEndDate().equals(other.getEndDate()))
            && (this.getLeaseMonths() == null ? other.getLeaseMonths() == null : this.getLeaseMonths().equals(other.getLeaseMonths()))
            && (this.getPaymentMethod() == null ? other.getPaymentMethod() == null : this.getPaymentMethod().equals(other.getPaymentMethod()))
            && (this.getPaymentMonths() == null ? other.getPaymentMonths() == null : this.getPaymentMonths().equals(other.getPaymentMonths()))
            && (this.getMonthlyRent() == null ? other.getMonthlyRent() == null : this.getMonthlyRent().equals(other.getMonthlyRent()))
            && (this.getDeposit() == null ? other.getDeposit() == null : this.getDeposit().equals(other.getDeposit()))
            && (this.getServiceFee() == null ? other.getServiceFee() == null : this.getServiceFee().equals(other.getServiceFee()))
            && (this.getFirstPaymentAmount() == null ? other.getFirstPaymentAmount() == null : this.getFirstPaymentAmount().equals(other.getFirstPaymentAmount()))
            && (this.getContractId() == null ? other.getContractId() == null : this.getContractId().equals(other.getContractId()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getHouseId() == null) ? 0 : getHouseId().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getStartDate() == null) ? 0 : getStartDate().hashCode());
        result = prime * result + ((getEndDate() == null) ? 0 : getEndDate().hashCode());
        result = prime * result + ((getLeaseMonths() == null) ? 0 : getLeaseMonths().hashCode());
        result = prime * result + ((getPaymentMethod() == null) ? 0 : getPaymentMethod().hashCode());
        result = prime * result + ((getPaymentMonths() == null) ? 0 : getPaymentMonths().hashCode());
        result = prime * result + ((getMonthlyRent() == null) ? 0 : getMonthlyRent().hashCode());
        result = prime * result + ((getDeposit() == null) ? 0 : getDeposit().hashCode());
        result = prime * result + ((getServiceFee() == null) ? 0 : getServiceFee().hashCode());
        result = prime * result + ((getFirstPaymentAmount() == null) ? 0 : getFirstPaymentAmount().hashCode());
        result = prime * result + ((getContractId() == null) ? 0 : getContractId().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", houseId=").append(houseId);
        sb.append(", status=").append(status);
        sb.append(", startDate=").append(startDate);
        sb.append(", endDate=").append(endDate);
        sb.append(", leaseMonths=").append(leaseMonths);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", paymentMonths=").append(paymentMonths);
        sb.append(", monthlyRent=").append(monthlyRent);
        sb.append(", deposit=").append(deposit);
        sb.append(", serviceFee=").append(serviceFee);
        sb.append(", firstPaymentAmount=").append(firstPaymentAmount);
        sb.append(", contractId=").append(contractId);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}