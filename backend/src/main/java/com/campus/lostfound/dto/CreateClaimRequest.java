package com.campus.lostfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateClaimRequest {
    @NotBlank(message = "Item ID is required")
    private String itemId;

    private String claimantName;
    private String claimantEmail;

    @NotBlank(message = "Claim message is required")
    private String claimMessage;

    @NotBlank(message = "Proof details are required to substantiate ownership")
    private String proofDetails;

    public CreateClaimRequest() {
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
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
}
