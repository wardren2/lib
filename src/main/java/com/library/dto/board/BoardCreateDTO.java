package com.library.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/*
    게시글 작성 요청 DTO (Data Transfer Object)
    
    DTO의 역할
        - 클라이언트(웹 브라우저)와 서버 간의 데이터 전송 객체
        - Entity와 분리하여 계층 간 결합도 감소
        - 필요한 데이터만 선택적으로 전송
        - Validation을 통한 입력값 검증

    Entity vs DTO
        - Entity (Board)
            - 데이터베이스 테이블과 1:1 매핑
            - JPA과 관리하는 영속성(Persistence) 객체에 해당
            - id, createdAt, viewCount 등 자동 생성 필드 포함
        - DTO (BoardCreateDTO)
            - 사용자 입력값만 포함
            - title, content, category, files만 받음
            - 작성자 정보는 Security Context에서 가져옴 (로그인 사용자)
            - 반드시 @Setter가 필요함
        - 사용 흐름
            - 사용자 입력 (웹 폼)
            - 데이터가 DTO와 바인딩 됨 (BoardCreateDTO)
            - 입력값에 대해서 Validation 검증 (@Valid)
            - BoardService.createBoard()
            - Board Entity 생성 (DTO => Entity 변환)
            - DB 저장
    
 */
@Builder
public class BoardCreateDTO {
    // title, content, category, files만 받음

    @NotBlank(message = "제목은 필수 입력 사항입니다.")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입력 사항입니다.")
    private String content;

    @NotBlank(message = "카테고리 필수 입력 사항입니다.")
    private String category;
    
    @Builder.Default    // Builder 사용 시 기본값 지정. (비어있는 리스트) : 첨부파일은 없을 수 있다
    private List<MultipartFile> files = new ArrayList<>();

}
