package com.campus.lostfound.controller;

import com.campus.lostfound.dto.ApiResponse;
import com.campus.lostfound.dto.AuthUserDto;
import com.campus.lostfound.dto.RequestCodeRequest;
import com.campus.lostfound.dto.RequestCodeResponse;
import com.campus.lostfound.dto.VerifyCodeRequest;
import com.campus.lostfound.model.User;
import com.campus.lostfound.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/request-code")
    public ResponseEntity<ApiResponse<RequestCodeResponse>> requestCode(@Valid @RequestBody RequestCodeRequest req) throws SQLException {
        RequestCodeResponse response = authService.requestCode(req);
        return ResponseEntity.ok(ApiResponse.ok(response.getMessage(), response));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<AuthUserDto>> verifyCode(
            @Valid @RequestBody VerifyCodeRequest req,
            HttpServletRequest request) throws SQLException {

        AuthUserDto user = authService.verifyCode(req, request);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", user));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserDto>> getCurrentUser(HttpServletRequest request) throws SQLException {
        User user = authService.getAuthenticatedUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No active authenticated session found. Please sign in."));
        }
        return ResponseEntity.ok(ApiResponse.ok(AuthUserDto.fromUser(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Session signed out successfully", null));
    }
}
