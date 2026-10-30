package com.campus.lostfound.controller;

import com.campus.lostfound.dto.ApiResponse;
import com.campus.lostfound.dto.ClaimResponse;
import com.campus.lostfound.dto.CreateClaimRequest;
import com.campus.lostfound.exception.UnauthorizedException;
import com.campus.lostfound.model.User;
import com.campus.lostfound.service.AuthService;
import com.campus.lostfound.service.ClaimService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final AuthService authService;

    public ClaimController(ClaimService claimService, AuthService authService) {
        this.claimService = claimService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClaimResponse>> submitClaim(
            @Valid @RequestBody CreateClaimRequest req,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        if (user == null || !user.isActive()) {
            throw new UnauthorizedException("Please sign in with your campus email to submit an ownership claim.");
        }

        ClaimResponse created = claimService.createClaim(req, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Ownership claim submitted for review", created));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getMyClaims(HttpServletRequest request) throws SQLException {
        User user = authService.getAuthenticatedUser(request);
        if (user == null) {
            throw new UnauthorizedException("Please sign in to view your claims.");
        }

        List<ClaimResponse> claims = claimService.getMyClaims(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(claims));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaimById(
            @PathVariable("id") String id,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        ClaimResponse claim = claimService.getClaimById(id, user);
        return ResponseEntity.ok(ApiResponse.ok(claim));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getClaimsByItem(
            @PathVariable("itemId") String itemId,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        boolean isStaff = user != null && user.isActive() && (user.isAdmin() || user.isStaff());

        List<ClaimResponse> claims = claimService.getClaimsByItem(itemId, isStaff);
        return ResponseEntity.ok(ApiResponse.ok(claims));
    }
}
