package com.library.entity.board;

import com.library.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

// 게시글 첨부파일 Entity - 게시글에 첨부된 파일 정보를 관리하며, Board와 다대일 관계
@Entity
@Table(name="board_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 게시글 (Board와 N:1 관계) - 게시글 삭제시 첨부파일도 함께 삭제됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length=255)
    private String originalFilename;    // 원본 파일명 (사용자가 업로드한 파일 원래 이름)

    @Column(nullable = false, length=255)
    private String storedFilename;      // 저장된 파일명 (UUID로 저장 - 파일명 중복 방지 목적)
}
