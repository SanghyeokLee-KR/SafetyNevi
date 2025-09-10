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

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final DisasterMessageRepository disasterMessageRepository;
    private final DisasterService disasterService;
    private final AiClientService aiClientService;

    // 1분마다 실행. 외부 I/O(크롤링·AI) 중에는 DB 커넥션을 잡지 않으려고 메서드 트랜잭션은 두지 않음
    @Scheduled(fixedDelay = 60000)
    public void crawlAndSaveDisasterMessage() {
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
            log.info("새 메시지 저장: {} ({})", newMessage.getArea(), newMessage.getDisasterType());

            // AI 분석 후 위험하면 지도에 표시
            analyzeAndTriggerDisaster(newMessage);

        } catch (IOException e) {
            log.error("크롤링 오류", e);
        }
    }

    private void analyzeAndTriggerDisaster(DisasterMessage msg) {
        // 1. AI 서버에 위험도 분석 요청
        boolean isDangerous = aiClientService.isCritical(msg.getContent());

        if (isDangerous) {
            log.info("위험 판정 - 재난 영역 생성");

            // 좌표 없이 지역명만 저장 -> 프론트에서 폴리곤으로 표시
            disasterService.createAreaDisaster(
                    msg.getArea(),         // 지역명 (예: 경상북도 경주시 -> geojson 매핑됨)
                    msg.getDisasterType(), // 재난유형 (예: 호우, 지진 -> 색상 결정)
                    60                     // 지속시간 (60분)
            );
        } else {
            log.info("안전 판정 - 표시 안 함");
        }
    }

    private String getText(Document doc, String selector) {
        if (doc.selectFirst(selector) != null) return doc.selectFirst(selector).text();
        return "정보 없음";
    }
}