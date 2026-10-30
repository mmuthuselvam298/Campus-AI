package com.campus.lostfound.model;

import java.time.Instant;

public class Claim {
    private String id;
    private String itemId;
    private String claimantUserId;
    private String claimantName;
    private String claimantEmail;
    private String claimMessage;
    private String proofDetails;
    private ClaimStatus status = ClaimStatus.PENDING;
    private String resolutionNotes;
    private String resolvedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Claim() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getClaimantUserId() {
        return claimantUserId;
    }

    public void setClaimantUserId(String claimantUserId) {
        this.claimantUserId = claimantUserId;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public String getClaimantEmail() {
        return claimantEmail;
    }

    public void setClaimantEmail(String claimantEmail) {
        this.claimantEmail = claimantEmail;
    }

    public String getClaimMessage() {
        return claimMessage;
    }

    public void setClaimMessage(String claimMessage) {
        this.claimMessage = claimMessage;
    }

    public String getProofDetails() {
        return proofDetails;
    }

    public void setProofDetails(String proofDetails) {
        this.proofDetails = proofDetails;
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

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
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
}
