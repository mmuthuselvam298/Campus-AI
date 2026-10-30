package com.campus.lostfound.dto;

import com.campus.lostfound.model.Claim;
import com.campus.lostfound.model.ClaimStatus;

import java.time.Instant;

public class ClaimResponse {
    private String id;
    private String itemId;
    private String claimantUserId;
    private String claimantName;
    private String claimantEmail;
    private String claimMessage;
    private String proofDetails;
    private ClaimStatus status;
    private String resolutionNotes;
    private String resolvedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private ItemResponse item;

    public ClaimResponse() {
    }

    public static ClaimResponse fromClaim(Claim claim, boolean isAdmin) {
        if (claim == null) return null;
        ClaimResponse resp = new ClaimResponse();
        resp.setId(claim.getId());
        resp.setItemId(claim.getItemId());
        resp.setClaimantUserId(claim.getClaimantUserId());
        resp.setClaimMessage(claim.getClaimMessage());
        resp.setStatus(claim.getStatus());
        resp.setResolutionNotes(claim.getResolutionNotes());
        resp.setResolvedBy(claim.getResolvedBy());
        resp.setCreatedAt(claim.getCreatedAt());
        resp.setUpdatedAt(claim.getUpdatedAt());

        if (isAdmin) {
            resp.setClaimantName(claim.getClaimantName());
            resp.setClaimantEmail(claim.getClaimantEmail());
            resp.setProofDetails(claim.getProofDetails());
        } else {
            // Mask claimant private details for public view
            resp.setClaimantName(maskName(claim.getClaimantName()));
            resp.setClaimantEmail(maskEmail(claim.getClaimantEmail()));
            resp.setProofDetails("[Protected Proof Submitted]");
        }
        return resp;
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Anonymous";
        return name.charAt(0) + "***";
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int idx = email.indexOf('@');
        return email.substring(0, Math.min(2, idx)) + "***" + email.substring(idx);
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

    public ItemResponse getItem() {
        return item;
    }

    public void setItem(ItemResponse item) {
        this.item = item;
    }
}
