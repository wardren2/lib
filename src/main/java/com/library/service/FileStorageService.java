package com.library.service;

import com.library.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
    파일 저장 Service - 파일 업로드, 다운로드, 삭제 등의 파일 관리 기능 제공
        - 주요 기능
            - 1) 파일 검증 (확장자, 크기, 파일명)
            - 2) 파일 저장 (UUID 파일명, 날짜별 폴더 구조)
            - 3) 파일 다운로드 (Resource 반환)
            - 4) 파일 삭제 (물리적 삭제)
    @Value 어노테이션
        - Spring의 프로퍼티 값을 주입받는 어노테이션
        - 형식: @Value()

 */
@Service
@Slf4j
public class FileStorageService {
    private final Path uploadPath;      // 파일 저장 기본 경로 (절대 경로로 변환하여 저장)
    private final long maxFileSize;     // 최대 파일 크기 (바이트 단위, 기본값 10MB)
    private final Set<String> allowedExtensions;    // 허용된 파일 확장자 Set (Set - 중복 방지되고 검색이 빠름)

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadPath,
                              @Value("${file.max-size:10485760}") long maxFileSize,
                              @Value("${file.allowed-extensions:jpg,jpeg,png,pdf,gif,doc,docx,xls,xlsx,ppt,pptx,txt,zip,hwp}") String[] allowedExts) {
                            /* 📢 문제 발생과 해결1) : @Value 어노테이션으로 프로퍼티에서 허용 확장자를 불러올 때, 공백 없이 쉼표로만 구분해야 합니다. 예시: jpg, jpeg,(X) 예시: jpg,jpeg,(O) */
                            /* 📢 문제 발생과 해결2) : 매개변수명과 필드명이 같으면 매개변수가 필드를 가립니다(shadowing) 그러므로 String[] allowedExtensions)라고 쓰지 않고, String[] allowedExts라고 써야 Test에서 에러가 발생하지 않습니다. */
        /* 절대 경로로 변환 (uploads = > D:/Dev/temp/..../uploads)  */
        this.uploadPath = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize; // 파일 크기 제한 설정
        this.allowedExtensions = new HashSet<>(Arrays.asList(allowedExts)); // 허용확장자를 Set으로 변환 (배열보다 검색속도 빠름)

        try {
            Files.createDirectories(this.uploadPath);       // 업로드 없으면 자동 생성

            log.info("파일 저장 디렉토리 생성 완료 : {}", this.uploadPath);
            log.info("파일 크기 제한: {} bytes ({} MB)", this.maxFileSize, maxFileSize/1024/1024);
            log.info("허용된 확장자 : {}", allowedExtensions);

        }catch(IOException e){
                log.error("파일 저장 디렉토리 생성 실패", e);
                throw new RuntimeException("파일 저장 디렉토리를 생성할 수 없습니다.", e);
        }


    }

    /*
         파일 검증 - 확장자, 크기, 파일명 등을 검증하여 보안 위협 차단
    */
    public void validateFile(MultipartFile file){
        // 1. 파일 존재 여부 검증
        if (file == null || file.isEmpty()){
            throw new InvalidFileException("파일이 비었습니다.");
        }

        // 2. 파일 크기 검증(maxFileSize 초과시 예외 발생)
        if (file.getSize() > maxFileSize){
            throw new InvalidFileException(
                    String.format("파일 크기가 너무 큽니다. (최대: %d MB, 현재: %.2f MB)", maxFileSize / 1024 / 1024, file.getSize() / 1024/1024)
            );
        }

        // 3. 파일명 유효성 검증
        String originalFilename = file.getOriginalFilename();
        if(originalFilename == null | originalFilename.trim().isEmpty()){
            throw new InvalidFileException("파일명이 올바르지 않습니다.");
        }

        // 4. 파일 확장자 추출 및 검증
        String extension = getFileExtension(originalFilename);
        if (extension.isEmpty()){
            throw new InvalidFileException("파일 확장자가 없습니다.");
        }

        // 5. 허용된 확장자 목록에 있는지 확인 (대소문자 구분 없이 목록에 비교)
        if (! allowedExtensions.contains(extension.toLowerCase())){
            throw new InvalidFileException(
                    String.format("허용되지 않은 파일 형식입니다. (허용: %s, 현재: %s)",
                            allowedExtensions, extension)
            );
        }
        log.debug("파일 검증 성공: {} (크기: {} bytes, 확장자: {})",
                originalFilename, file.getSize(), extension);
    }

    /* 파일 저장 - UUID 파일명 생성 및 날짜별 폴더 구조로 저장
        저장 프로세스
            - 파일 검증 (validateFile())
            - UUID 생성하고 고유한 파일명 만들기
            - 날짜별 폴더 경로 생성 (yyyy/MM/dd 형식)
            - 디렉토리 생성 (없으면 자동 생성)
            - 파일 저장
     */

    /*
        파일 저장 - UUID 파일명 생성 및 날짜별 폴더 구조로 저장
     */
    public String[] storeFile(MultipartFile file, String subDirectory){
        // 1. 파일 검증
        validateFile(file);

        String originalFilename = file.getOriginalFilename();

        // 2. 파일 확장자 추출
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")){
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 3. UUID로 고유한 파일명 생성
        String storedFilename = UUID.randomUUID().toString() + extension;

        // 4. 날짜별 디렉토리 경로 생성 (boards/2025/10/15)
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = subDirectory +"/"+ datePath + "/";

        // 5. 전체 경로 생성 (기본 경로 + 상대 경로)
        Path targetLocation = this.uploadPath.resolve(relativePath).normalize();

        try{
            // 6. 디렉토리 생성 (부모 디렉토리도 함께 생성)
            Files.createDirectories(targetLocation);
            // 7. 파일 저장 (중복 시 덮어쓰기)
            Path filePath = targetLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("파일 저장 완료: {}",filePath);

            // 8. 저장된 파일 정보 반환 [파일명, 경로]
            return new String[]{storedFilename, relativePath};

        } catch (IOException e) {
            log.error("파일 저장 실패: {}", originalFilename);
            throw new RuntimeException("파일을 저장할 수 없습니다. "+ originalFilename);
        }

    }
    /*
        파일 다운로드 - 저장된 파일을 Resource로 반환
            - 동작 과정
                - 전체 파일 경로 생성 (기본경로 + 상대경로 + 파일명)
                - 파일을 UrlResource로 변환
                - 파일 존재 및 읽기 가능 여부 확인
                - Resource 반환 (HTTP 응답으로 전달)
            - Resource란?
                - Spring의 파일 추상화 인터페이스
                - 파일 시스템, 클래스패스, URL 등 다양한 위치의 리소스를 통일된 방식으로 다룸
                - 파일 다운로드 응답 생성
     */
    public Resource loadFileAsResource(String filePath, String storedFilename){
        try {
            // 1. 전체 파일 경로 생성 및 정규화
            Path file =     // uploads/boards/2025/10/15/uuid.pdf
            this.uploadPath.resolve(filePath).resolve(storedFilename).normalize();

            // 2. 파일을 Resources로 변환
            Resource resource = new UrlResource(file.toUri());

            // 3. 파일 존재 및 읽기 가능 여부 확인
            if (resource.exists() && resource.isReadable()){
                return resource;
            } else {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + storedFilename);
            }

        } catch (MalformedURLException e) {
            log.error("파일 다운로드 실패: {}", storedFilename, e);
            throw new RuntimeException("파일을 찾을 수 없습니다: " + storedFilename, e);
        }
    }

    /*
        파일 삭제 - 물리적으로 파일을 디스크에서 삭제
            - 사용 시점
                - 게시글 삭제 시 (연관된 파일을 모두 삭제)
                - 파일 수정 시 (기존 파일 삭제 후 새 파일 저장)
     */
    public void deleteFile(String filePath, String storedFilename){
        try{
            // 전체 파일 경로 생성 및 정규화
            Path file = this.uploadPath.resolve(filePath).resolve(storedFilename).normalize();

            // 파일 삭제 (파일이 없어도 예외 발생하지 않음)
            Files.deleteIfExists(file);
            log.info("파일 삭제 완료: {}", file);

        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", storedFilename, e);
            // 파일 삭제 실패는 로그만 남기고 예외를 던지지 않음
        }
    }

    /*
        파일 확장자 추출 - 파일명에서 확장자를 추출하여 소문자로 반환
            - 처리 과정
                - 1. 파일명 null/empty 체크
                - 2. 마지막 점(.) 위치 찾기
                - 3. 점 이후 문자열 추출 (점은 제외)
                - 4. 소문자로 변환하여 반환
     */
    public String getFileExtension(String filename){
        // 1. 파일명 null/empty 체크
        if (filename == null || filename.trim().isEmpty()){
            return "";
        }

        // 2. 마지막 점(.) 위치 찾기
        int lastDotIndex =  filename.lastIndexOf(".");

        // 3. 점이 없거나 파일명 끝에 점이 있는 경우 (예: "readme" 또는 "file.")
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {    // -1 : 없음  또는  파일명 마지막에 위치
            return "";
        }

        // 4. 점 이후 문자열 추출 후 소문자로 변환 (예: '.PDF' => 'pdf")
        return filename.substring(lastDotIndex+1).toLowerCase();
    }
}
