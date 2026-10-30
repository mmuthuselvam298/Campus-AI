package com.campus.lostfound.model;

import java.time.Instant;

public class User {
    private String id;
    private String fullName;
    private String campusEmail;
    private String role = "STUDENT";
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    private String requestedRole;
    private boolean emailVerified = false;
    private String approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {
    }

    public User(String id, String fullName, String campusEmail, String role, AccountStatus accountStatus, String requestedRole, boolean emailVerified, String approvedBy, Instant approvedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.campusEmail = campusEmail;
        this.role = role;
        this.accountStatus = accountStatus != null ? accountStatus : AccountStatus.ACTIVE;
        this.requestedRole = requestedRole;
        this.emailVerified = emailVerified;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCampusEmail() {
        return campusEmail;
    }

    public void setCampusEmail(String campusEmail) {
        this.campusEmail = campusEmail;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isStaff() {
        return "STAFF".equalsIgnoreCase(role);
    }

    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }
}
