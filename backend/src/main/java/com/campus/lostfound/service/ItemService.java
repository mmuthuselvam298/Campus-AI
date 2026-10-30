package com.campus.lostfound.service;

import com.campus.lostfound.dao.AuditLogDao;
import com.campus.lostfound.dao.ItemDao;
import com.campus.lostfound.dto.CreateItemRequest;
import com.campus.lostfound.dto.ItemMatchDto;
import com.campus.lostfound.dto.ItemResponse;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    private final ItemDao itemDao;
    private final ItemMatchingService matchingService;
    private final AuditLogDao auditLogDao;

    public ItemService(ItemDao itemDao, ItemMatchingService matchingService, AuditLogDao auditLogDao) {
        this.itemDao = itemDao;
        this.matchingService = matchingService;
        this.auditLogDao = auditLogDao;
    }

    public ItemResponse createItem(CreateItemRequest req) throws SQLException {
        return createItem(req, null);
    }

    public ItemResponse createItem(CreateItemRequest req, User user) throws SQLException {
        Item item = new Item();
        item.setId(UUID.randomUUID().toString());
        item.setReportType(req.getReportType());
        item.setCategory(req.getCategory());
        item.setDescription(req.getDescription());
        item.setColor(req.getColor());
        item.setBrand(req.getBrand());
        item.setDistinctiveFeatures(req.getDistinctiveFeatures());
        item.setSearchKeywords(req.getSearchKeywords());
        item.setImagePath(req.getImagePath());
        item.setLocation(req.getLocation());
        item.setIncidentDate(req.getIncidentDate());
        item.setStatus(ItemStatus.OPEN);

        if (user != null) {
            item.setReporterUserId(user.getId());
            item.setReporterName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : req.getReporterName());
            item.setReporterContact(user.getCampusEmail());
        } else {
            item.setReporterName(req.getReporterName());
            item.setReporterContact(req.getReporterContact());
        }

        item.setPrivateDetails(req.getPrivateDetails());
        Instant now = Instant.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        itemDao.insert(item);

        auditLogDao.log(
            item.getReporterName() != null ? item.getReporterName() : (user != null ? user.getCampusEmail() : "CAMPUS_USER"),
            "CREATE_ITEM",
            "ITEM",
            item.getId(),
            String.format("Reported %s item [%s] at %s", item.getReportType(), item.getCategory(), item.getLocation())
        );

        logger.info("Created item record {} ({}) by user {}", item.getId(), item.getCategory(), item.getReporterUserId());
        return ItemResponse.fromItem(item, true);
    }

    public ItemResponse getItemById(String id, boolean isAdmin) throws SQLException {
        Item item = itemDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return ItemResponse.fromItem(item, isAdmin);
    }

    public ItemResponse getItemById(String id, User user) throws SQLException {
        Item item = itemDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        boolean isPrivileged = user != null && user.isActive() && (
                user.isAdmin() || user.isStaff() || (item.getReporterUserId() != null && item.getReporterUserId().equals(user.getId()))
        );
        return ItemResponse.fromItem(item, isPrivileged);
    }

    public List<ItemResponse> getAllItems(String status, String type, String search, boolean isAdmin) throws SQLException {
        List<Item> items = itemDao.findAll(status, type, search);
        return items.stream()
                .map(item -> ItemResponse.fromItem(item, isAdmin))
                .collect(Collectors.toList());
    }

    public List<ItemResponse> getItemsByReporter(String reporterUserId) throws SQLException {
        List<Item> items = itemDao.findByReporterUserId(reporterUserId);
        return items.stream()
                .map(item -> ItemResponse.fromItem(item, true))
                .collect(Collectors.toList());
    }

    public List<ItemMatchDto> getMatchesForItem(String id) throws SQLException {
        Item item = itemDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return matchingService.findMatchesForItem(item);
    }

    public boolean updateStatus(String id, ItemStatus status, String actor) throws SQLException {
        Item item = itemDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        boolean updated = itemDao.updateStatus(id, status);
        if (updated) {
            auditLogDao.log(
                actor != null ? actor : "STAFF",
                "UPDATE_STATUS",
                "ITEM",
                id,
                String.format("Status changed from %s to %s", item.getStatus(), status)
            );
        }
        return updated;
    }

    public boolean updateStatus(String id, ItemStatus status, User user) throws SQLException {
        Item item = itemDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (user == null || !user.isActive()) {
            throw new com.campus.lostfound.exception.ForbiddenException("Active account required to update item status.");
        }

        boolean isStaffOrAdmin = user.isStaff() || user.isAdmin();
        boolean isOwner = item.getReporterUserId() != null && item.getReporterUserId().equals(user.getId());

        if (!isStaffOrAdmin && !isOwner) {
            throw new com.campus.lostfound.exception.ForbiddenException("You do not have permission to modify this item.");
        }

        boolean updated = itemDao.updateStatus(id, status);
        if (updated) {
            auditLogDao.log(
                user.getCampusEmail(),
                "UPDATE_STATUS",
                "ITEM",
                id,
                String.format("Status changed from %s to %s by %s (%s)", item.getStatus(), status, user.getFullName(), user.getRole())
            );
        }
        return updated;
    }

    public boolean deleteItem(String id, String actor) throws SQLException {
        boolean deleted = itemDao.deleteById(id);
        if (deleted) {
            auditLogDao.log(
                actor != null ? actor : "STAFF",
                "DELETE_ITEM",
                "ITEM",
                id,
                "Item deleted from registry"
            );
        }
        return deleted;
    }

    public boolean deleteItem(String id, User user) throws SQLException {
        Item item = itemDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (user == null || !user.isActive()) {
            throw new com.campus.lostfound.exception.ForbiddenException("Active account required to delete items.");
        }

        boolean isStaffOrAdmin = user.isStaff() || user.isAdmin();
        boolean isOwner = item.getReporterUserId() != null && item.getReporterUserId().equals(user.getId());

        if (!isStaffOrAdmin && !isOwner) {
            throw new com.campus.lostfound.exception.ForbiddenException("You do not have permission to delete this item.");
        }

        boolean deleted = itemDao.deleteById(id);
        if (deleted) {
            auditLogDao.log(
                user.getCampusEmail(),
                "DELETE_ITEM",
                "ITEM",
                id,
                String.format("Item deleted by %s (%s)", user.getFullName(), user.getRole())
            );
        }
        return deleted;
    }
}
