package com.cjw;

import com.collab.workspace.WorkspaceApplication;
import com.collab.workspace.dto.LoginRequest;
import com.collab.workspace.dto.SignupRequest;
import com.collab.workspace.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WorkspaceApplication.class)
class CollaborativeJavaWorkspaceBackendApplicationTests {

    @Autowired
    private AuthService authService;

    @Test
    void contextLoads() {
        assertThat(authService).isNotNull();
    }

    @Test
    void testAuthSignupAndLogin() {
        SignupRequest signup = new SignupRequest();
        signup.setName("Test User");
        signup.setEmail("test.user@example.com");
        signup.setPassword("password123");

        var signupResponse = authService.signup(signup);
        assertThat(signupResponse).isNotNull();
        assertThat(signupResponse.getToken()).isNotBlank();
        assertThat(signupResponse.getEmail()).isEqualTo("test.user@example.com");

        LoginRequest login = new LoginRequest();
        login.setEmail("test.user@example.com");
        login.setPassword("password123");

        var loginResponse = authService.login(login);
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getToken()).isNotBlank();
        assertThat(loginResponse.getName()).isEqualTo("Test User");
    }
}