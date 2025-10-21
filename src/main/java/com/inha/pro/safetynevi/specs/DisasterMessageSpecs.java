package com.inha.pro.safetynevi.specs;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

// 재난문자 동적 검색 조건들
@Component
public class DisasterMessageSpecs {

    // 지역명 앞부분 매칭 (LIKE '서울%')
    public static Specification<DisasterMessage> likeArea(String area) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get("area"), area + "%");
    }

    public static Specification<DisasterMessage> equalDisasterType(String disasterType) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("disasterType"), disasterType);
    }
}