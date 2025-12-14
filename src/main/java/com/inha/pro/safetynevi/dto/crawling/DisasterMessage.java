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

    private Long sn; // 재난문자 API 일련번호. 중복 방지용 자연키로 씀

    private String disasterType;
    private String emergencyLevel; // 공식 긴급단계(위급/긴급/안전안내), 진짜 위험도, AI 추정 아님
    private String area;
    private String sentDate;

    @Lob // 본문이 길어질 수 있음
    private String content;

    public DisasterMessage(DisasterMessageDto dto) {
        this.disasterType = dto.getDisasterType();
        this.emergencyLevel = dto.getEmergencyLevel();
        this.area = dto.getArea();
        this.sentDate = dto.getSentDate();
        this.content = dto.getContent();
    }
}