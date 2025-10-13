package com.library.controller;

import com.library.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller                     // Spring MVC Controller로 등록
@RequestMapping("/boards")      // 기본 URL 매핑 : /boards
@RequiredArgsConstructor        // final 필드에 대한 생성자 자동 생성 (DI)
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,     //페이지 번호 (기본값:0)
            @RequestParam(defaultValue = "10") int size,    //한 페이지에 보여줄 게시글 갯수 (기본값: 10개)
            Model model                                     //뷰에 데이터 전달용 Model
    ) {
        // Service를 통해 게시글 목록 조회


        return "board/list";
    }

}
