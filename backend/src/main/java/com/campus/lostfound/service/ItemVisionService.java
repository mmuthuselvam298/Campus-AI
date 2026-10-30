package com.campus.lostfound.service;

import com.campus.lostfound.dto.ItemAnalysisResponse;
import com.campus.lostfound.exception.AiServiceException;

public interface ItemVisionService {
    /**
     * Analyzes an uploaded item photograph using vision AI.
     * Returns structured metadata (category, colors, brand if visible,
     * description, distinctive features, keywords, confidence).
     */
    ItemAnalysisResponse analyzeItemImage(byte[] imageBytes, String mimeType) throws AiServiceException;
}
