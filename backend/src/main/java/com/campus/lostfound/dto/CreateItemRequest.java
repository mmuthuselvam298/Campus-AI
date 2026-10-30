package com.campus.lostfound.dto;

import com.campus.lostfound.model.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class CreateItemRequest {
    private ReportType reportType;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Description is required")
    private String description;

    private String color;
    private String brand;
    private List<String> distinctiveFeatures = new ArrayList<>();
    private List<String> searchKeywords = new ArrayList<>();
    private String imagePath;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Incident date is required")
    private String incidentDate;

    private String reporterName;
    private String reporterContact;

    private String privateDetails;

    public CreateItemRequest() {
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(String incidentDate) {
        this.incidentDate = incidentDate;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterContact() {
        return reporterContact;
    }

    public void setReporterContact(String reporterContact) {
        this.reporterContact = reporterContact;
    }

    public String getPrivateDetails() {
        return privateDetails;
    }

    public void setPrivateDetails(String privateDetails) {
        this.privateDetails = privateDetails;
    }
}
