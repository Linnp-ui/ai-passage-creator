package com.yupi.template.controller;

import com.mybatisflex.core.paginate.Page;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.exception.GlobalExceptionHandler;
import com.yupi.template.model.entity.PaymentRecord;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.vo.StatisticsVO;
import com.yupi.template.service.LocalImageStorageService;
import com.yupi.template.service.PaymentService;
import com.yupi.template.service.StatisticsService;
import com.yupi.template.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MiscControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private UserService userService;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private LocalImageStorageService localImageStorageService;

    @InjectMocks
    private PaymentController paymentController;

    @InjectMocks
    private StatisticsController statisticsController;

    @InjectMocks
    private HealthController healthController;

    @InjectMocks
    private ImageController imageController;

    @InjectMocks
    private StripeWebhookController stripeWebhookController;

    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        paymentController,
                        statisticsController,
                        healthController,
                        imageController,
                        stripeWebhookController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        lenient().when(localImageStorageService.getStoragePath()).thenReturn(tempDir);
    }

    @Test
    void healthEndpointShouldReturnOkForBothPaths() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("ok"));

        mockMvc.perform(get("/health/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void createVipPaymentSessionShouldReturnSessionUrl() throws Exception {
        User loginUser = User.builder().id(21L).build();
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(paymentService.createVipPaymentSession(21L)).thenReturn("https://pay.example/session");

        mockMvc.perform(post("/payment/create-vip-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("https://pay.example/session"));
    }

    @Test
    void createVipPaymentSessionShouldWrapUnexpectedErrors() throws Exception {
        User loginUser = User.builder().id(22L).build();
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(paymentService.createVipPaymentSession(22L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/payment/create-vip-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("创建支付会话失败"));
    }

    @Test
    void refundShouldReturnSuccess() throws Exception {
        User loginUser = User.builder().id(23L).build();
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(paymentService.handleRefund(23L, "duplicate")).thenReturn(true);

        mockMvc.perform(post("/payment/refund").param("reason", "duplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void refundShouldWrapUnexpectedErrors() throws Exception {
        User loginUser = User.builder().id(24L).build();
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(paymentService.handleRefund(24L, null)).thenThrow(new RuntimeException("refund-error"));

        mockMvc.perform(post("/payment/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("退款失败"));
    }

    @Test
    void getPaymentRecordsShouldReturnCurrentUserRecords() throws Exception {
        User loginUser = User.builder().id(25L).build();
        PaymentRecord record = PaymentRecord.builder()
                .id(1L)
                .amount(BigDecimal.TEN)
                .status("SUCCEEDED")
                .build();
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(paymentService.getPaymentRecords(25L)).thenReturn(List.of(record));

        mockMvc.perform(get("/payment/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].status").value("SUCCEEDED"));
    }

    @Test
    void statisticsOverviewShouldReturnAggregatedData() throws Exception {
        StatisticsVO statisticsVO = StatisticsVO.builder()
                .todayCount(3L)
                .totalCount(12L)
                .successRate(98.5)
                .build();
        when(statisticsService.getStatistics()).thenReturn(statisticsVO);

        mockMvc.perform(get("/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(12))
                .andExpect(jsonPath("$.data.successRate").value(98.5));
    }

    @Test
    void imageControllerShouldReturnStoredImageBytes() throws Exception {
        Path folder = Files.createDirectories(tempDir.resolve("covers"));
        Path imageFile = folder.resolve("ok.png");
        byte[] bytes = new byte[]{1, 2, 3};
        Files.write(imageFile, bytes);

        mockMvc.perform(get("/images/covers/ok.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=86400"))
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void imageControllerShouldReturnNotFoundWhenFileMissing() throws Exception {
        mockMvc.perform(get("/images/covers/missing.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void imageControllerShouldRejectPathTraversal() throws Exception {
        mockMvc.perform(get("/images/../evil.png"))
                .andExpect(status().isForbidden());
    }

    @Test
    void imageControllerShouldReturnServerErrorWhenPathIsDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("broken"));

        mockMvc.perform(get("/images/broken/folder.png"))
                .andExpect(status().isNotFound());

        Files.createDirectories(tempDir.resolve("dir"));
        Files.createDirectories(tempDir.resolve("dir").resolve("subdir.png"));

        mockMvc.perform(get("/images/dir/subdir.png"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void stripeWebhookShouldHandleCompletedCheckout() throws Exception {
        Event event = org.mockito.Mockito.mock(Event.class);
        EventDataObjectDeserializer deserializer = org.mockito.Mockito.mock(EventDataObjectDeserializer.class);
        Session session = org.mockito.Mockito.mock(Session.class);
        when(paymentService.constructEvent("payload", "sig")).thenReturn(event);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        mockMvc.perform(post("/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("payload")
                        .header("Stripe-Signature", "sig"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService).handlePaymentSuccess(session);
    }

    @Test
    void stripeWebhookShouldIgnoreUnhandledEventTypes() throws Exception {
        Event event = org.mockito.Mockito.mock(Event.class);
        when(paymentService.constructEvent("payload", "sig")).thenReturn(event);
        when(event.getType()).thenReturn("invoice.created");

        mockMvc.perform(post("/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("payload")
                        .header("Stripe-Signature", "sig"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService, never()).handlePaymentSuccess(any(Session.class));
    }

    @Test
    void stripeWebhookShouldReturnErrorOnSignatureFailure() throws Exception {
        when(paymentService.constructEvent("payload", "sig"))
                .thenThrow(new BusinessException(ErrorCode.SYSTEM_ERROR, "invalid signature"));

        mockMvc.perform(post("/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("payload")
                        .header("Stripe-Signature", "sig"))
                .andExpect(status().isOk())
                .andExpect(content().string("error"));
    }
}
