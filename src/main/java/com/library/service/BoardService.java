package com.library.service;

import com.library.dto.board.BoardCreateDTO;
import com.library.dto.board.BoardDetailDTO;
import com.library.dto.board.BoardListDTO;
import com.library.entity.board.Board;
import com.library.entity.board.BoardFile;
import com.library.entity.board.BoardStatus;
import com.library.entity.member.Member;
import com.library.repository.BoardRepository;
import com.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/*
    게시글 Service
        - 게시글 관련 비즈니스 로직을 처림함
        - 트랜잭션 관리 및 Entity와 DTO 간 변환을 담당함
        - N + 1 문제 해결
            - 게시글 목록 조회 시 작성자 정보(author)도 함께 조회
            - Fetch Join을 사용하는 Repository 메서드 활용
            - 추가 쿼리 없이 한 번의 Join 쿼리로 모든 데이터 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    /*
        게시글 목록 조회 (페이징)
            - ACTIVE 상태의 게시글만 조회하며, 최신순으로 정렬함
            - Entity를 DTO로 변환하여 반환함
            - N + 1 문제 해결
                - findByStatusWithAuthor() 메서드 사용
                - Board와 Member를 JOIN으로 한 번에 조회
                - BoardListDTO 변환 시 author.getName() 호출해도 추가 쿼리 없음

        @param page 조회할 페이지 번호 (0부터 시작)
        @param size 페이지당 게시글 수
        @return 페이징된 게시글 목록 (BoardListDTO)
     */
    public Page<BoardListDTO> getBoardList(int page, int size) {
        // 페이징 정보 생성 (페이지 번호, 크기, 정렬 조건)
        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        // ACTIVE 상태의 게시글 조회 (작성자 정보 포함 - Fetch Join)
        Page<Board> boards =
                boardRepository.findByStatusWithAuthor(BoardStatus.ACTIVE, pageable);

        /*
            Entity를 DTO로 변환하여 반환
             Page.map() : Page 내부의 각 Board Entity를 BoardListDTO로 변환
             BoardListDTO::from - 메서드 레퍼런스 (board -> BoardListDTO.from(board))
         */
        return boards.map(BoardListDTO::from);
    }

    /*
        게시글 상세 조회
            - ACTIVE 상태의 게시글만 조회
            - 조회수를 1 증가시킴 (더티체킹으로 자동 반영)
        @Transactional (더티체킹의 핵심)
            - readOnly = false (기본값)
                - 조회수 증가를 위해서는 readOnly = false (기본값)을 유지해야 함.
                -> 즉, readOnly=true를 사용하지 않는다
                - 트랜잭션 내에서 엔터티 변경 => 더티체킹으로 자동 UPDATE
     */
    @Transactional      // 더티체킹을 위해 반드시 필요
    public BoardDetailDTO getBoard(Long id) {

        // 1. DB에서 게시글 조회
        Board board = boardRepository.findByIdAndStatusWithAuthor(id, BoardStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 2. 조회수 증가 (메모리상에서만 증가) -> 이 시점에는 DB에 반영X
        board.increaseViewCount();

        // 3. Entity를 DTO로 변환해서 반환
        return BoardDetailDTO.from(board);
        // 4. 메서드 종료 - 트랙잭션이 커밋 직전 더티체킹 실행
        // JPA가 스냅샷과 현재 엔터티를 비교하여 viewCount 변경 감지
        // UPDATE board SET view_count=?, updated_at=? WHERE id=?
    }

    /*
        게시글 작성
            - 새로운 게시글을 생성하여 DB에 저장함
            - 현재 로그인한 사용자를 작성자로 설정함
                - Spring Security에서 현재 로그인한 사용자의 이메일 가져옴
            - 첨부파일이 있으면 함께 저장함
                - 파일 저장 처리
                    - MultipartFile 리스트를 순회하며 각 파일을 저장
            - 초기 상태는 ACTIVE, 조회수/좋아요는 0으로 설정됨
            
            - @Transactional
                - readOnly = false (기본값)
                - 쓰기 작업이므로 트랜잭션 필요
                - save() 호출 후 자동으로 커밋

            - 동작 과정
                - 1) Spring Security (SecurityContext)에서 현재 사용자 이메일 추출
                - 2) 이메일로 Member 조회
                - 3) DTO 데이터 + Member로 Board 엔터티 생성
                - 4) 첨부파일이 있으면:
                    - 각 파일을 서버에 저장
                    - BoardFile 엔터티 생성
                    - Board에 파일 추가 (양방향 관계 설정)
                - 5) boardRepository.save()로 DB 저장 (파일도 함께 저장됨)
                - 6) Insert board, board_file 쿼리 실행
                - 7) 생성된 게시글의 ID 반환
    */
    @Transactional
    public Long createBoard(BoardCreateDTO createDTO, String userEmail) {
        // 1) 현재 로그인한 사용자 정보 조회
        Member author = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2) Board 엔터티 생성
        Board board = Board.builder()
                .title(createDTO.getTitle())
                .content(createDTO.getContent())
                .category(createDTO.getCategory())
                .author(author)
                .status(BoardStatus.ACTIVE)
                .viewCount(0L)
                .likeCount(0L)
                .build();

        // 3) 첨부파일 처리
        if (createDTO.getFiles() != null && !createDTO.getFiles().isEmpty()) {
            for (MultipartFile file : createDTO.getFiles()) {
                // 빈 파일을 건너뛰기
                if (file.isEmpty()) {
                    continue;
                }
                // 파일을 서버에 물리적으로 저장
                String[] fileInfo = fileStorageService.storeFile(file, "boards");
                String storedFilename = fileInfo[0];        // 저장된 파일명  (UUID + 확장자)
                String filePath = fileInfo[1];              // 파일이 저장된 전체 경로

                BoardFile boardFile = BoardFile.builder()   //BoardFile 엔터티 빌더 시작
                        .originalFilename(file.getOriginalFilename())  // 사용자가 업로드한원본
                        .storedFilename(storedFilename) // 서버에 저장된 고유 파일명
                        .filePath(filePath)     // 파일의 전체 저장 경로
                        .fileSize(file.getSize())   // 파일 크기 (바이트 단위)   
                        .fileExtension(fileStorageService.getFileExtension(file.getOriginalFilename()))     // 파일 확장자
                        .mimeType(file.getContentType())    //파일의 MIME 타입 (예:image/png)
                        .downloadCount(0L)  // 다운로드 회수 초기값 0
                        .build();       // BoardFile 엔터티 생성 완료
                board.addFile(boardFile);  // Board와 BoardFile 양방향 연관관계 설정
            }
        }
        // 4) DB에 저장 (cascade로 파일도 함께 저장됨)
        Board savedBoard = boardRepository.save(board);

        // 5) 생성된 게시글 ID 반환
        return savedBoard.getId();
    }

    /*
        게시글 삭제 (Soft Delete)
            - 실제 데이터를 삭제하지 않고 상태만 DELETED로 변경함
            - 작성자 본인만 삭제할 수 있음 (권한 검증)
            - 더티체킹으로 상태변경이 DB에 자동 반영
            - 장점 : 데이터 복구 가능, 감사 추적 유지, 통계 데이터 보존, 외래키 제약 조건 유지
    */
    @Transactional
    public void deleteBoard(Long id, String userEmail){
        // 1) 게시글 조회 (작성자 정보 포함 - Fetch Join)
        Board board = boardRepository.findByIdAndStatusWithAuthor(id, BoardStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다.")); /*없으면*/

        // 2) 권한 검증 - 작성자 본인만 삭제 가능
        if(!board.getAuthor().getEmail().equals(userEmail)){
            throw new RuntimeException("게시글을 삭제할 권한이 없습니다.");
        }

        // 3) Soft Delete 실행 (상태만 변경)
        board.delete();

        // 4) 메서드 종료 - 트랜잭션 커밋 직전 더티체킹 실행
        /*
            JPA과 스냅샷과 함께 엔터티를 비교하여 status 변경 감지
            UPDATE board SET status='DELETED', updated_at? WHERE id=?
         */
    }
}
