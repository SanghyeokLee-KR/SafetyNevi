package com.inha.pro.safetynevi.service.dashboard;

import com.inha.pro.safetynevi.dao.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MemberRepository mrepo;

    public Map<String, Object> dashboardChart() {
        Map<String, Object> resultMap = new HashMap<>();

        long totalCount = mrepo.count();

        // 오늘 가입자 수
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todayCount = mrepo.countByJoinDateBetween(startOfDay, endOfDay);

        // 지역별 가입자 집계 - 주소 첫 토큰("서울특별시" 등)만 떼서 시/도 단위로 묶음
        List<String> addressList = mrepo.findAllAddresses();
        Map<String, Long> areaMap = new HashMap<>();

        for (String address : addressList) {
            if (address == null || address.trim().isEmpty()) continue;

            String[] tokens = address.split(" ");
            if (tokens.length > 0) {
                String normalizedRegion = normalizeRegion(tokens[0]);
                areaMap.put(normalizedRegion, areaMap.getOrDefault(normalizedRegion, 0L) + 1);
            }
        }

        resultMap.put("totalCount", totalCount);
        resultMap.put("todayCount", todayCount);
        resultMap.put("areaCounts", areaMap);

        return resultMap;
    }

    // 시/도 표기가 제각각이라("충북"/"충청북도" 등) 차트용으로 통일
    private String normalizeRegion(String rawRegion) {
        if (rawRegion.startsWith("서울")) return "서울";
        if (rawRegion.startsWith("경기")) return "경기";
        if (rawRegion.startsWith("인천")) return "인천";
        if (rawRegion.startsWith("부산")) return "부산";
        if (rawRegion.startsWith("대구")) return "대구";
        if (rawRegion.startsWith("광주")) return "광주";
        if (rawRegion.startsWith("대전")) return "대전";
        if (rawRegion.startsWith("울산")) return "울산";
        if (rawRegion.startsWith("세종")) return "세종";
        if (rawRegion.startsWith("강원")) return "강원";
        if (rawRegion.startsWith("충북") || rawRegion.startsWith("충청북")) return "충북";
        if (rawRegion.startsWith("충남") || rawRegion.startsWith("충청남")) return "충남";
        if (rawRegion.startsWith("전북") || rawRegion.startsWith("전라북")) return "전북";
        if (rawRegion.startsWith("전남") || rawRegion.startsWith("전라남")) return "전남";
        if (rawRegion.startsWith("경북") || rawRegion.startsWith("경상북")) return "경북";
        if (rawRegion.startsWith("경남") || rawRegion.startsWith("경상남")) return "경남";
        if (rawRegion.startsWith("제주")) return "제주";

        return rawRegion;
    }
}