package com.campus.lostfound.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestCodeResponse {

    private boolean devMode;
    private String devOtp;
    private boolean emailSent;
    private String message;
    private int cooldownSeconds;
    private int expiryMinutes;

    public RequestCodeResponse() {
    }

    public RequestCodeResponse(boolean devMode, String devOtp, boolean emailSent, String message, int cooldownSeconds, int expiryMinutes) {
        this.devMode = devMode;
        this.devOtp = devOtp;
        this.emailSent = emailSent;
        this.message = message;
        this.cooldownSeconds = cooldownSeconds;
        this.expiryMinutes = expiryMinutes;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public String getDevOtp() {
        return devOtp;
    }

    public void setDevOtp(String devOtp) {
        this.devOtp = devOtp;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
}
