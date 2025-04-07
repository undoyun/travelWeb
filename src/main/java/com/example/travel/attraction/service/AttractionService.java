package com.example.travel.attraction.service;

import com.example.travel.attraction.model.Attraction;
import com.example.travel.attraction.model.AttractionApiData;
import com.example.travel.attraction.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionService {

    private final AttractionRepository attractionRepository;

    @Value("${attraction.api.key:Q2cZk3vVfQ%2FhOPuuXXTqATN7Im5pUArW9nZAyxJZ5bLxTUhRSbQr9YJBL8ImMTgreNqnaB0uzdWcplSXdAQpHQ%3D%3D}")
    private String apiKey;

    @Value("${attraction.api.url:https://apis.data.go.kr/6260000/AttractionService/getAttractionKr}")
    private String apiUrl;

    @Value("${attraction.api.update.interval:86400000}") // 기본값: 24시간(밀리초)
    private long updateInterval;

    /**
     * 모든 관광 명소 목록을 조회합니다. (캐싱 적용)
     */
    @Cacheable(value = "attractionList")
    public List<Attraction> getAllAttractions() {
        log.debug("캐시에서 조회되지 않아 DB에서 모든 관광 명소를 조회합니다.");
        return attractionRepository.findAll();
    }

    /**
     * 페이징을 적용하여 관광 명소 목록을 조회합니다.
     */
    public Page<Attraction> getAttractionsByPage(Pageable pageable) {
        return attractionRepository.findAll(pageable);
    }

    /**
     * ID로 특정 관광 명소를 조회합니다.
     */
    public Optional<Attraction> getAttractionById(Long id) {
        if (id == null) {
            log.warn("ID가 null인 관광 명소 조회 요청이 있습니다.");
            return Optional.empty();
        }

        log.debug("DB에서 ID {}인 관광 명소를 조회합니다.", id);
        return attractionRepository.findById(id);
    }

    /**
     * 구군 이름으로 관광 명소를 조회합니다. (캐싱 적용)
     */
    @Cacheable(value = "attractionsByGugun", key = "#gugunNm")
    public List<Attraction> getAttractionsByGugun(String gugunNm) {
        log.debug("캐시에서 조회되지 않아 DB에서 구군 {}의 관광 명소를 조회합니다.", gugunNm);
        return attractionRepository.findByGugunNm(gugunNm);
    }

    /**
     * 페이징을 적용하여 구군 이름으로 관광 명소를 조회합니다.
     */
    public Page<Attraction> getAttractionsByGugunWithPaging(String gugunNm, Pageable pageable) {
        return attractionRepository.findByGugunNm(gugunNm, pageable);
    }

    /**
     * 최근 추가된 관광 명소를 조회합니다. (캐싱 적용)
     */
    @Cacheable(value = "recentAttractions")
    public List<Attraction> getRecentAttractions() {
        log.debug("캐시에서 조회되지 않아 DB에서 최근 추가된 관광 명소를 조회합니다.");
        return attractionRepository.findTop10ByOrderByIdDesc();
    }

    /**
     * 키워드로 관광 명소를 검색합니다.
     */
    public Page<Attraction> searchAttractions(String keyword, Pageable pageable) {
        return attractionRepository.findByMainTitleContaining(keyword, pageable);
    }

    /**
     * 구군과 키워드로 관광 명소를 검색합니다.
     */
    public Page<Attraction> searchAttractionsByGugun(String gugunNm, String keyword, Pageable pageable) {
        return attractionRepository.findByGugunNmAndMainTitleContaining(gugunNm, keyword, pageable);
    }

    /**
     * 인기 지역(구군별 관광 명소 수)을 조회합니다.
     */
    @Cacheable(value = "popularAttractions")
    public List<Object[]> getPopularRegions() {
        return attractionRepository.countByGugunNmGroupByGugunNm();
    }

    /**
     * API에서 관광 명소 데이터를 가져와 DB에 저장합니다.
     * 페이징을 적용하여 모든 데이터를 가져옵니다.
     */
    @Transactional
    @CacheEvict(value = { "attractionList", "attractionsByGugun", "recentAttractions",
            "popularAttractions" }, allEntries = true)
    public void fetchAndSaveAttractions() {
        // 기본값으로 최대 20페이지까지 가져옵니다.
        fetchAndSaveAttractionsWithMaxPages(20, 100);
    }

    /**
     * 관광 명소 데이터 리스트를 처리하고 처리 결과 카운트를 반환합니다.
     *
     * @return int[] {처리된 데이터 개수, 신규 추가 개수, 업데이트 개수}
     */
    @Transactional
    private int[] processAttractionDataListWithCounts(List<AttractionApiData> apiDataList) {
        int processedCount = 0;
        int newCount = 0;
        int updatedCount = 0;

        for (AttractionApiData apiData : apiDataList) {
            try {
                String ucSeq = apiData.getUC_SEQ();
                if (ucSeq == null || ucSeq.isEmpty()) {
                    log.warn("UC_SEQ가 없는 데이터를 건너뜁니다: {}", apiData.getMAIN_TITLE());
                    continue;
                }

                Optional<Attraction> existingAttraction = attractionRepository.findByUcSeq(ucSeq);

                if (existingAttraction.isPresent()) {
                    // 기존 데이터 업데이트
                    Attraction attraction = existingAttraction.get();
                    attraction.updateFromApiData(apiData);
                    attractionRepository.save(attraction);
                    updatedCount++;
                } else {
                    // 새 데이터 추가
                    Attraction newAttraction = Attraction.fromApiData(apiData);
                    attractionRepository.save(newAttraction);
                    newCount++;
                }

                processedCount++;
            } catch (Exception e) {
                log.error("관광 명소 데이터 처리 중 오류 발생: {}", e.getMessage());
            }
        }

        log.info("데이터 처리 결과: 총 {}개 처리, {}개 신규 추가, {}개 업데이트",
                processedCount, newCount, updatedCount);

        return new int[] { processedCount, newCount, updatedCount };
    }

    /**
     * XML 문자열을 Document 객체로 파싱합니다.
     */
    private Document parseXmlDocument(String xmlData) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlData)));
    }

    /**
     * XML Document를 AttractionApiData 객체 리스트로 파싱합니다.
     */
    private List<AttractionApiData> parseXmlToAttractionApiData(Document document) {
        List<AttractionApiData> result = new ArrayList<>();

        try {
            // XML 구조 로깅
            log.debug("XML 루트 요소: {}", document.getDocumentElement().getNodeName());

            // 오류 확인
            NodeList errorNodes = document.getElementsByTagName("errMsg");
            if (errorNodes.getLength() > 0) {
                String errorMsg = errorNodes.item(0).getTextContent();
                log.error("API 오류 메시지: {}", errorMsg);

                NodeList reasonNodes = document.getElementsByTagName("returnAuthMsg");
                if (reasonNodes.getLength() > 0) {
                    String reasonMsg = reasonNodes.item(0).getTextContent();
                    log.error("API 오류 이유: {}", reasonMsg);
                }

                return result;
            }

            // 다양한 경로로 item 태그 찾기 시도
            NodeList itemList = null;

            // 1. 직접 item 태그 검색
            itemList = document.getElementsByTagName("item");
            log.debug("직접 item 태그 검색 결과: {}", itemList.getLength());

            // 2. body > items > item 경로 시도
            if (itemList == null || itemList.getLength() == 0) {
                NodeList bodyList = document.getElementsByTagName("body");
                if (bodyList.getLength() > 0) {
                    Element bodyElement = (Element) bodyList.item(0);
                    NodeList itemsList = bodyElement.getElementsByTagName("items");
                    if (itemsList.getLength() > 0) {
                        Element itemsElement = (Element) itemsList.item(0);
                        itemList = itemsElement.getElementsByTagName("item");
                        log.debug("body > items > item 경로 검색 결과: {}", itemList.getLength());
                    }
                }
            }

            // 3. response > body > items > item 경로 시도
            if (itemList == null || itemList.getLength() == 0) {
                NodeList responseList = document.getElementsByTagName("response");
                if (responseList.getLength() > 0) {
                    Element responseElement = (Element) responseList.item(0);
                    NodeList bodyList = responseElement.getElementsByTagName("body");
                    if (bodyList.getLength() > 0) {
                        Element bodyElement = (Element) bodyList.item(0);
                        NodeList itemsList = bodyElement.getElementsByTagName("items");
                        if (itemsList.getLength() > 0) {
                            Element itemsElement = (Element) itemsList.item(0);
                            itemList = itemsElement.getElementsByTagName("item");
                            log.debug("response > body > items > item 경로 검색 결과: {}", itemList.getLength());
                        }
                    }
                }
            }

            // 여전히 item 태그가 없으면 로그 출력
            if (itemList == null || itemList.getLength() == 0) {
                log.warn("XML에서 item 태그를 찾을 수 없습니다.");
                return result;
            }

            // item 태그에서 데이터 추출
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);
                AttractionApiData apiData = new AttractionApiData();

                // 각 필드 설정
                apiData.setMAIN_TITLE(getElementTextContent(item, "MAIN_TITLE"));
                apiData.setLNG(getElementTextContent(item, "LNG"));
                apiData.setMIDDLE_SIZE_RM1(getElementTextContent(item, "MIDDLE_SIZE_RM1"));
                apiData.setUC_SEQ(getElementTextContent(item, "UC_SEQ"));
                apiData.setUSAGE_AMOUNT(getElementTextContent(item, "USAGE_AMOUNT"));
                apiData.setCNTCT_TEL(getElementTextContent(item, "CNTCT_TEL"));
                apiData.setMAIN_IMG_NORMAL(getElementTextContent(item, "MAIN_IMG_NORMAL"));
                apiData.setTRFC_INFO(getElementTextContent(item, "TRFC_INFO"));
                apiData.setHLDY_INFO(getElementTextContent(item, "HLDY_INFO"));
                apiData.setITEMCNTNTS(getElementTextContent(item, "ITEMCNTNTS"));
                apiData.setPLACE(getElementTextContent(item, "PLACE"));
                apiData.setSUBTITLE(getElementTextContent(item, "SUBTITLE"));
                apiData.setUSAGE_DAY(getElementTextContent(item, "USAGE_DAY"));
                apiData.setUSAGE_DAY_WEEK_AND_TIME(getElementTextContent(item, "USAGE_DAY_WEEK_AND_TIME"));
                apiData.setGUGUN_NM(getElementTextContent(item, "GUGUN_NM"));
                apiData.setADDR1(getElementTextContent(item, "ADDR1"));
                apiData.setHOMEPAGE_URL(getElementTextContent(item, "HOMEPAGE_URL"));
                apiData.setTITLE(getElementTextContent(item, "TITLE"));
                apiData.setMAIN_IMG_THUMB(getElementTextContent(item, "MAIN_IMG_THUMB"));
                apiData.setLAT(getElementTextContent(item, "LAT"));

                // UC_SEQ가 없으면 다른 필드로 대체 시도
                if (apiData.getUC_SEQ() == null || apiData.getUC_SEQ().isEmpty()) {
                    // MAIN_TITLE이 있으면 그것을 UC_SEQ로 사용
                    if (apiData.getMAIN_TITLE() != null && !apiData.getMAIN_TITLE().isEmpty()) {
                        apiData.setUC_SEQ(apiData.getMAIN_TITLE());
                        log.debug("UC_SEQ가 없어 MAIN_TITLE을 UC_SEQ로 사용: {}", apiData.getMAIN_TITLE());
                    }
                }

                // 필수 필드가 있는 경우에만 추가
                if (apiData.getMAIN_TITLE() != null && !apiData.getMAIN_TITLE().isEmpty() &&
                        apiData.getUC_SEQ() != null && !apiData.getUC_SEQ().isEmpty()) {
                    result.add(apiData);
                    log.debug("관광 명소 데이터 추가: {}", apiData.getMAIN_TITLE());
                } else {
                    log.warn("필수 필드(MAIN_TITLE 또는 UC_SEQ)가 없는 데이터를 건너뜁니다.");
                }
            }

            log.info("총 {}개의 관광 명소 데이터를 파싱했습니다.", result.size());
        } catch (Exception e) {
            log.error("XML 파싱 실패: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * XML 요소에서 텍스트 콘텐츠를 가져옵니다.
     */
    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    /**
     * 애플리케이션 시작 시 API에서 데이터를 가져와 DB에 저장합니다.
     */
    //애플리케이션 시작 시 실행 **********************************************************
    // @PostConstruct
    public void initAttractionData() {
        // 데이터 개수 확인
        long count = attractionRepository.count();
        log.info("현재 DB에 저장된 관광 명소 데이터 개수: {}", count);

        // 모든 페이지의 데이터를 가져옵니다.
        log.info("관광 명소 데이터 초기화 시작 - 모든 페이지의 데이터를 가져옵니다.");
        fetchAndSaveAttractions();
    }

    /**
     * 일정 시간마다 API에서 데이터를 가져와 DB를 업데이트합니다.
     * 기본값: 매일 새벽 3시
     */
    @Scheduled(cron = "${attraction.api.update.cron:0 0 3 * * ?}")
    @CacheEvict(value = { "attractionList", "attractionsByGugun", "recentAttractions",
            "popularAttractions" }, allEntries = true)
    public void scheduledDataUpdate() {
        log.info("예약된 관광 명소 데이터 업데이트 시작");
        fetchAndSaveAttractions();
    }

    /**
     * 모든 캐시를 수동으로 갱신합니다.
     */
    @CacheEvict(value = { "attractionList", "attractionsByGugun", "recentAttractions",
            "popularAttractions" }, allEntries = true)
    public void refreshAllCaches() {
        log.info("모든 관광 명소 캐시를 갱신했습니다.");
    }

    /**
     * DB에 저장된 관광 명소 데이터 개수를 반환합니다.
     */
    public long getAttractionCount() {
        return attractionRepository.count();
    }

    /**
     * 최대 페이지 수를 설정하여 관광 명소 데이터를 가져와 DB에 저장합니다.
     */
    @Transactional
    public String fetchAndSaveAttractionsWithMaxPages(int maxPages, int size) {
        return fetchAndSaveAttractionsInRange(1, maxPages, size);
    }

    /**
     * 사용자가 지정한 페이지 범위의 관광 명소 데이터를 가져와 DB에 저장합니다.
     */
    @Transactional
    public String fetchAndSaveAttractionsInRange(int startPage, int endPage, int size) {
        int totalProcessedCount = 0;
        int totalNewCount = 0;
        int totalUpdatedCount = 0;
        int totalPages = 0;

        try {
            log.info("페이지 범위 {}~{} 관광 명소 데이터 가져오기 시작", startPage, endPage);

            // 페이지 범위 유효성 검사
            if (startPage < 1) {
                startPage = 1;
                log.warn("시작 페이지가 1보다 작아 1로 설정합니다.");
            }

            if (endPage < startPage) {
                endPage = startPage;
                log.warn("종료 페이지가 시작 페이지보다 작아 시작 페이지와 동일하게 설정합니다.");
            }

            // API 키가 이미 인코딩되어 있으므로 다시 인코딩하지 않도록 주의
            String encodedApiKey = apiKey;

            // 각 페이지 처리
            for (int currentPage = startPage; currentPage <= endPage; currentPage++) {
                // URL을 직접 구성하여 인코딩 문제 방지
                String pageUrl = apiUrl + "?serviceKey=" + encodedApiKey + "&pageNo=" + currentPage + "&numOfRows="
                        + size;

                try {
                    log.info("페이지 {} API 호출: {}", currentPage, pageUrl);

                    // 직접 HTTP 요청 수행
                    URL url = new URL(pageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    int responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        log.error("페이지 {} API 호출 실패: {}", currentPage, responseCode);
                        continue;
                    }

                    // 응답 읽기
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();

                    String responseBody = responseBuilder.toString();
                    if (responseBody == null || responseBody.trim().isEmpty()) {
                        log.warn("페이지 {} API 응답이 비어 있습니다.", currentPage);
                        continue;
                    }

                    log.debug("페이지 {} API 응답 받음 (길이: {})", currentPage, responseBody.length());

                    // XML 파싱 시도
                    List<AttractionApiData> pageData = new ArrayList<>();
                    try {
                        Document document = parseXmlDocument(responseBody);
                        pageData = parseXmlToAttractionApiData(document);
                    } catch (Exception e) {
                        log.error("XML 파싱 실패: {}", e.getMessage());
                        continue;
                    }

                    if (pageData.isEmpty()) {
                        log.warn("페이지 {}에서 데이터를 찾을 수 없습니다.", currentPage);
                        continue;
                    }

                    // 데이터 처리 및 결과 집계
                    int[] results = processAttractionDataListWithCounts(pageData);
                    int processedCount = results[0];
                    int newCount = results[1];
                    int updatedCount = results[2];

                    totalProcessedCount += processedCount;
                    totalNewCount += newCount;
                    totalUpdatedCount += updatedCount;
                    totalPages++;

                    log.info("{}페이지 처리 완료: {}개 데이터 (신규: {}, 업데이트: {})",
                            currentPage, processedCount, newCount, updatedCount);

                    // API 호출 간 딜레이 추가 (서버 부하 방지)
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.error("{}페이지 처리 중 오류 발생: {}", currentPage, e.getMessage(), e);
                }
            }

            String resultMessage = String.format(
                    "페이지 범위 %d~%d 관광 명소 데이터 업데이트 완료: 총 %d개 페이지, %d개 데이터 처리 (신규: %d, 업데이트: %d)",
                    startPage, endPage, totalPages, totalProcessedCount, totalNewCount, totalUpdatedCount);

            log.info(resultMessage);
            return resultMessage;
        } catch (Exception e) {
            String errorMessage = "페이지 범위 데이터 가져오기 실패: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 모든 관광 명소의 mainTitle에서 괄호와 그 안의 내용을 제거합니다.
     */
    @Transactional
    public void cleanupAllMainTitles() {
        log.info("모든 관광 명소의 mainTitle 정리 시작");

        List<Attraction> attractions = attractionRepository.findAll();
        int updatedCount = 0;

        for (Attraction attraction : attractions) {
            String originalTitle = attraction.getMainTitle();
            if (originalTitle != null && originalTitle.contains("(")) {
                String cleanedTitle = originalTitle.replaceAll("\\([^)]*\\)", "").trim();
                attraction.setMainTitle(cleanedTitle);
                attractionRepository.save(attraction);
                updatedCount++;

                if (updatedCount % 100 == 0) {
                    log.info("{}개의 관광 명소 mainTitle 정리 완료", updatedCount);
                }
            }
        }

        log.info("총 {}개의 관광 명소 mainTitle 정리 완료", updatedCount);
    }
}