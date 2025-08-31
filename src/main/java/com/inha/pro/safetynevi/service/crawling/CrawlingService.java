package com.inha.pro.safetynevi.service.crawling;

import com.inha.pro.safetynevi.dao.crawling.DisasterMessageRepository;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;
import com.inha.pro.safetynevi.service.ai.AiClientService;
import com.inha.pro.safetynevi.service.calamity.DisasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final DisasterMessageRepository disasterMessageRepository;
    private final DisasterService disasterService;
    private final AiClientService aiClientService;

    // 1분마다 실행
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void crawlAndSaveDisasterMessage() {
        // [테스트용] 강제 화재 경보 테스트 (필요시 주석 해제)
        // forceTestFireDisaster();
        // return;

        String url = "https://search.naver.com/search.naver?where=nexearch&query=%EC%9E%AC%EB%82%9C%EB%AC%B8%EC%9E%90";

        try {
            Document doc = Jsoup.connect(url).get();

            String disasterType = getText(doc, ".inner .disaster_info .disaster_type .text");
            String area = getText(doc, ".inner .disaster_info .info_box .area");
            String sentDate = getText(doc, ".inner .disaster_info .info_box .date");
            String content = getText(doc, ".inner .disaster_text");

            DisasterMessageDto crawledDto = new DisasterMessageDto(disasterType, area, sentDate, content);

            // 중복 방지
            DisasterMessage lastMessage = disasterMessageRepository.findTopByOrderByDmidDesc();
            if (lastMessage != null &&
                    lastMessage.getContent().equals(crawledDto.getContent()) &&
                    lastMessage.getSentDate().equals(crawledDto.getSentDate())) {
                return;
            }

            // DB 저장
            DisasterMessage newMessage = new DisasterMessage(crawledDto);
            disasterMessageRepository.save(newMessage);
            log.info("📥 [크롤링] 새 메시지 저장: {} ({})", newMessage.getArea(), newMessage.getDisasterType());

            // 🌟 AI 분석 및 지도 표시 연결
            analyzeAndTriggerDisaster(newMessage);

        } catch (IOException e) {
            log.error("❌ 크롤링 오류: ", e);
        }
    }

    private void analyzeAndTriggerDisaster(DisasterMessage msg) {
        // 1. AI 서버에 위험도 분석 요청
        boolean isDangerous = aiClientService.isCritical(msg.getContent());

        if (isDangerous) {
            log.info("🚨 [AI 판단] 'DANGER' -> 지도에 폴리곤(영역) 생성 요청");

            // 🌟 [핵심] createAreaDisaster를 호출합니다.
            // 이 메서드는 lat/lon을 null로 저장하므로,
            // 프론트엔드(JS)에서 "광범위한 지역 재난입니다" 알림을 띄우고 폴리곤을 그립니다.
            disasterService.createAreaDisaster(
                    msg.getArea(),         // 지역명 (예: 경상북도 경주시 -> geojson 매핑됨)
                    msg.getDisasterType(), // 재난유형 (예: 호우, 지진 -> 색상 결정)
                    60                     // 지속시간 (60분)
            );
        } else {
            log.info("✅ [AI 판단] 'SAFE' -> 지도 표시 안함");
        }
    }

    private String getText(Document doc, String selector) {
        if (doc.selectFirst(selector) != null) return doc.selectFirst(selector).text();
        return "정보 없음";
    }

    // [테스트] 강제로 '호우' 경보를 발생시켜 폴리곤이 그려지는지 확인
    private void forceTestFireDisaster() {
        System.out.println("🧪 [테스트] 강제 호우 경보 발령 (지역: 서울특별시)");

        DisasterMessage fakeMsg = new DisasterMessage();
        fakeMsg.setContent("[화재경보] 강원도 전역에 화재 발생 긴급 상황.");
        fakeMsg.setArea("강원도"); // GeoJSON에 있는 정확한 지역명이어야 폴리곤이 그려짐
        fakeMsg.setDisasterType("화재");

        analyzeAndTriggerDisaster(fakeMsg);
    }
}