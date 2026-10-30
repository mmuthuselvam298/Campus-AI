package com.campus.lostfound.dto;

import java.util.ArrayList;
import java.util.List;

public class ItemAnalysisResponse {
    private String category;
    private List<String> colour = new ArrayList<>();
    private String brand;
    private String description;
    private List<String> distinctiveFeatures = new ArrayList<>();
    private List<String> searchKeywords = new ArrayList<>();
    private double confidence = 0.85;
    private boolean fallback = false;
    private String note;

    public ItemAnalysisResponse() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getColour() {
        return colour;
    }

    public void setColour(List<String> colour) {
        this.colour = colour != null ? colour : new ArrayList<>();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDistinctiveFeatures() {
        return distinctiveFeatures;
    }

    public void setDistinctiveFeatures(List<String> distinctiveFeatures) {
        this.distinctiveFeatures = distinctiveFeatures != null ? distinctiveFeatures : new ArrayList<>();
    }

    public List<String> getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(List<String> searchKeywords) {
        this.searchKeywords = searchKeywords != null ? searchKeywords : new ArrayList<>();
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
