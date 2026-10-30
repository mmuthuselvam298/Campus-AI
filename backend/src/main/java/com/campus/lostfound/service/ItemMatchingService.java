package com.campus.lostfound.service;

import com.campus.lostfound.dao.ItemDao;
import com.campus.lostfound.dao.MatchDao;
import com.campus.lostfound.dto.ItemMatchDto;
import com.campus.lostfound.dto.ItemResponse;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.MatchRecord;
import com.campus.lostfound.model.ReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ItemMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(ItemMatchingService.class);

    // Centralized Scoring Weights (Total = 100.0)
    public static class ScoringWeights {
        public static final double CATEGORY_WEIGHT = 25.0;
        public static final double DISTINCTIVE_FEATURES_WEIGHT = 20.0;
        public static final double BRAND_WEIGHT = 15.0;
        public static final double COLOR_WEIGHT = 15.0;
        public static final double KEYWORDS_DESC_WEIGHT = 10.0;
        public static final double LOCATION_WEIGHT = 10.0;
        public static final double DATE_WEIGHT = 5.0;
        public static final double MIN_SCORE_THRESHOLD = 0.20; // 20%
    }

    private final ItemDao itemDao;
    private final MatchDao matchDao;

    public ItemMatchingService(ItemDao itemDao, MatchDao matchDao) {
        this.itemDao = itemDao;
        this.matchDao = matchDao;
    }

    /**
     * Finds active matching reports in the SQLite database for a given item.
     * If the item is LOST, it searches active FOUND items.
     * If the item is FOUND, it searches active LOST items.
     * Results are deduplicated, scored, explained, and sorted in descending order.
     */
    public List<ItemMatchDto> findMatchesForItem(Item sourceItem) throws SQLException {
        if (sourceItem == null) {
            return Collections.emptyList();
        }

        // Determine counterpart report type
        ReportType targetType = (sourceItem.getReportType() == ReportType.LOST)
                ? ReportType.FOUND
                : ReportType.LOST;

        // Fetch active candidate items from the SQLite database
        List<Item> candidates = itemDao.findActiveByReportType(targetType);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Deduplication and candidate ranking using Map and Set
        Map<String, ItemMatchDto> scoredMatches = new HashMap<>();

        for (Item candidate : candidates) {
            // Exclude self or inactive reports
            if (candidate.getId().equals(sourceItem.getId())) {
                continue;
            }
            if (candidate.getStatus() == ItemStatus.CLOSED || candidate.getStatus() == ItemStatus.RESOLVED) {
                continue;
            }

            // Calculate match score and generate reasons
            List<String> reasons = new ArrayList<>();
            double score = computeMatchScore(sourceItem, candidate, reasons);

            // Filter out weak candidates below threshold
            if (score >= ScoringWeights.MIN_SCORE_THRESHOLD) {
                ItemMatchDto matchDto = new ItemMatchDto();
                matchDto.setCandidateItemId(candidate.getId());
                matchDto.setItem(ItemResponse.fromItem(candidate, false));
                matchDto.setMatchScore(score);
                matchDto.setMatchReasons(reasons);

                scoredMatches.put(candidate.getId(), matchDto);

                // Persist match record into SQLite database
                persistMatchRecord(sourceItem, candidate, score, reasons);
            }
        }

        // Sort candidates in descending order of match score
        List<ItemMatchDto> results = new ArrayList<>(scoredMatches.values());
        results.sort((m1, m2) -> Double.compare(m2.getMatchScore(), m1.getMatchScore()));

        logger.info("Found {} matches for item {} ({})", results.size(), sourceItem.getId(), sourceItem.getCategory());
        return results;
    }

    public double computeMatchScore(Item a, Item b, List<String> reasons) {
        double score = 0.0;

        // 1. Category Similarity (25 pts)
        double catScore = evaluateCategory(a.getCategory(), b.getCategory());
        if (catScore > 0) {
            score += catScore * ScoringWeights.CATEGORY_WEIGHT;
            reasons.add(String.format("Matching or closely related category: %s", a.getCategory()));
        }

        // 2. Distinctive Features Overlap (20 pts)
        double featureScore = evaluateSetOverlap(a.getDistinctiveFeatures(), b.getDistinctiveFeatures());
        if (featureScore > 0) {
            score += featureScore * ScoringWeights.DISTINCTIVE_FEATURES_WEIGHT;
            reasons.add("Shared distinctive physical characteristics identified");
        }

        // 3. Brand Compatibility (15 pts)
        double brandScore = evaluateBrand(a.getBrand(), b.getBrand());
        if (brandScore > 0) {
            score += brandScore * ScoringWeights.BRAND_WEIGHT;
            if (a.getBrand() != null && b.getBrand() != null && a.getBrand().equalsIgnoreCase(b.getBrand())) {
                reasons.add(String.format("Identical brand/make: %s", a.getBrand()));
            }
        }

        // 4. Color Overlap (15 pts)
        double colorScore = evaluateColor(a.getColor(), b.getColor());
        if (colorScore > 0) {
            score += colorScore * ScoringWeights.COLOR_WEIGHT;
            reasons.add("Compatible color palette match");
        }

        // 5. Keywords and Description Similarity (10 pts)
        double keywordScore = evaluateKeywordsAndDescription(a, b);
        if (keywordScore > 0) {
            score += keywordScore * ScoringWeights.KEYWORDS_DESC_WEIGHT;
            reasons.add("Consistent search keywords and visual description");
        }

        // 6. Location Compatibility (10 pts)
        double locationScore = evaluateLocation(a.getLocation(), b.getLocation());
        if (locationScore > 0) {
            score += locationScore * ScoringWeights.LOCATION_WEIGHT;
            reasons.add(String.format("Compatible campus area: near %s", a.getLocation()));
        }

        // 7. Date Compatibility (5 pts)
        double dateScore = evaluateDates(a, b);
        if (dateScore > 0) {
            score += dateScore * ScoringWeights.DATE_WEIGHT;
            reasons.add("Compatible incident date and timeline");
        }

        // Normalize to 0.0 - 1.0 range
        return Math.min(1.0, Math.max(0.0, score / 100.0));
    }

    private double evaluateCategory(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return 0.0;
        String c1 = cat1.trim().toLowerCase();
        String c2 = cat2.trim().toLowerCase();
        if (c1.equals(c2)) return 1.0;
        if (c1.contains(c2) || c2.contains(c1)) return 0.8;
        return 0.0;
    }

    private double evaluateBrand(String brand1, String brand2) {
        if (brand1 == null || brand2 == null || brand1.isBlank() || brand2.isBlank()) {
            return 0.4; // Neutral partial score when brand is not visible
        }
        String b1 = brand1.trim().toLowerCase();
        String b2 = brand2.trim().toLowerCase();
        if (b1.equals(b2)) return 1.0;
        if (b1.contains(b2) || b2.contains(b1)) return 0.7;
        return 0.0; // Conflicting visible brands
    }

    private double evaluateColor(String col1, String col2) {
        if (col1 == null || col2 == null) return 0.2;
        Set<String> s1 = extractTokens(col1);
        Set<String> s2 = extractTokens(col2);
        if (s1.isEmpty() || s2.isEmpty()) return 0.2;

        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);
        if (!intersection.isEmpty()) {
            return (double) intersection.size() / Math.max(s1.size(), s2.size());
        }
        return 0.0;
    }

    private double evaluateSetOverlap(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null || list1.isEmpty() || list2.isEmpty()) {
            return 0.0;
        }
        Set<String> tokens1 = new HashSet<>();
        for (String item : list1) {
            tokens1.addAll(extractTokens(item));
        }

        Set<String> tokens2 = new HashSet<>();
        for (String item : list2) {
            tokens2.addAll(extractTokens(item));
        }

        Set<String> common = new HashSet<>(tokens1);
        common.retainAll(tokens2);

        if (!common.isEmpty()) {
            return Math.min(1.0, (double) common.size() / Math.min(tokens1.size(), tokens2.size()));
        }
        return 0.0;
    }

    private double evaluateKeywordsAndDescription(Item a, Item b) {
        Set<String> setA = new HashSet<>();
        setA.addAll(extractTokens(a.getDescription()));
        if (a.getSearchKeywords() != null) {
            for (String kw : a.getSearchKeywords()) setA.addAll(extractTokens(kw));
        }

        Set<String> setB = new HashSet<>();
        setB.addAll(extractTokens(b.getDescription()));
        if (b.getSearchKeywords() != null) {
            for (String kw : b.getSearchKeywords()) setB.addAll(extractTokens(kw));
        }

        if (setA.isEmpty() || setB.isEmpty()) return 0.0;

        Set<String> overlap = new HashSet<>(setA);
        overlap.retainAll(setB);

        return Math.min(1.0, (double) overlap.size() / Math.min(setA.size(), setB.size()));
    }

    private double evaluateLocation(String loc1, String loc2) {
        if (loc1 == null || loc2 == null) return 0.3;
        String l1 = loc1.trim().toLowerCase();
        String l2 = loc2.trim().toLowerCase();
        if (l1.equals(l2)) return 1.0;
        Set<String> t1 = extractTokens(l1);
        Set<String> t2 = extractTokens(l2);
        Set<String> common = new HashSet<>(t1);
        common.retainAll(t2);
        if (!common.isEmpty()) return 0.8;
        return 0.2; // Different campus areas still possible
    }

    private double evaluateDates(Item a, Item b) {
        try {
            LocalDate d1 = LocalDate.parse(a.getIncidentDate().substring(0, 10));
            LocalDate d2 = LocalDate.parse(b.getIncidentDate().substring(0, 10));
            long days = Math.abs(ChronoUnit.DAYS.between(d1, d2));
            if (days <= 1) return 1.0;
            if (days <= 7) return 0.8;
            if (days <= 30) return 0.5;
            return 0.2;
        } catch (Exception e) {
            return 0.5; // Neutral when date parsing fails
        }
    }

    private Set<String> extractTokens(String text) {
        if (text == null) return Collections.emptySet();
        Set<String> tokens = new HashSet<>();
        String[] words = text.toLowerCase().split("[^a-zA-Z0-9]+");
        for (String w : words) {
            if (w.length() > 2) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    private void persistMatchRecord(Item source, Item candidate, double score, List<String> reasons) {
        try {
            String lostId = (source.getReportType() == ReportType.LOST) ? source.getId() : candidate.getId();
            String foundId = (source.getReportType() == ReportType.FOUND) ? source.getId() : candidate.getId();

            MatchRecord rec = new MatchRecord();
            rec.setId(UUID.randomUUID().toString());
            rec.setLostItemId(lostId);
            rec.setFoundItemId(foundId);
            rec.setMatchScore(score);
            rec.setMatchReasons(reasons);
            rec.setReviewStatus("PENDING");
            rec.setCreatedAt(Instant.now());

            matchDao.upsertMatch(rec);
        } catch (Exception e) {
            logger.warn("Failed to persist match record to SQLite", e);
        }
    }
}
