package com.campus.lostfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RequestCodeRequest {
    @NotBlank(message = "Campus email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    private String fullName;
    private String role; // "STUDENT" or "STAFF"

    public RequestCodeRequest() {
    }

    public RequestCodeRequest(String email) {
        this.email = email;
    }

    public RequestCodeRequest(String email, String fullName, String role) {
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
