package com.inha.pro.safetynevi.controller.crawling;

import com.inha.pro.safetynevi.dao.crawling.DisasterMessageRepository;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessageDto;
import com.inha.pro.safetynevi.specs.DisasterMessageSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class DisasterMessageController {

    private final DisasterMessageRepository disasterMessageRepository;

    public DisasterMessageController(DisasterMessageRepository disasterMessageRepository) {
        this.disasterMessageRepository = disasterMessageRepository;
    }

    @GetMapping("/disasterMessage")
    public String disasterMessages(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "전국") String area,
                                   @RequestParam(defaultValue = "전체") String disasterType) {

        // 발령시각(sentDate) 최신순. 같은 시각이면 저장순(dmid)으로 안정 정렬.
        Pageable pageable = PageRequest.of(page, 8, Sort.by(Sort.Order.desc("sentDate"), Sort.Order.desc("dmid")));

        Specification<DisasterMessage> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (!"전국".equals(area)) {
            spec = spec.and(DisasterMessageSpecs.likeArea(area));
        }
        if (!"전체".equals(disasterType)) {
            spec = spec.and(DisasterMessageSpecs.equalDisasterType(disasterType));
        }

        Page<DisasterMessage> paging = disasterMessageRepository.findAll(spec, pageable);

        int startPage = Math.max(0, paging.getNumber() - 2);
        int endPage = Math.min(paging.getTotalPages() - 1, paging.getNumber() + 2);

        model.addAttribute("paging", paging);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("areaPrefixes", disasterMessageRepository.findDistinctAreaPrefixes());
        model.addAttribute("disasterTypes", disasterMessageRepository.findDistinctDisasterTypes());
        model.addAttribute("selectedArea", area);
        model.addAttribute("selectedType", disasterType);

        return "disaster/disasterMessage";
    }

    // 지도 사이드바 실시간 피드용 — 최신 재난문자 20건(JSON)
    @ResponseBody
    @GetMapping("/api/disaster-messages/recent")
    public List<DisasterMessageDto> recentMessages() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("sentDate"), Sort.Order.desc("dmid")));
        return disasterMessageRepository.findAll(pageable).stream()
                .map(m -> new DisasterMessageDto(m.getDisasterType(), m.getEmergencyLevel(),
                        m.getArea(), m.getSentDate(), m.getContent()))
                .toList();
    }
}
