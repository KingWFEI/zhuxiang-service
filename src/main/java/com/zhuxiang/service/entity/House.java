package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 房源主表
 * @TableName house
 */
@TableName(value ="house")
@Data
public class House implements Serializable {
    /**
     * 房源ID，主键
     */
    @TableId
    private String id;

    /**
     * 房源标题
     */
    private String title;

    /**
     * 房源封面图URL
     */
    private String coverImage;

    /**
     * 位置描述，例如渝北区
     */
    private String location;

    /**
     * 所属小区ID
     */
    private String communityId;

    /**
     * 房源详细地址
     */
    private String address;

    /**
     * 楼栋，例如3栋
     */
    private String building;

    /**
     * 单元，例如2单元
     */
    private String unit;

    /**
     * 房间号，例如1201
     */
    private String room;

    /**
     * 月租金，单位：分
     */
    private Integer price;

    /**
     * 押金，单位：分
     */
    private Integer deposit;

    /**
     * 付款方式，例如押一付一
     */
    private String paymentMethod;

    /**
     * 户型，例如1室1厅1卫
     */
    private String roomType;

    /**
     * 房屋面积，单位平方米
     */
    private BigDecimal area;

    /**
     * 楼层描述，例如12/28层
     */
    private String floor;

    /**
     * 房屋朝向，例如朝南
     */
    private String orientation;

    /**
     * 装修情况，例如精装修
     */
    private String decoration;

    /**
     * 可入住日期
     */
    private LocalDate availableDate;

    /**
     * 地铁交通信息，例如距3号线500m
     */
    private String metro;

    /**
     * 房源描述
     */
    private String description;

    /**
     * 租赁分类：recommended推荐，short_rent短租，homestay民宿，long_rent长租
     */
    private String rentType;

    /**
     * 房源状态：draft草稿，available可租，rented已租，offline下架
     */
    private String status;

    /**
     * 是否支持智能门锁：0否，1是
     */
    private Integer isSmartLockSupported;

    /**
     * 是否支持自主看房：0否，1是
     */
    private Integer isSelfViewingSupported;

    /**
     * 关联房东或管家ID
     */
    private String landlordId;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 收藏次数
     */
    private Integer favoriteCount;

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
        House other = (House) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getCoverImage() == null ? other.getCoverImage() == null : this.getCoverImage().equals(other.getCoverImage()))
            && (this.getLocation() == null ? other.getLocation() == null : this.getLocation().equals(other.getLocation()))
            && (this.getCommunityId() == null ? other.getCommunityId() == null : this.getCommunityId().equals(other.getCommunityId()))
            && (this.getAddress() == null ? other.getAddress() == null : this.getAddress().equals(other.getAddress()))
            && (this.getBuilding() == null ? other.getBuilding() == null : this.getBuilding().equals(other.getBuilding()))
            && (this.getUnit() == null ? other.getUnit() == null : this.getUnit().equals(other.getUnit()))
            && (this.getRoom() == null ? other.getRoom() == null : this.getRoom().equals(other.getRoom()))
            && (this.getPrice() == null ? other.getPrice() == null : this.getPrice().equals(other.getPrice()))
            && (this.getDeposit() == null ? other.getDeposit() == null : this.getDeposit().equals(other.getDeposit()))
            && (this.getPaymentMethod() == null ? other.getPaymentMethod() == null : this.getPaymentMethod().equals(other.getPaymentMethod()))
            && (this.getRoomType() == null ? other.getRoomType() == null : this.getRoomType().equals(other.getRoomType()))
            && (this.getArea() == null ? other.getArea() == null : this.getArea().equals(other.getArea()))
            && (this.getFloor() == null ? other.getFloor() == null : this.getFloor().equals(other.getFloor()))
            && (this.getOrientation() == null ? other.getOrientation() == null : this.getOrientation().equals(other.getOrientation()))
            && (this.getDecoration() == null ? other.getDecoration() == null : this.getDecoration().equals(other.getDecoration()))
            && (this.getAvailableDate() == null ? other.getAvailableDate() == null : this.getAvailableDate().equals(other.getAvailableDate()))
            && (this.getMetro() == null ? other.getMetro() == null : this.getMetro().equals(other.getMetro()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getRentType() == null ? other.getRentType() == null : this.getRentType().equals(other.getRentType()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getIsSmartLockSupported() == null ? other.getIsSmartLockSupported() == null : this.getIsSmartLockSupported().equals(other.getIsSmartLockSupported()))
            && (this.getIsSelfViewingSupported() == null ? other.getIsSelfViewingSupported() == null : this.getIsSelfViewingSupported().equals(other.getIsSelfViewingSupported()))
            && (this.getLandlordId() == null ? other.getLandlordId() == null : this.getLandlordId().equals(other.getLandlordId()))
            && (this.getViewCount() == null ? other.getViewCount() == null : this.getViewCount().equals(other.getViewCount()))
            && (this.getFavoriteCount() == null ? other.getFavoriteCount() == null : this.getFavoriteCount().equals(other.getFavoriteCount()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getCoverImage() == null) ? 0 : getCoverImage().hashCode());
        result = prime * result + ((getLocation() == null) ? 0 : getLocation().hashCode());
        result = prime * result + ((getCommunityId() == null) ? 0 : getCommunityId().hashCode());
        result = prime * result + ((getAddress() == null) ? 0 : getAddress().hashCode());
        result = prime * result + ((getBuilding() == null) ? 0 : getBuilding().hashCode());
        result = prime * result + ((getUnit() == null) ? 0 : getUnit().hashCode());
        result = prime * result + ((getRoom() == null) ? 0 : getRoom().hashCode());
        result = prime * result + ((getPrice() == null) ? 0 : getPrice().hashCode());
        result = prime * result + ((getDeposit() == null) ? 0 : getDeposit().hashCode());
        result = prime * result + ((getPaymentMethod() == null) ? 0 : getPaymentMethod().hashCode());
        result = prime * result + ((getRoomType() == null) ? 0 : getRoomType().hashCode());
        result = prime * result + ((getArea() == null) ? 0 : getArea().hashCode());
        result = prime * result + ((getFloor() == null) ? 0 : getFloor().hashCode());
        result = prime * result + ((getOrientation() == null) ? 0 : getOrientation().hashCode());
        result = prime * result + ((getDecoration() == null) ? 0 : getDecoration().hashCode());
        result = prime * result + ((getAvailableDate() == null) ? 0 : getAvailableDate().hashCode());
        result = prime * result + ((getMetro() == null) ? 0 : getMetro().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getRentType() == null) ? 0 : getRentType().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getIsSmartLockSupported() == null) ? 0 : getIsSmartLockSupported().hashCode());
        result = prime * result + ((getIsSelfViewingSupported() == null) ? 0 : getIsSelfViewingSupported().hashCode());
        result = prime * result + ((getLandlordId() == null) ? 0 : getLandlordId().hashCode());
        result = prime * result + ((getViewCount() == null) ? 0 : getViewCount().hashCode());
        result = prime * result + ((getFavoriteCount() == null) ? 0 : getFavoriteCount().hashCode());
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
        sb.append(", title=").append(title);
        sb.append(", coverImage=").append(coverImage);
        sb.append(", location=").append(location);
        sb.append(", communityId=").append(communityId);
        sb.append(", address=").append(address);
        sb.append(", building=").append(building);
        sb.append(", unit=").append(unit);
        sb.append(", room=").append(room);
        sb.append(", price=").append(price);
        sb.append(", deposit=").append(deposit);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", roomType=").append(roomType);
        sb.append(", area=").append(area);
        sb.append(", floor=").append(floor);
        sb.append(", orientation=").append(orientation);
        sb.append(", decoration=").append(decoration);
        sb.append(", availableDate=").append(availableDate);
        sb.append(", metro=").append(metro);
        sb.append(", description=").append(description);
        sb.append(", rentType=").append(rentType);
        sb.append(", status=").append(status);
        sb.append(", isSmartLockSupported=").append(isSmartLockSupported);
        sb.append(", isSelfViewingSupported=").append(isSelfViewingSupported);
        sb.append(", landlordId=").append(landlordId);
        sb.append(", viewCount=").append(viewCount);
        sb.append(", favoriteCount=").append(favoriteCount);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}