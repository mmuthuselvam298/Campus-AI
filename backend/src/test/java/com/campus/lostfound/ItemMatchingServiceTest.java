package com.campus.lostfound;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.dao.DatabaseInitializer;
import com.campus.lostfound.dao.ItemDao;
import com.campus.lostfound.dao.MatchDao;
import com.campus.lostfound.dto.ItemMatchDto;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.ReportType;
import com.campus.lostfound.service.ItemMatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemMatchingServiceTest {

    private File tempDbFile;
    private DatabaseConfig databaseConfig;
    private ItemDao itemDao;
    private MatchDao matchDao;
    private ItemMatchingService matchingService;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("test_matching_", ".db");
        tempDbFile.deleteOnExit();

        databaseConfig = new DatabaseConfig();
        ReflectionTestUtils.setField(databaseConfig, "databasePath", tempDbFile.getAbsolutePath());

        DatabaseInitializer initializer = new DatabaseInitializer(databaseConfig);
        initializer.initialize();

        ObjectMapper mapper = new ObjectMapper();
        itemDao = new ItemDao(databaseConfig, mapper);
        matchDao = new MatchDao(databaseConfig, mapper);
        matchingService = new ItemMatchingService(itemDao, matchDao);
    }

    @AfterEach
    void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    void testEmptyDatabaseReturnsZeroMatches() throws SQLException {
        Item lostItem = createSampleItem("lost-1", ReportType.LOST, "water bottle", "blue", "Hydro Flask", "Library", "2026-10-20");

        List<ItemMatchDto> matches = matchingService.findMatchesForItem(lostItem);

        assertNotNull(matches);
        assertTrue(matches.isEmpty(), "Zero matches must be returned when SQLite database has no candidate records");
    }

    @Test
    void testMatchingRankAndReasons() throws SQLException {
        Item lostItem = createSampleItem("lost-1", ReportType.LOST, "water bottle", "blue", "Hydro Flask", "Library Level 2", "2026-10-20");
        lostItem.setDistinctiveFeatures(List.of("black cap", "dent on base"));

        // Candidate 1: High match (same category, brand, color, location)
        Item found1 = createSampleItem("found-1", ReportType.FOUND, "water bottle", "blue", "Hydro Flask", "Library", "2026-10-21");
        found1.setDistinctiveFeatures(List.of("black cap"));
        itemDao.insert(found1);

        // Candidate 2: Low match (different category)
        Item found2 = createSampleItem("found-2", ReportType.FOUND, "umbrella", "black", null, "Campus Center", "2026-10-15");
        itemDao.insert(found2);

        List<ItemMatchDto> matches = matchingService.findMatchesForItem(lostItem);

        assertEquals(1, matches.size(), "Only matching candidates exceeding the threshold should be returned");
        ItemMatchDto topMatch = matches.get(0);
        assertEquals("found-1", topMatch.getCandidateItemId());
        assertTrue(topMatch.getMatchScore() > 0.6, "Expected strong match score > 0.6");
        assertFalse(topMatch.getMatchReasons().isEmpty(), "Human-readable reasons must be generated");
        assertTrue(topMatch.getMatchReasons().stream().anyMatch(r -> r.contains("water bottle")), "Should mention matching category");
    }

    @Test
    void testExcludesSelfAndClosedItems() throws SQLException {
        Item item = createSampleItem("item-1", ReportType.LOST, "laptop", "gray", "Apple", "Science Hall", "2026-10-22");

        Item sameId = createSampleItem("item-1", ReportType.FOUND, "laptop", "gray", "Apple", "Science Hall", "2026-10-22");
        itemDao.insert(sameId);

        Item closedItem = createSampleItem("found-closed", ReportType.FOUND, "laptop", "gray", "Apple", "Science Hall", "2026-10-22");
        closedItem.setStatus(ItemStatus.CLOSED);
        itemDao.insert(closedItem);

        List<ItemMatchDto> matches = matchingService.findMatchesForItem(item);
        assertTrue(matches.isEmpty(), "Self and closed items must be excluded from candidate matches");
    }

    private Item createSampleItem(String id, ReportType type, String cat, String color, String brand, String loc, String date) {
        Item item = new Item();
        item.setId(id);
        item.setReportType(type);
        item.setCategory(cat);
        item.setDescription("Test item " + cat);
        item.setColor(color);
        item.setBrand(brand);
        item.setLocation(loc);
        item.setIncidentDate(date);
        item.setStatus(ItemStatus.OPEN);
        item.setReporterName("Student User");
        item.setReporterContact("student@campus.edu");
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        return item;
    }
}
