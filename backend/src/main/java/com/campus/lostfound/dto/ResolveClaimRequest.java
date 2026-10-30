package com.campus.lostfound.dto;

import com.campus.lostfound.model.ClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResolveClaimRequest {
    @NotNull(message = "Decision status is required (APPROVED or REJECTED or RESOLVED)")
    private ClaimStatus status;

    private String resolutionNotes;

    @NotBlank(message = "Admin actor name is required")
    private String adminActor;

    public ResolveClaimRequest() {
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getAdminActor() {
        return adminActor;
    }

    public void setAdminActor(String adminActor) {
        this.adminActor = adminActor;
    }
}
