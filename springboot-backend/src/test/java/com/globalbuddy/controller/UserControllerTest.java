package com.globalbuddy.controller;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.UserFollow;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.repository.CommentRepository;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.PostLikeRepository;
import com.globalbuddy.repository.UserFollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import com.globalbuddy.security.JwtService;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private CommunityPostRepository postRepository;

    @MockBean
    private PostLikeRepository postLikeRepository;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private UserFollowRepository userFollowRepository;

    // PostController is injected into UserController; mock it here
    @MockBean
    private PostController postController;

    // Some security components may still be picked up; provide JwtService to satisfy wiring.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private AppUser currentUser;

    @BeforeEach
    void setUpSecurityContext() {
        currentUser = AppUser.create("alice", "alice@example.com", "hash", "Alice", "en");
        currentUser.setId("u-1");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("UTC-32: getMyProfile() returns authenticated user profile as JSON")
    void utc32_getMyProfile_returnsProfileJson() throws Exception {
        // Arrange: followers count and mutual follows
        when(userFollowRepository.countByFollowing(currentUser)).thenReturn(2L);

        AppUser bob = AppUser.create("bob", "bob@example.com", "hash", "Bob", "en");
        bob.setId("u-2");
        AppUser carol = AppUser.create("carol", "carol@example.com", "hash", "Carol", "en");
        carol.setId("u-3");

        UserFollow u1FollowsBob = UserFollow.builder().follower(currentUser).following(bob).build();
        UserFollow u1FollowsCarol = UserFollow.builder().follower(currentUser).following(carol).build();
        UserFollow bobFollowsU1 = UserFollow.builder().follower(bob).following(currentUser).build();

        when(userFollowRepository.findAll()).thenReturn(List.of(
                u1FollowsBob,
                u1FollowsCarol,
                bobFollowsU1
        ));

        // Act & Assert
        mockMvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON))
                .andDo(print()) // 打印出完整 JSON 响应，方便对照测试计划中的 Expected Result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("u-1"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.displayName").value("Alice"))
                .andExpect(jsonPath("$.data.preferredLanguage").value("en"))
                // followersCount 从仓库 mock 中来
                .andExpect(jsonPath("$.data.followersCount").value(2))
                // mutualFollowsCount 只计算与当前用户互相关注的人（这里是 Bob 1 个）
                .andExpect(jsonPath("$.data.mutualFollowsCount").value(1))
                // 不应该包含密码等敏感字段
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("UTC-33: getMutualFollows() 返回互相关注列表并支持 q 过滤")
    void utc33_getMutualFollows_returnsFilteredList() throws Exception {
        // Arrange: 构造当前用户 A、好友 B 和 C
        AppUser userA = currentUser;
        AppUser userB = AppUser.create("bob", "bob@example.com", "hash", "Bob", "en");
        userB.setId("uB");
        AppUser userC = AppUser.create("carol", "carol@example.com", "hash", "Carol", "en");
        userC.setId("uC");

        // A follows B & C
        UserFollow aFollowsB = UserFollow.builder().follower(userA).following(userB).build();
        UserFollow aFollowsC = UserFollow.builder().follower(userA).following(userC).build();
        // B & C follow A
        UserFollow bFollowsA = UserFollow.builder().follower(userB).following(userA).build();
        UserFollow cFollowsA = UserFollow.builder().follower(userC).following(userA).build();

        when(userFollowRepository.findAll()).thenReturn(List.of(
                aFollowsB, aFollowsC, bFollowsA, cFollowsA
        ));

        // Act & Assert 1: q 为空，返回 B 和 C
        mockMvc.perform(get("/api/users/mutual-follows")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].displayName", containsInAnyOrder("Bob", "Carol")));

        // Act & Assert 2: q = "bo"，只匹配 Bob
        mockMvc.perform(get("/api/users/mutual-follows")
                        .param("q", "bo")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value("uB"))
                .andExpect(jsonPath("$.data[0].displayName").value("Bob"));
    }
}


