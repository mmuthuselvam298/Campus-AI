package com.campus.lostfound.controller;

import com.campus.lostfound.dto.ApiResponse;
import com.campus.lostfound.dto.ItemAnalysisResponse;
import com.campus.lostfound.service.FileStorageService;
import com.campus.lostfound.service.ItemVisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    private final FileStorageService fileStorageService;
    private final ItemVisionService itemVisionService;

    public AiController(FileStorageService fileStorageService, ItemVisionService itemVisionService) {
        this.fileStorageService = fileStorageService;
        this.itemVisionService = itemVisionService;
    }

    @PostMapping(value = "/analyse-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeImage(@RequestParam("file") MultipartFile file) throws IOException {
        logger.info("Received image for AI analysis: name={}, size={} bytes, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 1. Store the uploaded file safely
        String storedFilename = fileStorageService.storeFile(file);
        String imagePath = "/uploads/" + storedFilename;

        // 2. Perform Vision AI Analysis with graceful fallback
        byte[] fileBytes = file.getBytes();
        ItemAnalysisResponse analysis;
        String message;

        try {
            analysis = itemVisionService.analyzeItemImage(fileBytes, file.getContentType());
            message = analysis.isFallback()
                    ? "Image uploaded. Manual description enabled."
                    : "Image successfully analyzed by AI Vision service.";
        } catch (Exception e) {
            logger.warn("Vision AI encountered an issue: {}. Enabling manual fallback mode.", e.getMessage());
            analysis = new ItemAnalysisResponse();
            analysis.setCategory("");
            analysis.setDescription("");
            analysis.setConfidence(0.0);
            analysis.setFallback(true);
            analysis.setNote("Vision AI was momentarily unavailable (" + e.getMessage() + "). Photo preserved; please confirm details manually.");
            message = "Image saved. Please enter description manually.";
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("imagePath", imagePath);
        responseData.put("analysis", analysis);

        return ResponseEntity.ok(ApiResponse.ok(message, responseData));
    }
}
