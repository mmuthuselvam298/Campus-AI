package com.campus.lostfound.dto;

import java.util.ArrayList;
import java.util.List;

public class ItemMatchDto {
    private String candidateItemId;
    private ItemResponse item;
    private double matchScore; // 0.0 - 1.0 (or percentage 0 - 100)
    private int matchPercentage;
    private List<String> matchReasons = new ArrayList<>();
    private String confidenceLabel; // e.g. "High Match", "Moderate Match", "Possible Match"

    public ItemMatchDto() {
    }

    public String getCandidateItemId() {
        return candidateItemId;
    }

    public void setCandidateItemId(String candidateItemId) {
        this.candidateItemId = candidateItemId;
    }

    public ItemResponse getItem() {
        return item;
    }

    public void setItem(ItemResponse item) {
        this.item = item;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
        this.matchPercentage = (int) Math.round(matchScore * 100.0);
        if (matchScore >= 0.75) {
            this.confidenceLabel = "Strong Match";
        } else if (matchScore >= 0.50) {
            this.confidenceLabel = "Likely Match";
        } else {
            this.confidenceLabel = "Possible Match";
        }
    }

    public int getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(int matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons != null ? matchReasons : new ArrayList<>();
    }

    public String getConfidenceLabel() {
        return confidenceLabel;
    }

    public void setConfidenceLabel(String confidenceLabel) {
        this.confidenceLabel = confidenceLabel;
    }
}
