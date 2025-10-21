package com.inha.pro.safetynevi.dto.map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class BoardRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @NotBlank(message = "카테고리를 선택해주세요.")
    private String category;

    @NotNull(message = "위치 정보(위도)가 누락되었습니다.")
    private Double latitude;

    @NotNull(message = "위치 정보(경도)가 누락되었습니다.")
    private Double longitude;

    private String locationType; // 위치 지정 방식 (GPS / MANUAL)

    // 첨부 이미지는 선택 사항
    private MultipartFile imageFile;
}