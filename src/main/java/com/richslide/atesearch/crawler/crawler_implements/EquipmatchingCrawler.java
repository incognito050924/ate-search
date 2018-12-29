package com.richslide.atesearch.crawler.crawler_implements;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.domain.model.mapper.JsonMapper;
import com.richslide.atesearch.business.helper.CrawlingException;
import com.richslide.atesearch.business.domain.model.mapper.DocumentMapper;
import com.richslide.atesearch.crawler.EquipmentWebCrawler;
import com.richslide.atesearch.crawler.helper.enumkey.CrawlingSiteKey;
import com.richslide.atesearch.crawler.helper.enumkey.EquipmentKey;
import com.richslide.atesearch.crawler.helper.utilities.JsoupUtil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
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

import static com.richslide.atesearch.crawler.helper.utilities.JsoupUtil.putOn;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class EquipmatchingCrawler extends EquipmentWebCrawler<Document, String> {
    private static final CrawlingSiteKey SITE = CrawlingSiteKey.EQUIPMATCING;
    private static final String JSON_FILE_PATH = "equipmatching.json";
    private static final Consumer<Equipment> DEFAULT_ACTION = System.out::println;
    private static final Consumer<Equipment> SAVE_AS_FILE = equipment -> {
        JsonMapper.writeAndAppendJsonToFile(new File(JSON_FILE_PATH), equipment, JsonMapper::bean2JsonPretty, false);
    };
    // private Iterable<Map<String, Object>> RESULT_LIST;

    public EquipmatchingCrawler() {
        this(DEFAULT_ACTION, null, null);
    }

    public EquipmatchingCrawler(final ExecutorService es) {
        this(DEFAULT_ACTION, null, es);
    }

    public EquipmatchingCrawler(final Long latency) {
        this(DEFAULT_ACTION, latency, null);
    }

    public EquipmatchingCrawler(final Consumer<Equipment> action) {
        this(action, null, null);
    }

    public EquipmatchingCrawler(final Consumer<Equipment> action, final ExecutorService executorService) {
        this(action, null, executorService);
    }

    public EquipmatchingCrawler(final long latency, final ExecutorService es) {
        this(DEFAULT_ACTION, latency, es);
    }

    public EquipmatchingCrawler(final Consumer<Equipment> action, final Long latency, final ExecutorService executorService) {
        super(SITE, action, latency, executorService);
    }

    @Override
    public Stream<Document> getCategoryUrl() throws CrawlingException {
        log.info("getCategoryUrl(): started");
        final Document mainDoc;
        Optional<Document> maybeDoc = JsoupUtil.getDocument(BASE_URL + "list_all_categories", BASE_URL, LATENCY);
        if (maybeDoc.isPresent()) {
            mainDoc = maybeDoc.get();
        } else {
            throw new CrawlingException("Document not found. request url: " + BASE_URL + "list_all_categories");
        }

        // 1-Depth category items.
        final Set<String> urlSet = mainDoc.select(".listall a")
                .stream()
                .map(element -> element.attributes().get("href"))
                .filter(href -> href.contains("index.html"))
                .peek(url -> log.info("List page URL: {}", url))
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
                                .collect(Collectors.toSet()))
                .reduce(urlSet, (set1, set2) -> {
                    set1.addAll(set2);
                    return set1;
                });

        log.info("getCategoryUrl(): completed\t[Result: Total size={}, Success count={}]", urlSet.size());
        return urlSet
                .stream()
                .map(href -> {
                    log.info("Request Document: {}", BASE_URL + href);
                    return JsoupUtil.getDocument(BASE_URL + href, BASE_URL, LATENCY);
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Stream<String> getCategorizedList(final Document categoryPage) {
        log.info("getCategorizedList(): start");
        final List<String> docList = new ArrayList<>();
        Optional<Document> nextDoc= Optional.of(categoryPage);

        do {
            Document document = nextDoc.get();

            // Parsing Equipment.
            document.select("#filterWrapper").get(0).siblingElements().tagName("table")
                    .stream()
                    .map(element -> element.select("a")
                            .stream()
                            .filter(a -> a.hasAttr("href") && a.attr("href").contains(".php"))
                            .map(a -> a.attr("href"))
                            .findFirst())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(href -> href.contains("used_equipment"))
                    .map(href -> BASE_URL + href.substring(1))
                    .peek(docList::add)
                    .forEach(href -> log.info("Equipment Page [#{}: {}] added.", docList.size(), href));
                    //.forEach(url -> parse(url, ACTION));

//            document.select("#filterWrapper + table td a")
//                    .stream()
//                    .map(element -> element.attributes().get("href"))
//                    .peek(url -> log.info("Equipment Detail page: {}", url))
//                    .forEach(docList::add);
                    //.forEach(url -> parse(BASE_URL + url, ACTION));

            // Request next page.
            Optional<Element> next = document.select("link")
                    .stream()
                    .filter(element -> element.hasAttr("rel") && element.attr("rel").equals("next"))
                    .findFirst();

            if (next.isPresent()) {
                String href = next.get().attr("href");
                log.info("Next page URL: {}", href);
                nextDoc = JsoupUtil.getDocument(href, BASE_URL, LATENCY);
            } else break;
        } while (nextDoc.isPresent());

        log.info("getCategorizedList(): completed Result: {}", docList.size());
        return docList.stream();
    }

    @Override
    public Map<String, Object> parseDocument(final String docUrl, final Consumer<Equipment> insertFunction) {
        log.info("parseDocument(): started");
        log.info("parseDocument(): Parse HTML URL= {}", docUrl);
        final Map<String, Object> equipmentMap = new HashMap<>();
        try {
            //Document doc = Jsoup.parse(new File("/Users/incognito/test3.html"), "utf-8");
            final Document doc;
            final Optional<Document> maybeDoc = JsoupUtil.getDocument(docUrl, BASE_URL, LATENCY);
            if (!maybeDoc.isPresent())
                throw new IOException("Document not found. request url: " + docUrl);

            doc = maybeDoc.get();

            putOn(equipmentMap, EquipmentKey.BASE_URL.getText(), BASE_URL, String.class);
            putOn(equipmentMap, EquipmentKey.URL.getText(), docUrl, String.class);

            final String[] titles = doc.select("title").text().split("\\|");
            putOn(equipmentMap, EquipmentKey.TITLE.getText(), titles[0], String.class);
            putOn(equipmentMap, EquipmentKey.AD_NUMBER.getText(), titles[2].trim().split(" Ad ")[1], Integer.class);

            final Elements elements = doc.select(".smallleft");
            putOn(equipmentMap, EquipmentKey.AUTHOR.getText(), elements.get(1).text(), String.class);
            putOn(equipmentMap, EquipmentKey.LOCATION.getText(), elements.get(3).text(), String.class);
            putOn(equipmentMap, EquipmentKey.AD_STATUS.getText(), elements.get(5).text(), String.class);
            putOn(equipmentMap, EquipmentKey.MANUFACTURER.getText(), elements.get(7).text(), String.class);
            putOn(equipmentMap, EquipmentKey.TYPE.getText(), elements.get(9).text(), String.class);
            putOn(equipmentMap, EquipmentKey.MODEL.getText(), elements.get(11).text(), String.class);
            putOn(equipmentMap, EquipmentKey.VINTAGE.getText(), elements.get(13).text(), Integer.class);
            putOn(equipmentMap, EquipmentKey.CONDITION.getText(), elements.get(15).text(), String.class);
            putOn(equipmentMap, EquipmentKey.PRICE.getText(), elements.get(17).text(), String.class);
            putOn(equipmentMap, EquipmentKey.QUANTITY.getText(), elements.get(19).text(), String.class);
            putOn(equipmentMap, EquipmentKey.SELLER_TYPE.getText(), elements.get(21).text(), String.class);
            putOn(equipmentMap, EquipmentKey.ADDITIONAL_INFO.getText(), elements.get(24).text(), String.class);

            final Elements relatedTemp = elements.get(26).children().select("a");
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

        } catch (IOException e) {
            log.error("parseDocument() exit with IOException[{}]", e.getMessage());
            FAILURE_URL_LIST.add(docUrl);
        }

        if (!equipmentMap.isEmpty()) {
            // Insert or anything else.
            final Optional<Equipment> maybeEquipment = DocumentMapper.map2Bean(equipmentMap, Equipment.class);
            maybeEquipment.ifPresent(RESULT_LIST::add);
            maybeEquipment.ifPresent(insertFunction);
        }
        //log.info("EquipmatchingCrawler.parseDocument(): resultMap => {}", equipmentMap.toString());
        log.info("parseDocument(): completed [#{} works done.]", TOTAL_COUNT.getAndIncrement());
        return equipmentMap;
    }

//    public static void main(String[] args) {
//        ExecutorService es = Executors.newFixedThreadPool(5);
//        EquipmatchingCrawler crawler = new EquipmatchingCrawler(es);
//        es.submit(crawler);
//    }

//    @Override
//    public Iterable<Equipment> call() throws Exception {
//        log.info("call(): Crawling started.");
//        final Consumer<Equipment> action = System.out::println;
//
//        final Consumer<Document> recursivelyCrawl = document -> {
//            Optional<Document> nextDoc;
//            do {
//                // Parsing Equipment.
//                document.select("#filterWrapper + table td a")
//                        .stream()
//                        .map(element -> element.attributes().get("href"))
//                        .forEach(url -> parse(BASE_URL + url, action));
//
//                // Request next page.
//                Optional<Element> next = document.select("link")
//                        .stream()
//                        .filter(element -> element.hasAttr("rel"))
//                        .filter(element -> element.attr("rel").equals("next"))
//                        .findFirst();
//
//                if (next.isPresent())
//                    nextDoc = JsoupUtil.getDocument(next.get().attr("href"), BASE_URL, LATENCY);
//                else break;
//            } while (nextDoc.isPresent());
//        };
//
//        try {
//            final Document mainDoc;
//            Optional<Document> maybeDoc = JsoupUtil.getDocument(BASE_URL + "list_all_categories", BASE_URL, LATENCY);
//            if (maybeDoc.isPresent()) {
//                mainDoc = maybeDoc.get();
//            } else {
//                throw new IOException("Document not found. request url: " + BASE_URL + "list_all_categories");
//            }
//
//            // 1-Depth category items.
//            mainDoc.select(".listall a").stream()
//                    .map(element -> element.attributes().get("href"))
//                    .filter(href -> href.contains("index.html"))
//                    .map(href -> JsoupUtil.getDocument(BASE_URL + href, BASE_URL, LATENCY))
//                    .filter(Optional::isPresent)
//                    .map(Optional::get)
//                    .forEach(recursivelyCrawl);
//
//            // 2-Depth category items.
//            mainDoc.select(".listall a").stream()
//                    .map(element -> element.attributes().get("href"))
//                    .filter(href -> !href.contains("index.html"))
//                    .map(href -> JsoupUtil.getDocument(BASE_URL + href, BASE_URL, LATENCY))
//                    .filter(Optional::isPresent)
//                    .map(Optional::get)
//                    .forEach(document -> document.select(".classcat2_sub a").stream()
//                            .map(element -> element.attributes().get("href"))
//                            .filter(href -> href.contains("index.html"))
//                            .map(href -> JsoupUtil.getDocument(BASE_URL + href, BASE_URL, LATENCY))
//                            .filter(Optional::isPresent)
//                            .map(Optional::get)
//                            .forEach(recursivelyCrawl)
//                    );
//
//
//        } catch (IOException e) {
//            log.error("call() exit with IOException [{}]", e.getMessage());
//        }
//        EXECUTOR_SERVICE.shutdown();
//        log.info("call(): Crawling completed.");
//        return RESULT_LIST;
//    }

//    public static void main(String[] args) {
//        ExecutorService es = Executors.newFixedThreadPool(5);
//        EquipmatchingCrawler c = new EquipmatchingCrawler(EquipmatchingCrawler.SAVE_AS_FILE, es);
//        es.submit(c);
//    }
}
