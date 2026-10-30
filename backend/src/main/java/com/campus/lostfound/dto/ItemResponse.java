package com.campus.lostfound.dto;

import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.ReportType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ItemResponse {
    private String id;
    private ReportType reportType;
    private String category;
    private String description;
    private String color;
    private String brand;
    private List<String> distinctiveFeatures = new ArrayList<>();
    private List<String> searchKeywords = new ArrayList<>();
    private String imagePath;
    private String location;
    private String incidentDate;
    private ItemStatus status;
    private String reporterName;
    private String reporterContact; // Masked for public
    private String reporterUserId;
    private String privateDetails;  // Only populated if authorized/admin
    private Instant createdAt;
    private Instant updatedAt;

    public ItemResponse() {
    }

    public static ItemResponse fromItem(Item item, boolean includePrivateDetails) {
        if (item == null) return null;
        ItemResponse resp = new ItemResponse();
        resp.setId(item.getId());
        resp.setReportType(item.getReportType());
        resp.setCategory(item.getCategory());
        resp.setDescription(item.getDescription());
        resp.setColor(item.getColor());
        resp.setBrand(item.getBrand());
        resp.setDistinctiveFeatures(item.getDistinctiveFeatures());
        resp.setSearchKeywords(item.getSearchKeywords());
        resp.setImagePath(item.getImagePath());
        resp.setLocation(item.getLocation());
        resp.setIncidentDate(item.getIncidentDate());
        resp.setStatus(item.getStatus());
        resp.setReporterUserId(item.getReporterUserId());
        resp.setCreatedAt(item.getCreatedAt());
        resp.setUpdatedAt(item.getUpdatedAt());

        if (includePrivateDetails) {
            resp.setReporterName(item.getReporterName());
            resp.setReporterContact(item.getReporterContact());
            resp.setPrivateDetails(item.getPrivateDetails());
        } else {
            resp.setReporterName(maskName(item.getReporterName()));
            resp.setReporterContact(maskContact(item.getReporterContact()));
            resp.setPrivateDetails(null);
        }
        return resp;
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Anonymous";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + "***";
        }
        return parts[0].charAt(0) + "*** " + parts[parts.length - 1].charAt(0) + ".";
    }

    private static String maskContact(String contact) {
        if (contact == null || contact.isBlank()) return "Hidden";
        if (contact.contains("@")) {
            int atIdx = contact.indexOf('@');
            if (atIdx <= 1) return "***" + contact.substring(atIdx);
            return contact.substring(0, 1) + "***" + contact.substring(atIdx);
        }
        if (contact.length() > 4) {
            return "***-***-" + contact.substring(contact.length() - 4);
        }
        return "***";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        this.distinctiveFeatures = distinctiveFeatures;
    }

    public List<String> getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(List<String> searchKeywords) {
        this.searchKeywords = searchKeywords;
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

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
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

    public String getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(String reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public String getPrivateDetails() {
        return privateDetails;
    }

    public void setPrivateDetails(String privateDetails) {
        this.privateDetails = privateDetails;
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
