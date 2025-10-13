package com.library.service;

import com.library.dto.board.BoardListDTO;
import com.library.entity.board.Board;
import com.library.entity.board.BoardStatus;
import com.library.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                PageRequest.of(page,size, Sort.by("createdAt").descending());

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
}
