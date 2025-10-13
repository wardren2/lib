package com.library.controller;

import com.library.dto.board.BoardListDTO;
import com.library.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
/*
    게시글 Controller
        - 게시글 관련 HTTP 요청을 처리하고 뷰를 반환
        - URL 매핑 : /boards
 */

@Controller                     // Spring MVC Controller로 등록
@RequestMapping("/boards")      // 기본 URL 매핑 : /boards
@RequiredArgsConstructor        // final 필드에 대한 생성자 자동 생성 (DI)
public class BoardController {

    private final BoardService boardService;        // 게시글 Service(DI)
    /*
        게시글 목록 페이지
            - ACTIVE 상태의 게시글 목록을 페이징으로 조회
            - View와 로직 분리 우너칙에 따라 페이징 계산은 Controller에서 처리함

        1-based 페이징 시스템 (URL: page=1부터 시작)
            - URL : GET /boards?page=1&size=10
     */

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "1") int page,     //조회할 페이지 번호 (기본값:1)
            @RequestParam(defaultValue = "10") int size,    //페이지당 보여줄 게시글 갯수 (기본값: 10개)
            Model model                                     //뷰에 데이터 전달용 Model
    ) {
        /* Service를 통해 게시글 목록 조회 (page -1 을 전달하여 0-based로 전환)
            Controller에서 들어오는 요청 예: /boards?page=1
            → 즉, page = 1이에요 (사람 기준으로 첫 페이지).


            하지만 JPA에 그대로 넘기면 JPA는 “두 번째 페이지로 가라”고 착각해요.
            왜냐면 JPA는 0이 첫 페이지라고 생각하거든요.

            그래서 맞춰주기 위해 이렇게 조정함
         */
        Page<BoardListDTO> boards = boardService.getBoardList(page-1, size);    //size는 10개 그대로

        // 전체 페이지 수
        int totalPages = boards.getTotalPages();        // 전체 페이지수

        int pageGroupSize = 10;         // 한 그룹에 표시하 페이지 버튼 개수
        int currentGroup = (page-1) / pageGroupSize;    // 현재 페이지가 속한 그룹 번호
        
        // 그룹의 시작 페이지 번호
        int startPage = currentGroup * pageGroupSize + 1;   // 그룹의 시작 페이지 번호
        int endPage = Math.min(startPage + pageGroupSize -1, totalPages); // 그룹의 종료 페이지 번호

        boolean hasPrevGroup = startPage > 1;   // [이전 그룹] 버튼 표시 여부
        int prevGroupPage = startPage - 1;  // 이전 그룹의 마지막 페이지
        boolean hasNextGroup = endPage < totalPages; // [다음 그룹] 버튼 표시 여부
        int nextGroupPage = endPage + 1;    // 다음 그룹의 첫 페이지
        
        
        // Model 데이터 추가 (Thymeleaf로 전달)
        model.addAttribute("boards", boards); // 게시글 목록
        model.addAttribute("currentPage", page);    //현재 페이지 번호(1-based)
        model.addAttribute("totalPages", totalPages);   //전체 페이지수
        model.addAttribute("totalElements", boards.getTotalElements()); // 전체 게시글 수

        model.addAttribute("startPage", startPage); // 그룹 시작 페이지(1-based)
        model.addAttribute("endPage", endPage); // 그룹 종료 페이지(1-based)

        model.addAttribute("hasPrevGroup", hasPrevGroup);
        model.addAttribute("hasNextGroup", hasNextGroup);
        model.addAttribute("prevGroupPage", prevGroupPage); // 이전 그룹으로 이동할 때 페이지 번호
        model.addAttribute("nextGroupPage", hasNextGroup); // 다음 그룹으로 이동할 때 페이지 번호

        return "board/list";    // 게시글 목록 뷰
    }

}




















