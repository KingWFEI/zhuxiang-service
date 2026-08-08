package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.dto.LandlordAuthDtos;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.LandlordAuthApplication;
import com.zhuxiang.service.entity.LandlordAuthProof;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LandlordAuthApplicationMapper;
import com.zhuxiang.service.mapper.LandlordAuthProofMapper;
import com.zhuxiang.service.mapper.LandlordMapper;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.IdCardCryptoService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.impl.LandlordAuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LandlordAuthServiceTests {
    private final LandlordAuthApplicationMapper applicationMapper = mock(LandlordAuthApplicationMapper.class);
    private final LandlordAuthProofMapper proofMapper = mock(LandlordAuthProofMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final LandlordMapper landlordMapper = mock(LandlordMapper.class);
    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final IdCardCryptoService cryptoService = mock(IdCardCryptoService.class);
    private final LandlordService landlordService = mock(LandlordService.class);
    private final MessageService messageService = mock(MessageService.class);
    private LandlordAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LandlordAuthServiceImpl(
                applicationMapper, proofMapper, userMapper, landlordMapper,
                fileRecordService, cryptoService, landlordService, messageService
        );
    }

    @Test
    void submitEncryptsIdCardAndPersistsAtLeastOneOwnedProof() {
        User tenant = user("tenant-1", "TENANT");
        when(userMapper.selectById("tenant-1")).thenReturn(tenant);
        when(applicationMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(cryptoService.encrypt(anyString())).thenReturn("v1:ciphertext");
        when(cryptoService.mask(anyString())).thenReturn("500101********1234");
        when(proofMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.submit("tenant-1", new LandlordAuthDtos.SubmitRequest(
                "张三", "500101199001011234", "https://cos/front.jpg", "https://cos/back.jpg",
                List.of(new LandlordAuthDtos.ProofRequest(
                        "PROPERTY_CERTIFICATE", "file-1", "https://cos/property.jpg"
                )),
                "13800000000", "wx", null, "重庆市", "工作日18点后", null, false
        ));

        ArgumentCaptor<LandlordAuthApplication> captor =
                ArgumentCaptor.forClass(LandlordAuthApplication.class);
        verify(applicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getIdCardCiphertext()).isEqualTo("v1:ciphertext");
        assertThat(captor.getValue().getIdCardMasked()).isEqualTo("500101********1234");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        verify(fileRecordService).validateFileOwnership(
                "tenant-1", "file-1", "https://cos/property.jpg", "landlord_proof_property"
        );
        verify(proofMapper).insert(any(LandlordAuthProof.class));
    }

    @Test
    void approvalPromotesTenantCreatesProfileAndSendsRealtimeMessage() {
        User operator = user("admin-1", "ADMIN");
        User tenant = user("tenant-1", "TENANT");
        when(userMapper.selectById(anyString())).thenAnswer(invocation ->
                "admin-1".equals(invocation.getArgument(0)) ? operator : tenant
        );
        LandlordAuthApplication application = new LandlordAuthApplication();
        application.setId("application-1");
        application.setApplicationNo("LDA001");
        application.setUserId("tenant-1");
        application.setStatus("PENDING");
        application.setRealName("张三");
        application.setContactPhone("13800000000");
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        when(applicationMapper.selectById("application-1")).thenReturn(application);
        when(applicationMapper.reviewPending(
                eq("application-1"), eq("APPROVED"), eq(null),
                eq("admin-1"), any(LocalDateTime.class)
        )).thenReturn(1);
        when(proofMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        Landlord profile = new Landlord();
        profile.setId("profile-1");
        profile.setUserId("tenant-1");
        when(landlordService.ensureProfile(tenant)).thenReturn(profile);

        LandlordAuthDtos.ApplicationView result = service.review(
                "admin-1", "application-1",
                new LandlordAuthDtos.ReviewRequest("APPROVED", null)
        );

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(tenant.getRole()).isEqualTo("LANDLORD");
        verify(userMapper).updateById(tenant);
        verify(landlordMapper).updateById(profile);
        verify(messageService).sendMessage(
                eq("tenant-1"), eq("system"), eq("房东认证已通过"),
                anyString(), eq("route"), eq("/landlord/workbench")
        );
    }

    private User user(String id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus("active");
        user.setPhone("13800000000");
        return user;
    }
}
