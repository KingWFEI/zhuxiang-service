package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.HouseRoomTypeDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HouseRoomType;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.AdminHouseRoomTypeService;
import com.zhuxiang.service.service.HouseRoomTypeService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminHouseRoomTypeServiceImpl implements AdminHouseRoomTypeService {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "HOUSEKEEPER");

    private final HouseRoomTypeService roomTypeService;
    private final HouseService houseService;
    private final UserService userService;

    public AdminHouseRoomTypeServiceImpl(
            HouseRoomTypeService roomTypeService,
            HouseService houseService,
            UserService userService
    ) {
        this.roomTypeService = roomTypeService;
        this.houseService = houseService;
        this.userService = userService;
    }

    @Override
    public List<HouseRoomTypeDtos.Item> list(String operatorId) {
        requireRole(operatorId);
        return roomTypeService.list(
                        Wrappers.<HouseRoomType>lambdaQuery()
                                .orderByAsc(HouseRoomType::getSortOrder)
                                .orderByAsc(HouseRoomType::getName))
                .stream().map(this::toItem).toList();
    }

    @Override
    public HouseRoomTypeDtos.Item create(HouseRoomTypeDtos.CreateRequest request, String operatorId) {
        requireRole(operatorId);
        String name = request.name().trim();
        ensureNameAvailable(name, null);
        LocalDateTime now = LocalDateTime.now();
        HouseRoomType roomType = new HouseRoomType();
        roomType.setId(UUID.randomUUID().toString());
        roomType.setName(name);
        roomType.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        roomType.setEnabled(Boolean.FALSE.equals(request.enabled()) ? 0 : 1);
        roomType.setCreatedAt(now);
        roomType.setUpdatedAt(now);
        if (!roomTypeService.save(roomType)) {
            throw BusinessException.badRequest("户型创建失败");
        }
        return toItem(roomType);
    }

    @Override
    public HouseRoomTypeDtos.Item update(
            String id,
            HouseRoomTypeDtos.UpdateRequest request,
            String operatorId
    ) {
        requireRole(operatorId);
        HouseRoomType roomType = requireRoomType(id);
        String name = request.name().trim();
        ensureNameAvailable(name, id);
        if (!name.equals(roomType.getName()) && houseService.count(
                Wrappers.<House>lambdaQuery().eq(House::getRoomType, roomType.getName())) > 0) {
            throw BusinessException.conflict("该户型已被房源使用，不能修改名称；可停用后新增户型");
        }
        roomType.setName(name);
        roomType.setSortOrder(request.sortOrder());
        roomType.setEnabled(request.enabled() ? 1 : 0);
        roomType.setUpdatedAt(LocalDateTime.now());
        if (!roomTypeService.updateById(roomType)) {
            throw BusinessException.badRequest("户型更新失败");
        }
        return toItem(roomType);
    }

    @Override
    @Transactional
    public void delete(String id, String operatorId) {
        requireRole(operatorId);
        HouseRoomType roomType = requireRoomType(id);
        if (houseService.count(
                Wrappers.<House>lambdaQuery().eq(House::getRoomType, roomType.getName())) > 0) {
            throw BusinessException.conflict("该户型仍被房源使用，请改为停用");
        }
        if (!roomTypeService.removeById(id)) {
            throw BusinessException.badRequest("户型删除失败");
        }
    }

    private void requireRole(String operatorId) {
        User user = userService.requireActiveUser(operatorId);
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            throw BusinessException.forbidden("当前账号无权配置户型");
        }
    }

    private HouseRoomType requireRoomType(String id) {
        HouseRoomType roomType = roomTypeService.getById(id);
        if (roomType == null) {
            throw BusinessException.notFound("户型不存在");
        }
        return roomType;
    }

    private void ensureNameAvailable(String name, String excludeId) {
        if (roomTypeService.count(Wrappers.<HouseRoomType>lambdaQuery()
                .eq(HouseRoomType::getName, name)
                .ne(excludeId != null, HouseRoomType::getId, excludeId)) > 0) {
            throw BusinessException.conflict("户型名称已存在");
        }
    }

    private HouseRoomTypeDtos.Item toItem(HouseRoomType roomType) {
        return new HouseRoomTypeDtos.Item(
                roomType.getId(), roomType.getName(), roomType.getSortOrder(),
                Integer.valueOf(1).equals(roomType.getEnabled()));
    }
}
