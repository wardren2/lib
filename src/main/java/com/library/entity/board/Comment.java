package com.library.entity.board;

import com.library.entity.base.BaseEntity;
import com.library.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Table;

/*
    댓글 Entity
        - 게시글에 대한 댓글 정보를 관리함
        - BaseEntity를 상속받아 생성일시/수정일시가 자동 관리됨
        - 게시글(Board)와 다대일(N:1) 연관관계를 가짐
        - 작성자(Member)와 다대일(N:1) 연관관계를 가짐
 */
@Entity
@Table(name ="comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=1000)
    private String content;     // 댓글 내용. 필수 입력 항목

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;        // 댓글이 속한 게시글 (Board와 N:1 관계)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;        // 댓글의 작성자 (Board와 N:1 관계)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommentStatus status = CommentStatus.ACTIVE;    // 활성 상태

    // 댓글 수정
    public void update(String content){
        this.content = content;
    }

    // 댓글 삭제 (소프트 삭제)
    public void delete(){
        this.status = CommentStatus.DELETED;
    }
    
    // 연관관계 메서드
}
