package com.yupi.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yupi.template.exception.GlobalExceptionHandler;
import com.yupi.template.model.dto.user.UserAddRequest;
import com.yupi.template.model.dto.user.UserLoginRequest;
import com.yupi.template.model.dto.user.UserQueryRequest;
import com.yupi.template.model.dto.user.UserRegisterRequest;
import com.yupi.template.model.dto.user.UserUpdateRequest;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.vo.LoginUserVO;
import com.yupi.template.model.vo.UserVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void userRegisterShouldReturnUserId() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount("tester");
        request.setUserPassword("12345678");
        request.setCheckPassword("12345678");
        when(userService.userRegister("tester", "12345678", "12345678")).thenReturn(101L);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(101L));
    }

    @Test
    void userRegisterShouldRejectNullBody() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void userLoginShouldReturnLoginUserVo() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserAccount("tester");
        request.setUserPassword("12345678");
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(1L);
        loginUserVO.setUserAccount("tester");
        when(userService.userLogin(eq("tester"), eq("12345678"), any())).thenReturn(loginUserVO);

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userAccount").value("tester"));
    }

    @Test
    void getLoginUserShouldReturnCurrentUser() throws Exception {
        User loginUser = User.builder().id(2L).userAccount("login-user").build();
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(2L);
        loginUserVO.setUserAccount("login-user");
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(userService.getLoginUserVO(loginUser)).thenReturn(loginUserVO);

        mockMvc.perform(get("/user/get/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(2L));
    }

    @Test
    void userLogoutShouldReturnTrue() throws Exception {
        when(userService.userLogout(any())).thenReturn(true);

        mockMvc.perform(post("/user/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void addUserShouldReturnGeneratedId() throws Exception {
        UserAddRequest request = new UserAddRequest();
        request.setUserAccount("new-user");
        request.setUserName("New User");
        when(userService.getEncryptPassword("12345678")).thenReturn("encrypted");
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(88L);
            return true;
        });

        mockMvc.perform(post("/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(88L));
    }

    @Test
    void getUserByIdShouldReturnUser() throws Exception {
        User user = User.builder().id(9L).userAccount("admin-view").build();
        when(userService.getById(9L)).thenReturn(user);

        mockMvc.perform(get("/user/get").param("id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userAccount").value("admin-view"));
    }

    @Test
    void getUserVoByIdShouldReturnConvertedVo() throws Exception {
        User user = User.builder().id(10L).userAccount("target-user").build();
        UserVO userVO = new UserVO();
        userVO.setId(10L);
        userVO.setUserAccount("target-user");
        when(userService.getById(10L)).thenReturn(user);
        when(userService.getUserVO(user)).thenReturn(userVO);

        mockMvc.perform(get("/user/get/vo").param("id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userAccount").value("target-user"));
    }

    @Test
    void deleteUserShouldRejectInvalidId() throws Exception {
        mockMvc.perform(post("/user/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void updateUserShouldRejectMissingId() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUserName("nobody");

        mockMvc.perform(post("/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void listUserVoByPageShouldReturnPagedUsers() throws Exception {
        UserQueryRequest request = new UserQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        Page<User> userPage = new Page<>(1, 10, 1);
        userPage.setRecords(List.of(User.builder().id(11L).userAccount("page-user").build()));
        UserVO userVO = new UserVO();
        userVO.setId(11L);
        userVO.setUserAccount("page-user");
        when(userService.getQueryWrapper(any(UserQueryRequest.class))).thenReturn(null);
        when(userService.page(any(Page.class), eq((QueryWrapper) null))).thenReturn(userPage);
        when(userService.getUserVOList(any())).thenReturn(List.of(userVO));

        mockMvc.perform(post("/user/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("page-user"));

        verify(userService).getQueryWrapper(any(UserQueryRequest.class));
    }
}
