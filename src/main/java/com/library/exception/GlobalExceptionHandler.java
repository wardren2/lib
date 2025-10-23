package com.library.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;

/*
    전역 예외 처리 핸들러 - 애플리케이션 전체에서 발생하는 예외를 중앙에서 처리
    @ControllerAdvice
        - Spring의 AOP를 활용한 전역 예외 처리 매커니즘
            - 모든 @Controller에서 발생하는 예외를 한 곳에서 처리
            - 코드 중복 제거 및 일관된 에러 응답 제공
            - Controller에서 try~catch 불필요

    예외 처리 우선순위 ( 구체적인 것 => 일반적인 것 )
        1) InvalidFileException.java (파일 검증 실패)
        2) MaxUploadSizeExceededException (Spring 파일 크기 제한)
        3) RuntimeException (일반 런타임 에러)
        4) Exception (모든 예외의 최종 방어선)
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
}
