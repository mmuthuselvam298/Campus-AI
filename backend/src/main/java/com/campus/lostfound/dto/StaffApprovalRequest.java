package com.campus.lostfound.dto;

import com.campus.lostfound.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class StaffApprovalRequest {
    @NotNull(message = "Decision status is required (ACTIVE or REJECTED)")
    private AccountStatus status;

    public StaffApprovalRequest() {
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
