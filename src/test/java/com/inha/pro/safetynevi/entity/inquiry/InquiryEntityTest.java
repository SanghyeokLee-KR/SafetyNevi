package com.inha.pro.safetynevi.entity.inquiry;

import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryEntityTest {

    // 폼에 status·answerContent를 끼워 넣어도 toEntity는 무시하고 항상 대기·답변없음으로 만든다 (답변 위조 차단)
    @Test
    void toEntity는_폼의_status와_answer를_무시하고_대기상태로_고정한다() {
        InquiryDTO dto = new InquiryDTO();
        dto.setTitle("문의 제목");
        dto.setContent("문의 내용");
        dto.setCategory("결제");
        dto.setWriterId("user1");
        dto.setWriterName("홍길동");
        dto.setIsSecret(1);
        // 공격자가 끼워 넣었다고 가정한 값
        dto.setStatus("COMPLETED");
        dto.setAnswerContent("관리자인 척 위조한 답변");

        InquiryEntity entity = InquiryEntity.toEntity(dto);

        assertThat(entity.getStatus()).isEqualTo(InquiryEntity.InquiryStatus.WAITING);
        assertThat(entity.getAnswerContent()).isNull();
        // 정상 필드는 그대로 전달
        assertThat(entity.getTitle()).isEqualTo("문의 제목");
        assertThat(entity.getIsSecret()).isEqualTo(1);
    }

    @Test
    void toEntity는_isSecret이_null이면_0으로_채운다() {
        InquiryDTO dto = new InquiryDTO();
        dto.setTitle("t");
        dto.setContent("c");
        dto.setCategory("기타");
        dto.setWriterId("u");
        dto.setWriterName("n");
        dto.setIsSecret(null);

        InquiryEntity entity = InquiryEntity.toEntity(dto);

        assertThat(entity.getIsSecret()).isEqualTo(0);
        assertThat(entity.getStatus()).isEqualTo(InquiryEntity.InquiryStatus.WAITING);
    }
}
