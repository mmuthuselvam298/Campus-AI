package com.campus.lostfound.dto;

import com.campus.lostfound.model.AccountStatus;
import com.campus.lostfound.model.User;

public class AuthUserDto {
    private String id;
    private String fullName;
    private String campusEmail;
    private String role;
    private AccountStatus accountStatus;
    private String requestedRole;
    private boolean emailVerified;

    public AuthUserDto() {
    }

    public static AuthUserDto fromUser(User user) {
        if (user == null) return null;
        AuthUserDto dto = new AuthUserDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setCampusEmail(user.getCampusEmail());
        dto.setRole(user.getRole());
        dto.setAccountStatus(user.getAccountStatus());
        dto.setRequestedRole(user.getRequestedRole());
        dto.setEmailVerified(user.isEmailVerified());
        return dto;
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
}
