package com.campus.lostfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyCodeRequest {
    @NotBlank(message = "Campus email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be exactly 6 digits")
    private String code;

    private String fullName;
    private String role; // "STUDENT" or "STAFF" (used only if creating new user account)

    public VerifyCodeRequest() {
    }

    public VerifyCodeRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public VerifyCodeRequest(String email, String code, String role, String fullName) {
        this.email = email;
        this.code = code;
        this.role = role;
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
