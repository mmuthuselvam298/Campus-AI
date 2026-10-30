package com.campus.lostfound.controller;

import com.campus.lostfound.dto.ApiResponse;
import com.campus.lostfound.dto.CreateItemRequest;
import com.campus.lostfound.dto.ItemMatchDto;
import com.campus.lostfound.dto.ItemResponse;
import com.campus.lostfound.exception.UnauthorizedException;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.ReportType;
import com.campus.lostfound.model.User;
import com.campus.lostfound.service.AuthService;
import com.campus.lostfound.service.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final AuthService authService;

    public ItemController(ItemService itemService, AuthService authService) {
        this.itemService = itemService;
        this.authService = authService;
    }

    @PostMapping("/lost")
    public ResponseEntity<ApiResponse<ItemResponse>> reportLostItem(
            @Valid @RequestBody CreateItemRequest req,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        if (user == null || !user.isActive()) {
            throw new UnauthorizedException("Please sign in with your campus email to report an item.");
        }

        req.setReportType(ReportType.LOST);
        ItemResponse created = itemService.createItem(req, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lost item report registered successfully", created));
    }

    @PostMapping("/found")
    public ResponseEntity<ApiResponse<ItemResponse>> reportFoundItem(
            @Valid @RequestBody CreateItemRequest req,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        if (user == null || !user.isActive()) {
            throw new UnauthorizedException("Please sign in with your campus email to report an item.");
        }

        req.setReportType(ReportType.FOUND);
        ItemResponse created = itemService.createItem(req, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Found item report registered successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getAllItems(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        boolean isAdminOrStaff = user != null && user.isActive() && (user.isAdmin() || user.isStaff());

        List<ItemResponse> items = itemService.getAllItems(status, type, search, isAdminOrStaff);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getMyItems(HttpServletRequest request) throws SQLException {
        User user = authService.getAuthenticatedUser(request);
        if (user == null) {
            throw new UnauthorizedException("Please sign in to view your items.");
        }
        List<ItemResponse> items = itemService.getItemsByReporter(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> getItemById(
            @PathVariable("id") String id,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        ItemResponse item = itemService.getItemById(id, user);
        return ResponseEntity.ok(ApiResponse.ok(item));
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<ApiResponse<List<ItemMatchDto>>> getItemMatches(@PathVariable("id") String id) throws SQLException {
        List<ItemMatchDto> matches = itemService.getMatchesForItem(id);
        String msg = matches.isEmpty()
                ? "No matching items found in the current database."
                : String.format("Found %d potential match(es).", matches.size());
        return ResponseEntity.ok(ApiResponse.ok(msg, matches));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> updateItemStatus(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        if (user == null) {
            throw new UnauthorizedException("Authentication required to update item.");
        }

        String statusStr = payload.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing 'status' in payload"));
        }
        ItemStatus status = ItemStatus.valueOf(statusStr.toUpperCase());

        boolean updated = itemService.updateStatus(id, status, user);
        return ResponseEntity.ok(ApiResponse.ok("Item status updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> deleteItem(
            @PathVariable("id") String id,
            HttpServletRequest request) throws SQLException {

        User user = authService.getAuthenticatedUser(request);
        if (user == null) {
            throw new UnauthorizedException("Authentication required to delete item.");
        }

        boolean deleted = itemService.deleteItem(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Item deleted", deleted));
    }
}
