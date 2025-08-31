package com.inha.pro.safetynevi.dto.crawling;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "DM")
@NoArgsConstructor
public class DisasterMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dmid;

    private String disasterType; // 재난 상황
    private String area; // 지역
    private String sentDate; // 날짜

    @Lob // 내용이 길 수 있으므로 Lob 타입을 사용합니다.
    private String content; // 내용

    // DTO를 Entity로 변환하는 생성자
    public DisasterMessage(DisasterMessageDto dto) {
        this.disasterType = dto.getDisasterType();
        this.area = dto.getArea();
        this.sentDate = dto.getSentDate();
        this.content = dto.getContent();
    }
}