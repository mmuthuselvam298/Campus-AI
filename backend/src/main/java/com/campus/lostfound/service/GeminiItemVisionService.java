package com.campus.lostfound.service;

import com.campus.lostfound.dto.ItemAnalysisResponse;
import com.campus.lostfound.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class GeminiItemVisionService implements ItemVisionService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiItemVisionService.class);

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String modelName;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiItemVisionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public ItemAnalysisResponse analyzeItemImage(byte[] imageBytes, String mimeType) throws AiServiceException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new AiServiceException("Image data is missing or empty.");
        }

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.trim().equals("your_gemini_api_key_here")) {
            logger.warn("GEMINI_API_KEY is not configured. Falling back to manual description mode.");
            ItemAnalysisResponse fallback = new ItemAnalysisResponse();
            fallback.setCategory("Unspecified");
            fallback.setDescription("Photo received. Please enter the item description and attributes manually.");
            fallback.setConfidence(0.0);
            fallback.setFallback(true);
            fallback.setNote("Vision AI key (GEMINI_API_KEY) is not configured in backend environment. Manual entry is enabled.");
            return fallback;
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String effectiveMimeType = (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg";

            String prompt = """
                You are a campus lost and found item identification assistant.
                Analyze this photo of an item. Return ONLY a single raw JSON object (no markdown, no backticks, no explanations) matching this exact format:
                {
                  "category": "category name (e.g. water bottle, laptop, keys, phone, jacket, umbrella, bag, wallet, glasses)",
                  "colour": ["primary colour", "accent colour"],
                  "brand": "brand name if clearly visible on the item, otherwise null",
                  "description": "Clear, objective, 1-2 sentence description of the item appearance",
                  "distinctiveFeatures": ["feature 1 (e.g. scratch, sticker, specific zipper)", "feature 2"],
                  "searchKeywords": ["keyword1", "keyword2", "keyword3"],
                  "confidence": 0.85
                }
                CRITICAL INSTRUCTIONS:
                - Never invent brands, serial numbers, or features not visible.
                - If brand is not visible, set brand to null.
                - Do NOT perform facial recognition or identify people.
                - Return purely the raw JSON object.
                """;

            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", effectiveMimeType);
            inlineData.put("data", base64Image);

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> imagePart = Map.of("inline_data", inlineData);

            Map<String, Object> content = Map.of("parts", List.of(textPart, imagePart));
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("contents", List.of(content));

            // Request JSON output
            Map<String, Object> generationConfig = Map.of("response_mime_type", "application/json");
            requestBodyMap.put("generationConfig", generationConfig);

            String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

            String[] modelCandidates = new String[] { modelName.trim(), "gemini-3.1-flash-lite", "gemini-3.8-flash", "gemini-flash-latest" };
            HttpResponse<String> response = null;
            String usedModel = modelName;

            for (String candidateModel : modelCandidates) {
                usedModel = candidateModel;
                String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    candidateModel, apiKey.trim()
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(45))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                        .build();

                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    break;
                }
                logger.warn("Model {} returned HTTP {}, checking alternate model if available...", candidateModel, response.statusCode());
                if (response.statusCode() != 404) {
                    // Not a model not found error, don't cascade on auth or bad request
                    break;
                }
            }

            if (response.statusCode() != 200) {
                logger.error("Gemini API returned status {}: {}", response.statusCode(), response.body());
                if (response.statusCode() == 429) {
                    throw new AiServiceException("Gemini AI API rate limit reached. Please wait a moment or fill details manually.");
                } else if (response.statusCode() == 400 || response.statusCode() == 403) {
                    throw new AiServiceException("Gemini AI API authorization or request failed (HTTP " + response.statusCode() + "). Please check your GEMINI_API_KEY.");
                } else {
                    throw new AiServiceException("Gemini AI API returned error HTTP " + response.statusCode());
                }
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new AiServiceException("AI service returned an empty response. Please review the item manually.");
            }

            String aiRawText = textNode.asText().trim();
            // Clean code fences if returned despite prompt
            if (aiRawText.startsWith("```json")) {
                aiRawText = aiRawText.substring(7);
            } else if (aiRawText.startsWith("```")) {
                aiRawText = aiRawText.substring(3);
            }
            if (aiRawText.endsWith("```")) {
                aiRawText = aiRawText.substring(0, aiRawText.length() - 3);
            }
            aiRawText = aiRawText.trim();

            JsonNode parsedJson = objectMapper.readTree(aiRawText);
            ItemAnalysisResponse result = new ItemAnalysisResponse();

            result.setCategory(parsedJson.path("category").asText("Uncategorized"));
            result.setDescription(parsedJson.path("description").asText(""));
            if (parsedJson.hasNonNull("brand")) {
                result.setBrand(parsedJson.path("brand").asText());
            }

            if (parsedJson.has("colour") && parsedJson.path("colour").isArray()) {
                List<String> colors = new ArrayList<>();
                for (JsonNode c : parsedJson.path("colour")) {
                    colors.add(c.asText().trim().toLowerCase());
                }
                result.setColour(colors);
            }

            if (parsedJson.has("distinctiveFeatures") && parsedJson.path("distinctiveFeatures").isArray()) {
                List<String> feats = new ArrayList<>();
                for (JsonNode f : parsedJson.path("distinctiveFeatures")) {
                    feats.add(f.asText().trim());
                }
                result.setDistinctiveFeatures(feats);
            }

            if (parsedJson.has("searchKeywords") && parsedJson.path("searchKeywords").isArray()) {
                List<String> kws = new ArrayList<>();
                for (JsonNode k : parsedJson.path("searchKeywords")) {
                    kws.add(k.asText().trim());
                }
                result.setSearchKeywords(kws);
            }

            double conf = parsedJson.path("confidence").asDouble(0.85);
            result.setConfidence(Math.max(0.1, Math.min(1.0, conf)));
            result.setFallback(false);
            result.setNote("Analyzed with " + modelName);

            return result;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error during image AI analysis", e);
            throw new AiServiceException("Failed to analyze image with AI: " + e.getMessage(), e);
        }
    }
}
