package com.inha.pro.safetynevi.controller.crawling;

import com.inha.pro.safetynevi.dao.crawling.DisasterMessageRepository;
import com.inha.pro.safetynevi.dto.crawling.DisasterMessage;
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

@Controller
public class DisasterMessageController {

    private final DisasterMessageRepository disasterMessageRepository;

    public DisasterMessageController(DisasterMessageRepository disasterMessageRepository) {
        this.disasterMessageRepository = disasterMessageRepository;
    }

    @GetMapping("/disasterMessage")
    // disasterType 파라미터를 다시 추가합니다.
    public String disasterMessages(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "전국") String area,
                                   @RequestParam(defaultValue = "전체") String disasterType) {

        Pageable pageable = PageRequest.of(page, 8, Sort.by("dmid").descending());

        Specification<DisasterMessage> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (!"전국".equals(area)) {
            spec = spec.and(DisasterMessageSpecs.likeArea(area));
        }
        // disasterType 필터 조건을 추가합니다.
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
        // disasterTypes 목록과 현재 선택된 값을 모델에 추가합니다.
        model.addAttribute("disasterTypes", disasterMessageRepository.findDistinctDisasterTypes());
        model.addAttribute("selectedArea", area);
        model.addAttribute("selectedType", disasterType);

        return "disasterMessage";
    }
}
