package com.example.travel.attraction.controller;

import com.example.travel.attraction.model.Attraction;
import com.example.travel.attraction.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/attractions")
public class AttractionController {

    private final AttractionService attractionService;

    /**
     * 관광 명소 목록 페이지를 표시합니다. (페이징 적용)
     */
    @GetMapping
    public String listAttractions(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Attraction> attractionsPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            attractionsPage = attractionService.searchAttractions(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else {
            attractionsPage = attractionService.getAttractionsByPage(pageable);
        }

        model.addAttribute("attractionsPage", attractionsPage);
        return "attractions/attractionList";
    }

    /**
     * 특정 관광 명소의 상세 정보 페이지를 표시합니다.
     */
    @GetMapping("/{id}")
    public String viewAttraction(@PathVariable(value = "id") Long id, Model model) {
        if (id == null) {
            log.warn("ID가 null인 관광 명소 조회 요청이 있습니다.");
            return "redirect:/attractions";
        }

        Optional<Attraction> attractionOpt = attractionService.getAttractionById(id);
        if (attractionOpt.isPresent()) {
            model.addAttribute("attraction", attractionOpt.get());
            return "attractions/attractionDetail";
        } else {
            log.warn("ID {}인 관광 명소를 찾을 수 없습니다.", id);
            return "redirect:/attractions";
        }
    }

    /**
     * 구군별 관광 명소 목록 페이지를 표시합니다. (페이징 적용)
     */
    @GetMapping("/gugun/{gugunNm}")
    public String listAttractionsByGugun(
            @PathVariable(value = "gugunNm") String gugunNm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Attraction> attractionsPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            attractionsPage = attractionService.searchAttractionsByGugun(gugunNm, keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else {
            attractionsPage = attractionService.getAttractionsByGugunWithPaging(gugunNm, pageable);
        }

        model.addAttribute("attractionsPage", attractionsPage);
        model.addAttribute("gugunNm", gugunNm);
        return "attractions/attractionList";
    }

    /**
     * 최근 추가된 관광 명소 목록을 표시합니다.
     */
    @GetMapping("/recent")
    public String listRecentAttractions(Model model) {
        List<Attraction> attractions = attractionService.getRecentAttractions();
        model.addAttribute("attractions", attractions);
        return "attractions/recent";
    }

    /**
     * 인기 지역(구군별 관광 명소 수) 목록을 표시합니다.
     */
    @GetMapping("/popular-regions")
    public String listPopularRegions(Model model) {
        List<Object[]> popularRegions = attractionService.getPopularRegions();
        model.addAttribute("popularRegions", popularRegions);
        return "attractions/popular-regions";
    }

    /**
     * API에서 관광 명소 데이터를 가져와 DB에 저장합니다. (관리자용)
     */
    @GetMapping("/fetch")
    @ResponseBody
    public String fetchAttractions() {
        log.info("관광 명소 데이터 가져오기 요청 받음");

        // 비동기로 데이터 가져오기 시작
        new Thread(() -> {
            try {
                attractionService.fetchAndSaveAttractions();
            } catch (Exception e) {
                log.error("관광 명소 데이터 가져오기 실패", e);
            }
        }).start();

        return "관광 명소 데이터 가져오기가 백그라운드에서 시작되었습니다. 서버 로그를 확인하세요.";
    }

    /**
     * 모든 캐시를 수동으로 갱신합니다. (관리자용)
     */
    @GetMapping("/refresh-cache")
    @ResponseBody
    public String refreshCache() {
        attractionService.refreshAllCaches();
        return "모든 캐시를 성공적으로 갱신했습니다.";
    }

    /**
     * 현재 DB에 저장된 관광 명소 데이터 개수를 확인합니다.
     */
    @GetMapping("/count")
    @ResponseBody
    public String getAttractionCount() {
        long count = attractionService.getAttractionCount();
        return "현재 DB에 저장된 관광 명소 데이터 개수: " + count + "개";
    }

    /**
     * 사용자가 지정한 페이지 범위의 데이터를 가져옵니다.
     */
    @GetMapping("/fetch-range")
    @ResponseBody
    public String fetchAttractionsInRange(
            @RequestParam(value = "startPage", defaultValue = "1") int startPage,
            @RequestParam(value = "endPage", defaultValue = "3") int endPage,
            @RequestParam(value = "size", defaultValue = "100") int size) {

        log.info("페이지 범위 {}~{} 관광 명소 데이터 가져오기 요청 받음", startPage, endPage);

        // 비동기로 데이터 가져오기 시작
        new Thread(() -> {
            try {
                String result = attractionService.fetchAndSaveAttractionsInRange(startPage, endPage, size);
                log.info("페이지 범위 데이터 가져오기 결과: {}", result);
            } catch (Exception e) {
                log.error("페이지 범위 데이터 가져오기 실패", e);
            }
        }).start();

        return String.format("페이지 %d~%d (각 페이지당 %d개)의 관광 명소 데이터 가져오기가 백그라운드에서 시작되었습니다. 서버 로그를 확인하세요.",
                startPage, endPage, size);
    }

    /**
     * 최대 페이지 수를 지정하여 데이터를 가져옵니다.
     */
    @GetMapping("/fetch-max-pages")
    @ResponseBody
    public String fetchAttractionsWithMaxPages(
            @RequestParam(value = "maxPages", defaultValue = "5") int maxPages,
            @RequestParam(value = "size", defaultValue = "100") int size) {

        log.info("최대 {}페이지까지 관광 명소 데이터 가져오기 요청 받음", maxPages);

        // 비동기로 데이터 가져오기 시작
        new Thread(() -> {
            try {
                String result = attractionService.fetchAndSaveAttractionsWithMaxPages(maxPages, size);
                log.info("최대 페이지 데이터 가져오기 결과: {}", result);
            } catch (Exception e) {
                log.error("최대 페이지 데이터 가져오기 실패", e);
            }
        }).start();

        return String.format("최대 %d페이지(각 페이지당 %d개)의 관광 명소 데이터 가져오기가 백그라운드에서 시작되었습니다. 서버 로그를 확인하세요.",
                maxPages, size);
    }
}