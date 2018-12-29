package com.richslide.atesearch.business.crawler.implemented;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.helper.CrawlingException;
import com.richslide.atesearch.business.domain.model.mapper.DocumentMapper;
import com.richslide.atesearch.business.crawler.EquipmentWebCrawler;
import com.richslide.atesearch.business.crawler.CrawlingSiteKey;
import com.richslide.atesearch.business.domain.model.EquipmentKey;
import com.richslide.atesearch.business.crawler.utilities.JsoupUtil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.richslide.atesearch.business.crawler.utilities.JsoupUtil.putOn;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class EquipmatchingCrawler extends EquipmentWebCrawler<String, String> {
    public static final CrawlingSiteKey SITE = CrawlingSiteKey.EQUIPMATCING;
    public static final String JSON_FILE_PATH = "equipmatching.json";
//    private static final Consumer<Equipment> SAVE_AS_FILE = equipment -> {
//        JsonMapper.writeAndAppendJsonToFile(new File(JSON_FILE_PATH), equipment, JsonMapper::bean2JsonPretty, false);
//    };
    // private Iterable<Map<String, Object>> RESULT_LIST;

    public EquipmatchingCrawler() {
        this(new InitOptionBuilder().withWebSiteUrl(SITE.getText()));
    }
    public EquipmatchingCrawler(final InitOptionBuilder initOptionBuilder) {
        super(initOptionBuilder.build());
    }

    public List<String> getFailures() {return FAILURE_URL_LIST;}

    @Override
    public Stream<Optional<String>> getCategoryList() throws CrawlingException {
        log.info("getCategoryList(): started");
        final Document mainDoc;
        final Optional<Document> maybeDoc = JsoupUtil.getDocument(BASE_URL + "list_all_categories", BASE_URL, LATENCY);
        if (maybeDoc.isPresent()) {
            mainDoc = maybeDoc.get();
        } else {
            FAILURE_URL_LIST.add(BASE_URL + "list_all_categories");
            throw new CrawlingException("Document not found. request url: " + BASE_URL + "list_all_categories");
        }

        // 1-Depth category items.
        final Set<Optional<String>> urlSet = mainDoc.select(".listall a")
                .stream()
                .map(element -> element.attributes().get("href"))
                .filter(href -> href.contains("index.html"))
                .peek(url -> log.info("List page URL: {}", url))
                .map(Optional::of)
                .collect(Collectors.toSet());

        // 2-Depth category items.
        mainDoc.select(".listall a")
                .stream()
                .map(element -> element.attributes().get("href"))
                .filter(href -> !href.contains("index.html"))
                .map(href -> JsoupUtil.getDocument(BASE_URL + href, BASE_URL, LATENCY))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(document -> document.select(".classcat2_sub a").stream()
                                .map(element -> element.attributes().get("href"))
                                .filter(href -> href.contains("index.html"))
                                .peek(url -> log.info("List page URL: {} [Duplicated: {}]", url, urlSet.contains(url)))
                                .map(Optional::of)
                                .collect(Collectors.toSet()))
                .reduce(urlSet, (set1, set2) -> {
                    set1.addAll(set2);
                    return set1;
                });

        log.info("getCategoryList(): completed\t[Result: Total size={}]", urlSet.size());
        return urlSet
                .stream();
    }

    @Override
    public Stream<Optional<String>> getEquipmentList(final String categoryPage) {
        log.info("getEquipmentList(): start");
        final List<Optional<String>> docList = new ArrayList<>();
        Optional<Document> nextDoc = JsoupUtil.getDocument(BASE_URL + categoryPage, BASE_URL, LATENCY);

        while (nextDoc.isPresent()) {
            Document document = nextDoc.get();

            // Parsing Equipment.
            document.select("table a")
                    .stream()
                    .filter(element -> element.hasAttr("href"))
                    .filter(element -> {
                        final String href = element.attr("href");
                        return href.contains("/used_equipment") && href.contains(".php");
                    })
                    .map(element -> element.attr("href"))
                    .map(href -> BASE_URL + href.substring(1))
                    .peek(href -> log.info("Equipment Page [#{}: {}] added.", docList.size(), href))
                    .map(Optional::of)
                    .forEach(docList::add);

            // Request next page.
            Optional<Element> next = document.select("link")
                    .stream()
                    .filter(element -> element.hasAttr("rel") && element.attr("rel").equals("next"))
                    .findFirst();

            if (next.isPresent()) {
                String href = next.get().attr("href");
                log.info("Next page URL: {}", href);
                nextDoc = JsoupUtil.getDocument(href, BASE_URL, LATENCY);
            } else {
                nextDoc = Optional.empty();
            }
        }

        log.info("getEquipmentList(): completed Result: {}", docList.size());
        return docList.stream();
    }

    @Override
    public Optional<Map<String, Object>> parseDocument(final String docUrl, final Consumer<Equipment> action) {
        log.info("parseDocument(): started");
        log.info("parseDocument(): Parse HTML URL= {}", docUrl);
        final Map<String, Object> equipmentMap = new HashMap<>();
        //Document doc = Jsoup.parse(new File("/Users/incognito/test3.html"), "utf-8");

        final Optional<Document> maybeDoc = JsoupUtil.getDocument(docUrl, BASE_URL, LATENCY);
        maybeDoc.ifPresent(doc -> {
            doc = maybeDoc.get();

            putOn(equipmentMap, EquipmentKey.BASE_URL.getText(), BASE_URL, String.class);
            putOn(equipmentMap, EquipmentKey.URL.getText(), docUrl, String.class);

            final String[] titles = doc.select("title").text().split("\\|");
            putOn(equipmentMap, EquipmentKey.TITLE.getText(), titles[0], String.class);
            putOn(equipmentMap, EquipmentKey.AD_NUMBER.getText(), titles[2].trim().split(" Ad ")[1], Integer.class);

            final Elements elements = doc.select(".classcat6 .smallleft");
            putOn(equipmentMap, EquipmentKey.AUTHOR.getText(), elements.get(1).text(), String.class);
            putOn(equipmentMap, EquipmentKey.LOCATION.getText(), elements.get(3).text(), String.class);
            putOn(equipmentMap, EquipmentKey.AD_STATUS.getText(), elements.get(5).text(), String.class);
            putOn(equipmentMap, EquipmentKey.MANUFACTURER.getText(), elements.get(7).text(), String.class);
            putOn(equipmentMap, EquipmentKey.TYPE.getText(), elements.get(9).text(), String.class);
            putOn(equipmentMap, EquipmentKey.MODEL.getText(), elements.get(11).text(), String.class);
            putOn(equipmentMap, EquipmentKey.VINTAGE.getText(), elements.get(13).text(), String.class);
            putOn(equipmentMap, EquipmentKey.CONDITION.getText(), elements.get(15).text(), String.class);
            putOn(equipmentMap, EquipmentKey.PRICE.getText(), elements.get(17).text(), String.class);
            putOn(equipmentMap, EquipmentKey.QUANTITY.getText(), elements.get(19).text(), String.class);
            putOn(equipmentMap, EquipmentKey.SELLER_TYPE.getText(), elements.get(21).text(), String.class);
            putOn(equipmentMap, EquipmentKey.ADDITIONAL_INFO.getText(), elements.get(24).text(), String.class);

            final Elements relatedTemp = doc.select(".RoundedCorner .smallleft a");
            final List<String> related = relatedTemp.stream()
                    .map(element -> element.text().trim())
                    .filter(text -> text.length() > 0)
                    .collect(Collectors.toList());
            equipmentMap.put(EquipmentKey.RELATED_ITEMS_FIELD_NAME, new ArrayList<>(related));

            final Elements cate = doc.select(".maincatnav a");
            final String[] categories = cate.stream()
                    .map(element -> element.text().trim())
                    .filter(text -> text.length() > 0)
                    .filter(text -> !text.toLowerCase().matches("classifieds"))
                    .toArray(String[]::new);
            equipmentMap.put(EquipmentKey.CATEGORY_FIELD_NAME, new ArrayList<>(Arrays.asList(categories)));
        });

    if (!equipmentMap.isEmpty()) {
        // Insert or anything else.
        final Optional<Equipment> maybeEquipment = DocumentMapper.map2Bean(equipmentMap, Equipment.class);
        maybeEquipment.ifPresent(action);
//        maybeEquipment.ifPresent(RESULT_LIST::add);

    } else {
        FAILURE_URL_LIST.add(docUrl);
        log.error("parseDocument(): Parsed result is invalid {}", docUrl);
        log.error("parseDocument(): Failure Data = #{}", FAILURE_URL_LIST.size());
        return Optional.empty();
    }
    //log.info("EquipmatchingCrawler.parseDocument(): resultMap => {}", equipmentMap.toString());
    log.info("parseDocument(): completed [#{} works done.]", TOTAL_COUNT.get());
    return Optional.of(equipmentMap);
}

//    public static void main(String[] args) {
//        ExecutorService es = Executors.newFixedThreadPool(5);
//        final InitOptionBuilder builder = new InitOptionBuilder()
//                .withLatencyAsSeconds(2)
//                .withWebSiteUrl(SITE.getText())
//                .withSaveFilePath(JSON_FILE_PATH);
//
//        EquipmatchingCrawler c = new EquipmatchingCrawler(builder);
//        es.submit(c);
//    }
}
