package com.campus.lostfound.service;

import com.campus.lostfound.dao.AuditLogDao;
import com.campus.lostfound.dao.ClaimDao;
import com.campus.lostfound.dao.ItemDao;
import com.campus.lostfound.dto.ClaimResponse;
import com.campus.lostfound.dto.CreateClaimRequest;
import com.campus.lostfound.dto.ItemResponse;
import com.campus.lostfound.dto.ResolveClaimRequest;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.model.Claim;
import com.campus.lostfound.model.ClaimStatus;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimService {

    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimDao claimDao;
    private final ItemDao itemDao;
    private final AuditLogDao auditLogDao;

    public ClaimService(ClaimDao claimDao, ItemDao itemDao, AuditLogDao auditLogDao) {
        this.claimDao = claimDao;
        this.itemDao = itemDao;
        this.auditLogDao = auditLogDao;
    }

    public ClaimResponse createClaim(CreateClaimRequest req) throws SQLException {
        return createClaim(req, null);
    }

    public ClaimResponse createClaim(CreateClaimRequest req, User user) throws SQLException {
        Item item = itemDao.findById(req.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Target item not found with id: " + req.getItemId()));

        if (item.getStatus() == ItemStatus.RESOLVED || item.getStatus() == ItemStatus.CLOSED) {
            throw new ValidationException("Cannot submit a claim for an item that has already been resolved or closed.");
        }

        // Prevent reporting user from claiming their own item
        if (user != null && item.getReporterUserId() != null && item.getReporterUserId().equals(user.getId())) {
            throw new ValidationException("You cannot submit an ownership claim on an item that you reported yourself.");
        }

        Claim claim = new Claim();
        claim.setId(UUID.randomUUID().toString());
        claim.setItemId(req.getItemId());

        if (user != null) {
            claim.setClaimantUserId(user.getId());
            claim.setClaimantName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : req.getClaimantName());
            claim.setClaimantEmail(user.getCampusEmail());
        } else {
            claim.setClaimantName(req.getClaimantName());
            claim.setClaimantEmail(req.getClaimantEmail());
        }

        claim.setClaimMessage(req.getClaimMessage());
        claim.setProofDetails(req.getProofDetails());
        claim.setStatus(ClaimStatus.PENDING);
        Instant now = Instant.now();
        claim.setCreatedAt(now);
        claim.setUpdatedAt(now);

        claimDao.insert(claim);

        // Update item status to CLAIMED to indicate pending ownership review
        itemDao.updateStatus(item.getId(), ItemStatus.CLAIMED);

        auditLogDao.log(
            claim.getClaimantName() != null ? claim.getClaimantName() : (user != null ? user.getCampusEmail() : "STUDENT"),
            "SUBMIT_CLAIM",
            "CLAIM",
            claim.getId(),
            String.format("Claim submitted for item %s (%s)", item.getId(), item.getCategory())
        );

        logger.info("Created claim {} for item {} by user {}", claim.getId(), item.getId(), claim.getClaimantUserId());
        ClaimResponse resp = ClaimResponse.fromClaim(claim, true);
        resp.setItem(ItemResponse.fromItem(item, false));
        return resp;
    }

    public ClaimResponse getClaimById(String id, boolean isAdmin) throws SQLException {
        Claim claim = claimDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));

        ClaimResponse resp = ClaimResponse.fromClaim(claim, isAdmin);
        itemDao.findById(claim.getItemId()).ifPresent(item -> resp.setItem(ItemResponse.fromItem(item, isAdmin)));
        return resp;
    }

    public ClaimResponse getClaimById(String id, User user) throws SQLException {
        Claim claim = claimDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));

        if (user == null || !user.isActive()) {
            throw new com.campus.lostfound.exception.UnauthorizedException("Sign in required to view claim details.");
        }

        boolean isStaffOrAdmin = user.isStaff() || user.isAdmin();
        boolean isClaimant = claim.getClaimantUserId() != null && claim.getClaimantUserId().equals(user.getId());

        if (!isStaffOrAdmin && !isClaimant) {
            throw new com.campus.lostfound.exception.ForbiddenException("You do not have permission to view this claim.");
        }

        ClaimResponse resp = ClaimResponse.fromClaim(claim, true);
        itemDao.findById(claim.getItemId()).ifPresent(item -> resp.setItem(ItemResponse.fromItem(item, isStaffOrAdmin)));
        return resp;
    }

    public List<ClaimResponse> getClaimsByItem(String itemId, boolean isAdmin) throws SQLException {
        List<Claim> claims = claimDao.findByItemId(itemId);
        List<ClaimResponse> list = new ArrayList<>();
        for (Claim c : claims) {
            list.add(ClaimResponse.fromClaim(c, isAdmin));
        }
        return list;
    }

    public List<ClaimResponse> getMyClaims(String claimantUserId) throws SQLException {
        List<Claim> claims = claimDao.findByClaimantUserId(claimantUserId);
        List<ClaimResponse> list = new ArrayList<>();
        for (Claim c : claims) {
            ClaimResponse resp = ClaimResponse.fromClaim(c, true);
            itemDao.findById(c.getItemId()).ifPresent(item -> resp.setItem(ItemResponse.fromItem(item, false)));
            list.add(resp);
        }
        return list;
    }

    public List<ClaimResponse> getAllClaims(String status, boolean isAdmin) throws SQLException {
        List<Claim> claims = claimDao.findAll(status);
        List<ClaimResponse> list = new ArrayList<>();
        for (Claim c : claims) {
            ClaimResponse resp = ClaimResponse.fromClaim(c, isAdmin);
            itemDao.findById(c.getItemId()).ifPresent(item -> resp.setItem(ItemResponse.fromItem(item, isAdmin)));
            list.add(resp);
        }
        return list;
    }

    public ClaimResponse resolveClaim(String claimId, ResolveClaimRequest req, User adminUser) throws SQLException {
        if (adminUser == null || !adminUser.isActive() || (!adminUser.isStaff() && !adminUser.isAdmin())) {
            throw new com.campus.lostfound.exception.ForbiddenException("Only active staff or administrators can resolve claims.");
        }
        req.setAdminActor(adminUser.getCampusEmail());
        return resolveClaim(claimId, req);
    }

    public ClaimResponse resolveClaim(String claimId, ResolveClaimRequest req) throws SQLException {
        Claim claim = claimDao.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        claimDao.updateResolution(claimId, req.getStatus(), req.getResolutionNotes(), req.getAdminActor());

        // Update item status in sync
        if (req.getStatus() == ClaimStatus.APPROVED || req.getStatus() == ClaimStatus.RESOLVED) {
            itemDao.updateStatus(claim.getItemId(), ItemStatus.RESOLVED);
        } else if (req.getStatus() == ClaimStatus.REJECTED) {
            // Check if there are other pending claims
            List<Claim> remaining = claimDao.findByItemId(claim.getItemId());
            boolean hasOtherPending = remaining.stream().anyMatch(c -> !c.getId().equals(claimId) && c.getStatus() == ClaimStatus.PENDING);
            if (!hasOtherPending) {
                itemDao.updateStatus(claim.getItemId(), ItemStatus.OPEN);
            }
        }

        auditLogDao.log(
            req.getAdminActor() != null ? req.getAdminActor() : "STAFF",
            "RESOLVE_CLAIM",
            "CLAIM",
            claimId,
            String.format("Claim %s updated to %s. Notes: %s", claimId, req.getStatus(), req.getResolutionNotes())
        );

        return getClaimById(claimId, true);
    }
}
