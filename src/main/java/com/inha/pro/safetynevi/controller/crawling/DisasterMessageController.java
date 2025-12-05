package com.inha.pro.safetynevi.controller.crawling;

import com.inha.pro.safetynevi.dao.crawling.DisasterMessageRepository;
import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
import com.inha.pro.safetynevi.dto.crawling.FeedMessageDto;
import com.inha.pro.safetynevi.entity.member.Member;
import com.inha.pro.safetynevi.service.push.WebPushService;
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

import java.security.Principal;
import java.util.List;

@Controller
public class DisasterMessageController {

    private final DisasterMessageRepository disasterMessageRepository;
    private final MemberRepository memberRepository;

    public DisasterMessageController(DisasterMessageRepository disasterMessageRepository,
                                     MemberRepository memberRepository) {
        this.disasterMessageRepository = disasterMessageRepository;
        this.memberRepository = memberRepository;
    }

    @GetMapping("/disasterMessage")
    public String disasterMessages(Model model, Principal principal,
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

        // 로그인 회원이면 가입 주소의 시/도를 푸시 구독 기본 지역으로 (없으면 전국)
        String myProvince = "전국";
        if (principal != null) {
            Member me = memberRepository.findById(principal.getName()).orElse(null);
            String p = (me != null) ? WebPushService.provinceOf(me.getAreaName()) : null;
            if (p != null) myProvince = p;
        }
        model.addAttribute("myProvince", myProvince);

        return "disaster/disasterMessage";
    }

    // 지도 사이드바 실시간 피드용 — 최신 재난문자 20건(JSON). id(dmid)는 무한 스크롤 커서용.
    @ResponseBody
    @GetMapping("/api/disaster-messages/recent")
    public List<FeedMessageDto> recentMessages() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("sentDate"), Sort.Order.desc("dmid")));
        return disasterMessageRepository.findAll(pageable).stream()
                .map(this::toFeedDto)
                .toList();
    }

    // 사이드바 피드 무한 스크롤 — 커서(beforeDate·beforeId)보다 과거 문자 size건(기본 20, 최대 50)
    @ResponseBody
    @GetMapping("/api/disaster-messages/older")
    public List<FeedMessageDto> olderMessages(@RequestParam String beforeDate,
                                              @RequestParam Long beforeId,
                                              @RequestParam(defaultValue = "20") int size) {
        int capped = Math.min(Math.max(size, 1), 50);
        return disasterMessageRepository.findOlderThan(beforeDate, beforeId, PageRequest.of(0, capped)).stream()
                .map(this::toFeedDto)
                .toList();
    }

    private FeedMessageDto toFeedDto(DisasterMessage m) {
        return new FeedMessageDto(m.getDmid(), m.getDisasterType(), m.getArea(), m.getSentDate(), m.getContent());
    }
}
