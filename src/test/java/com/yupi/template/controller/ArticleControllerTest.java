package com.yupi.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.yupi.template.exception.GlobalExceptionHandler;
import com.yupi.template.manager.SseEmitterManager;
import com.yupi.template.model.dto.article.ArticleAiModifyOutlineRequest;
import com.yupi.template.model.dto.article.ArticleConfirmOutlineRequest;
import com.yupi.template.model.dto.article.ArticleConfirmTitleRequest;
import com.yupi.template.model.dto.article.ArticleCreateRequest;
import com.yupi.template.model.dto.article.ArticleQueryRequest;
import com.yupi.template.model.dto.article.ArticleState;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.vo.AgentExecutionStats;
import com.yupi.template.model.vo.ArticleVO;
import com.yupi.template.service.AgentLogService;
import com.yupi.template.service.ArticleAsyncService;
import com.yupi.template.service.ArticleResumeService;
import com.yupi.template.service.ArticleService;
import com.yupi.template.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ArticleService articleService;

    @Mock
    private ArticleAsyncService articleAsyncService;

    @Mock
    private SseEmitterManager sseEmitterManager;

    @Mock
    private UserService userService;

    @Mock
    private AgentLogService agentLogService;

    @Mock
    private ArticleResumeService articleResumeService;

    @InjectMocks
    private ArticleController articleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(articleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createArticleShouldReturnTaskIdAndTriggerPhase1() throws Exception {
        User loginUser = User.builder().id(1L).build();
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTopic("AI 测试");
        request.setStyle("tech");
        request.setEnabledImageMethods(List.of("PEXELS", "UNSPLASH"));
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(articleService.createArticleTaskWithQuotaCheck(
                eq("AI 测试"), eq("tech"), eq(List.of("PEXELS", "UNSPLASH")), eq(loginUser)))
                .thenReturn("task-1");

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("task-1"))
                .andExpect(jsonPath("$.message").value("ok"));

        verify(articleAsyncService).executePhase1("task-1", "AI 测试", "tech");
    }

    @Test
    void createArticleShouldRejectBlankTopic() throws Exception {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTopic("   ");

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("选题不能为空"));
    }

    @Test
    void getProgressShouldReturnEmitterAfterPermissionCheck() throws Exception {
        User loginUser = User.builder().id(1L).build();
        ArticleVO articleVO = new ArticleVO();
        SseEmitter emitter = new SseEmitter();
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(articleService.getArticleDetail("task-2", loginUser)).thenReturn(articleVO);
        when(sseEmitterManager.createEmitter("task-2")).thenReturn(emitter);

        mockMvc.perform(get("/article/progress/task-2"))
                .andExpect(status().isOk());

        verify(articleService).getArticleDetail("task-2", loginUser);
        verify(sseEmitterManager).createEmitter("task-2");
    }

    @Test
    void listArticleShouldReturnPagedResult() throws Exception {
        User loginUser = User.builder().id(2L).build();
        ArticleQueryRequest request = new ArticleQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        Page<ArticleVO> page = new Page<>(1, 10, 1);
        ArticleVO articleVO = new ArticleVO();
        articleVO.setTaskId("task-list");
        page.setRecords(List.of(articleVO));
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(articleService.listArticleByPage(any(ArticleQueryRequest.class), eq(loginUser))).thenReturn(page);

        mockMvc.perform(post("/article/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].taskId").value("task-list"));
    }

    @Test
    void deleteArticleShouldRejectMissingId() throws Exception {
        mockMvc.perform(post("/article/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void confirmTitleShouldRejectMissingMainTitle() throws Exception {
        ArticleConfirmTitleRequest request = new ArticleConfirmTitleRequest();
        request.setTaskId("task-3");
        request.setSelectedSubTitle("副标题");

        mockMvc.perform(post("/article/confirm-title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("主标题不能为空"));
    }

    @Test
    void confirmOutlineShouldRejectEmptyOutline() throws Exception {
        ArticleConfirmOutlineRequest request = new ArticleConfirmOutlineRequest();
        request.setTaskId("task-4");
        request.setOutline(List.of());

        mockMvc.perform(post("/article/confirm-outline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("大纲不能为空"));
    }

    @Test
    void aiModifyOutlineShouldReturnModifiedOutline() throws Exception {
        User loginUser = User.builder().id(3L).build();
        ArticleAiModifyOutlineRequest request = new ArticleAiModifyOutlineRequest();
        request.setTaskId("task-5");
        request.setModifySuggestion("增强结论部分");
        ArticleState.OutlineSection section = new ArticleState.OutlineSection();
        section.setSection(1);
        section.setTitle("修改后大纲");
        section.setPoints(List.of("点1"));
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(articleService.aiModifyOutline("task-5", "增强结论部分", loginUser)).thenReturn(List.of(section));

        mockMvc.perform(post("/article/ai-modify-outline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].title").value("修改后大纲"));
    }

    @Test
    void getExecutionLogsShouldReturnStats() throws Exception {
        AgentExecutionStats stats = AgentExecutionStats.builder()
                .taskId("task-6")
                .totalDurationMs(1200)
                .overallStatus("SUCCESS")
                .build();
        when(agentLogService.getExecutionStats("task-6")).thenReturn(stats);

        mockMvc.perform(get("/article/execution-logs/task-6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.taskId").value("task-6"));
    }

    @Test
    void checkResumeStatusShouldReturnResumeInfo() throws Exception {
        User loginUser = User.builder().id(4L).build();
        ArticleResumeService.ResumeInfo resumeInfo = new ArticleResumeService.ResumeInfo();
        resumeInfo.setCanResume(true);
        resumeInfo.setCurrentPhase("CONTENT_GENERATING");
        resumeInfo.setProgressPercent(65);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(articleResumeService.getResumeInfo("task-7", loginUser)).thenReturn(resumeInfo);

        mockMvc.perform(get("/article/resume/check/task-7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.canResume").value(true))
                .andExpect(jsonPath("$.data.progressPercent").value(65));
    }

    @Test
    void resumeArticleShouldReturnResult() throws Exception {
        User loginUser = User.builder().id(5L).build();
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(articleResumeService.resumeArticle("task-8", loginUser)).thenReturn(true);

        mockMvc.perform(post("/article/resume/task-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
