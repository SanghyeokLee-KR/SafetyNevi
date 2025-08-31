package com.inha.pro.safetynevi.specs;

import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * 재난 문자 검색 조건 (JPA Specification)
 * - 동적 쿼리 생성을 위한 Predicate 정의
 */
@Component
public class DisasterMessageSpecs {

    // 지역(Area) 부분 일치 검색 (LIKE '서울%')
    public static Specification<DisasterMessage> likeArea(String area) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get("area"), area + "%");
    }

    // 재난 유형(Type) 일치 검색 (Equal)
    public static Specification<DisasterMessage> equalDisasterType(String disasterType) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("disasterType"), disasterType);
    }
}