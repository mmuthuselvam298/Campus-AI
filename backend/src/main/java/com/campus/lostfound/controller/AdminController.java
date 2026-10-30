package com.campus.lostfound.controller;

import com.campus.lostfound.dao.AuditLogDao;
import com.campus.lostfound.dao.UserDao;
import com.campus.lostfound.dto.*;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.exception.UnauthorizedException;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.model.AccountStatus;
import com.campus.lostfound.model.AuditLog;
import com.campus.lostfound.model.User;
import com.campus.lostfound.service.AuthService;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.service.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ItemService itemService;
    private final ClaimService claimService;
    private final AuditLogDao auditLogDao;
    private final AuthService authService;
    private final UserDao userDao;

    public AdminController(ItemService itemService,
                           ClaimService claimService,
                           AuditLogDao auditLogDao,
                           AuthService authService,
                           UserDao userDao) {
        this.itemService = itemService;
        this.claimService = claimService;
        this.auditLogDao = auditLogDao;
        this.authService = authService;
        this.userDao = userDao;
    }

    private User requireStaffOrAdmin(HttpServletRequest request) throws SQLException {
        User user = authService.getAuthenticatedUser(request);
        if (user == null) {
            throw new UnauthorizedException("Sign in required to access administration and staff portal.");
        }
        if (!user.isActive()) {
            throw new ForbiddenException("Your account is pending review or suspended.");
        }
        if (!user.isStaff() && !user.isAdmin()) {
            throw new ForbiddenException("Staff or Administrator privileges required.");
        }
        return user;
    }

    private User requireAdmin(HttpServletRequest request) throws SQLException {
        User user = requireStaffOrAdmin(request);
        if (!user.isAdmin()) {
            throw new ForbiddenException("Administrator privileges required for this action.");
        }
        return user;
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getAdminItems(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search,
            HttpServletRequest request) throws SQLException {

        requireStaffOrAdmin(request);
        List<ItemResponse> items = itemService.getAllItems(status, type, search, true);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getAdminClaims(
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest request) throws SQLException {

        requireStaffOrAdmin(request);
        List<ClaimResponse> claims = claimService.getAllClaims(status, true);
        return ResponseEntity.ok(ApiResponse.ok(claims));
    }

    @PatchMapping("/claims/{id}/resolve")
    public ResponseEntity<ApiResponse<ClaimResponse>> resolveClaim(
            @PathVariable("id") String id,
            @Valid @RequestBody ResolveClaimRequest req,
            HttpServletRequest request) throws SQLException {

        User adminUser = requireStaffOrAdmin(request);
        ClaimResponse resolved = claimService.resolveClaim(id, req, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Claim successfully resolved", resolved));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            HttpServletRequest request) throws SQLException {

        requireStaffOrAdmin(request);
        List<AuditLog> logs = auditLogDao.findAll(limit);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @GetMapping("/staff-requests")
    public ResponseEntity<ApiResponse<List<AuthUserDto>>> getStaffRequests(HttpServletRequest request) throws SQLException {
        requireAdmin(request);
        List<User> list = userDao.findStaffRequests();
        List<AuthUserDto> dtos = list.stream().map(AuthUserDto::fromUser).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PatchMapping("/staff-requests/{id}")
    public ResponseEntity<ApiResponse<AuthUserDto>> resolveStaffRequest(
            @PathVariable("id") String id,
            @Valid @RequestBody StaffApprovalRequest req,
            HttpServletRequest request) throws SQLException {

        User adminUser = requireAdmin(request);

        User targetUser = userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (req.getStatus() != AccountStatus.ACTIVE && req.getStatus() != AccountStatus.REJECTED) {
            throw new ValidationException("Staff approval status must be either ACTIVE or REJECTED.");
        }

        userDao.updateStaffStatus(id, req.getStatus(), adminUser.getCampusEmail());

        auditLogDao.log(
                adminUser.getCampusEmail(),
                "STAFF_APPROVAL",
                "USER",
                id,
                String.format("Staff application %s for %s (%s)", req.getStatus(), targetUser.getFullName(), targetUser.getCampusEmail())
        );

        User updated = userDao.findById(id).orElse(targetUser);
        return ResponseEntity.ok(ApiResponse.ok("Staff request updated to " + req.getStatus(), AuthUserDto.fromUser(updated)));
    }
}
