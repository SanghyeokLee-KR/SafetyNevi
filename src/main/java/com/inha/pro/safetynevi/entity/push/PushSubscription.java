package com.inha.pro.safetynevi.entity.push;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 표준 Web Push 구독 한 건(브라우저·기기별). 페이지에 접속하지 않은 사용자에게도 재난 푸시를 보내기 위한 발송 대상.
// endpoint = 브라우저 푸시 서비스 주소, p256dh·auth = 페이로드 암호화용 공개키/인증 시크릿.
@Getter
@Setter
@Entity
@Table(name = "PUSH_SUBSCRIPTION")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "push_subscription_seq")
    @SequenceGenerator(name = "push_subscription_seq", sequenceName = "PUSH_SUBSCRIPTION_SEQ", allocationSize = 1)
    private Long id;

    // 푸시 서비스 endpoint. 기기마다 하나, 재발급되면 갱신되므로 unique 로 잡고 upsert 한다.
    @Column(name = "ENDPOINT", nullable = false, unique = true, length = 512)
    private String endpoint;

    @Column(name = "P256DH", nullable = false, length = 256)
    private String p256dh;

    @Column(name = "AUTH", nullable = false, length = 128)
    private String auth;

    // 구독한 회원. 비로그인 구독도 허용하므로 null 가능.
    @Column(name = "USER_ID", length = 50)
    private String userId;

    // 관심 지역. 지역별 발송(다음 단계)용으로 회원 주소에서 채워둔다.
    @Column(name = "AREA_NAME", length = 100)
    private String areaName;

    @Column(name = "USER_AGENT", length = 300)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
