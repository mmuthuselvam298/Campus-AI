package com.campus.lostfound.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MatchRecord {
    private String id;
    private String lostItemId;
    private String foundItemId;
    private double matchScore;
    private List<String> matchReasons = new ArrayList<>();
    private String reviewStatus = "PENDING";
    private Instant createdAt;

    public MatchRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLostItemId() {
        return lostItemId;
    }

    public void setLostItemId(String lostItemId) {
        this.lostItemId = lostItemId;
    }

    public String getFoundItemId() {
        return foundItemId;
    }

    public void setFoundItemId(String foundItemId) {
        this.foundItemId = foundItemId;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons != null ? matchReasons : new ArrayList<>();
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
