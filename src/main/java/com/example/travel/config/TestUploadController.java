package com.example.travel.config;

import com.example.travel.review.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestUploadController {
    
    private final FileStorageService fileStorageService;
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> testUpload(@RequestParam("file") MultipartFile file) {
        log.info("테스트 업로드 요청 받음: {}, 크기: {}, 타입: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String filePath = fileStorageService.storeFile(file, "reviews");
            log.info("테스트 파일 저장 성공: {}", filePath);
            
            response.put("success", true);
            response.put("filePath", filePath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("테스트 파일 업로드 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 