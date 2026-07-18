package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminContractServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminContractServiceTests {
    private final RentContractMapper mapper = mock(RentContractMapper.class);
    private final UserService userService = mock(UserService.class);
    private final EsignV3Client esignClient = mock(EsignV3Client.class);
    private final AdminContractServiceImpl service = new AdminContractServiceImpl(mapper, userService, esignClient);

    @BeforeEach
    void setUp() {
        User admin = new User(); admin.setId("admin-1"); admin.setRole("ADMIN");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_shouldReturnAllContractSnapshots() {
        RentContract contract = new RentContract();
        contract.setId("contract-1"); contract.setContractNo("CT202607180001"); contract.setStatus("signing");
        contract.setTenantName("测试租客"); contract.setLandlordName("测试房东");
        contract.setContractFileId("file-1"); contract.setCreatedAt(LocalDateTime.now());
        Page<RentContract> result = new Page<>(1, 20, 1); result.setRecords(List.of(contract));
        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(result);

        var page = service.list("admin-1", "signing", "测试", 1, 20);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).contractNo()).isEqualTo("CT202607180001");
        assertThat(page.items().get(0).hasContractFile()).isTrue();
    }

    @Test
    void list_shouldRejectUnsupportedStatus() {
        assertThatThrownBy(() -> service.list("admin-1", "unknown", null, 1, 20))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不支持的合同状态");
    }

    @Test
    void landlord_shouldNotSeeAllContracts() {
        User landlord = new User(); landlord.setId("landlord-1"); landlord.setRole("LANDLORD");
        when(userService.requireActiveUser("landlord-1")).thenReturn(landlord);
        assertThatThrownBy(() -> service.list("landlord-1", null, null, 1, 20))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无权查看合同");
    }
}
