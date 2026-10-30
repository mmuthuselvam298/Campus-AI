package com.campus.lostfound.model;

import java.time.Instant;

public class OtpToken {
    private String id;
    private String email;
    private String otpHash;
    private String salt;
    private int attempts = 0;
    private int maxAttempts = 5;
    private Instant expiresAt;
    private boolean consumed = false;
    private Instant createdAt;

    public OtpToken() {
    }

    public OtpToken(String id, String email, String otpHash, String salt, int attempts, int maxAttempts, Instant expiresAt, boolean consumed, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.otpHash = otpHash;
        this.salt = salt;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.expiresAt = expiresAt;
        this.consumed = consumed;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
