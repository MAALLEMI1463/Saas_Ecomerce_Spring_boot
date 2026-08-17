package com.example.demo.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    // Images stored inside the project under src/main/resources/static/images
    private final Path storageLocation = Paths.get("src/main/resources/static/images");

    public FileStorageService() {
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create image storage directory.", e);
        }
    }

    public String saveImage(MultipartFile file) {
        try {
            // Generate unique filename to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;

            Path targetPath = storageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return filename; // Only the filename is stored in DB
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image file.", e);
        }
    }

    public void deleteImage(String filename) {
        try {
            Path filePath = storageLocation.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image file.", e);
        }
    }
}