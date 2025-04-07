package com.example.travel.review.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * 파일을 저장하고 저장된 파일의 URL을 반환합니다.
     * 
     * @param file   저장할 파일
     * @param subDir 저장할 하위 디렉토리 (예: 'reviews', 'profiles')
     * @return 저장된 파일의 상대 경로
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    public String storeFile(MultipartFile file, String subDir) {
        try {
            // 입력 파일 검증
            if (file == null || file.isEmpty()) {
                log.warn("빈 파일을 저장할 수 없습니다. file is null: {}", file == null);
                throw new IOException("빈 파일을 저장할 수 없습니다.");
            }

            log.info("파일 저장 시작 - 원본 파일명: {}, 크기: {}, 컨텐츠 타입: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            log.info("저장 디렉토리: {}, 하위 디렉토리: {}", uploadDir, subDir);

            // 저장 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir, subDir);
            log.info("업로드 전체 경로: {}", uploadPath.toAbsolutePath());

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("디렉토리 생성됨: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("디렉토리 이미 존재함: {}", uploadPath.toAbsolutePath());
            }

            // 원본 파일명 추출
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 고유한 파일명 생성
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            log.info("생성된 고유 파일명: {}, 전체 파일 경로: {}", uniqueFilename, filePath.toAbsolutePath());

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 완료: {}", filePath.toAbsolutePath());

            // 저장된 파일이 실제로 존재하는지 확인
            if (Files.exists(filePath)) {
                log.info("파일 저장 확인됨: {}, 크기: {}", filePath.toAbsolutePath(), Files.size(filePath));
            } else {
                log.warn("파일이 저장되지 않음: {}", filePath.toAbsolutePath());
            }

            // 웹에서 접근 가능한 상대 경로 반환
            String relativePath = "/" + subDir + "/" + uniqueFilename;
            log.info("반환되는 파일 경로: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("파일을 저장할 수 없습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 파일을 삭제합니다.
     * 
     * @param fileUrl 삭제할 파일의 URL
     * @return 삭제 여부
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            // URL에서 파일 경로 추출
            String filePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path fullPath = Paths.get(uploadDir, filePath);

            // 파일 삭제
            return Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 발생: {}", fileUrl, e);
            return false;
        }
    }
}