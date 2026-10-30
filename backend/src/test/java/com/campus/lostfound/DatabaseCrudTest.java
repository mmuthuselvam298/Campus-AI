package com.campus.lostfound;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.dao.AuditLogDao;
import com.campus.lostfound.dao.ClaimDao;
import com.campus.lostfound.dao.DatabaseInitializer;
import com.campus.lostfound.dao.ItemDao;
import com.campus.lostfound.dto.CreateClaimRequest;
import com.campus.lostfound.dto.CreateItemRequest;
import com.campus.lostfound.dto.ItemResponse;
import com.campus.lostfound.dto.ResolveClaimRequest;
import com.campus.lostfound.model.ClaimStatus;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.ReportType;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.service.ItemMatchingService;
import com.campus.lostfound.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseCrudTest {

    private File tempDbFile;
    private DatabaseConfig databaseConfig;
    private ItemDao itemDao;
    private ClaimDao claimDao;
    private AuditLogDao auditLogDao;
    private ItemService itemService;
    private ClaimService claimService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("test_campus_lost_found_", ".db");
        tempDbFile.deleteOnExit();

        databaseConfig = new DatabaseConfig();
        ReflectionTestUtils.setField(databaseConfig, "databasePath", tempDbFile.getAbsolutePath());

        DatabaseInitializer initializer = new DatabaseInitializer(databaseConfig);
        initializer.initialize();

        objectMapper = new ObjectMapper();
        itemDao = new ItemDao(databaseConfig, objectMapper);
        claimDao = new ClaimDao(databaseConfig);
        auditLogDao = new AuditLogDao(databaseConfig);

        ItemMatchingService matchingService = new ItemMatchingService(itemDao, null);
        itemService = new ItemService(itemDao, matchingService, auditLogDao);
        claimService = new ClaimService(claimDao, itemDao, auditLogDao);
    }

    @AfterEach
    void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    void testDatabaseInitializesEmptyWithZeroSeededRecords() throws Exception {
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM items")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Database items table must start 100% empty with zero preloaded records");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM claims")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Database claims table must start empty");
            }
        }
    }

    @Test
    void testItemLifecycleAndClaimResolution() throws Exception {
        // 1. Report found item
        CreateItemRequest foundReq = new CreateItemRequest();
        foundReq.setReportType(ReportType.FOUND);
        foundReq.setCategory("backpack");
        foundReq.setDescription("Black North Face backpack with laptop sleeve");
        foundReq.setColor("black");
        foundReq.setBrand("The North Face");
        foundReq.setLocation("Campus Dining Hall");
        foundReq.setIncidentDate("2026-10-23");
        foundReq.setReporterName("Alex Finder");
        foundReq.setReporterContact("alex@campus.edu");
        foundReq.setPrivateDetails("Contains student ID 10923 inside front pocket");

        ItemResponse created = itemService.createItem(foundReq);
        assertNotNull(created.getId());
        assertEquals("backpack", created.getCategory());
        assertEquals(ItemStatus.OPEN, created.getStatus());

        // Verify public masking
        ItemResponse publicView = itemService.getItemById(created.getId(), false);
        assertNull(publicView.getPrivateDetails(), "Private details must not be exposed to public");
        assertTrue(publicView.getReporterContact().contains("***"), "Reporter contact must be masked");

        // 2. Submit Claim
        CreateClaimRequest claimReq = new CreateClaimRequest();
        claimReq.setItemId(created.getId());
        claimReq.setClaimantName("Sarah Owner");
        claimReq.setClaimantEmail("sarah@campus.edu");
        claimReq.setClaimMessage("I left my backpack on the chair near the entrance");
        claimReq.setProofDetails("Student ID number 10923 is in the front zip compartment");

        var claimResp = claimService.createClaim(claimReq);
        assertNotNull(claimResp.getId());
        assertEquals(ClaimStatus.PENDING, claimResp.getStatus());

        // Verify item status updated to CLAIMED
        Item updatedItem = itemDao.findById(created.getId()).orElseThrow();
        assertEquals(ItemStatus.CLAIMED, updatedItem.getStatus());

        // 3. Admin Resolves Claim
        ResolveClaimRequest resolveReq = new ResolveClaimRequest();
        resolveReq.setStatus(ClaimStatus.APPROVED);
        resolveReq.setResolutionNotes("Student ID matches and verified in person by Campus Security");
        resolveReq.setAdminActor("Security Officer Davis");

        var resolvedClaim = claimService.resolveClaim(claimResp.getId(), resolveReq);
        assertEquals(ClaimStatus.APPROVED, resolvedClaim.getStatus());

        // Verify item is now RESOLVED
        Item resolvedItem = itemDao.findById(created.getId()).orElseThrow();
        assertEquals(ItemStatus.RESOLVED, resolvedItem.getStatus());
    }
}
