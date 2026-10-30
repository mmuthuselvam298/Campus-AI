package com.campus.lostfound.service;

import com.campus.lostfound.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
            logger.info("Upload directory initialized at: {}", this.uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.uploadPath, e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Cannot upload an empty file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("Unsupported file type. Please upload a valid JPG, PNG, or WebP image.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                extension = ext;
            }
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetLocation = this.uploadPath.resolve(storedFilename).normalize();

        // Prevent path traversal
        if (!targetLocation.startsWith(this.uploadPath)) {
            throw new ValidationException("Invalid target file path");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored uploaded image at: {}", targetLocation);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + storedFilename, e);
        }
    }

    public Path loadFileAsPath(String filename) {
        Path filePath = this.uploadPath.resolve(filename).normalize();
        if (!filePath.startsWith(this.uploadPath) || !Files.exists(filePath)) {
            throw new ValidationException("File not found or invalid path: " + filename);
        }
        return filePath;
    }

    public byte[] loadFileAsBytes(String filename) throws IOException {
        Path path = loadFileAsPath(filename);
        return Files.readAllBytes(path);
    }
}
