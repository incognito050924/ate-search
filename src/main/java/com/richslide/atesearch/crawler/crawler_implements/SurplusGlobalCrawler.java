package com.richslide.atesearch.crawler.crawler_implements;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.domain.model.mapper.JsonMapper;
import com.richslide.atesearch.business.helper.DateTimeUtil;
import com.richslide.atesearch.business.domain.model.mapper.DocumentMapper;
import com.richslide.atesearch.business.helper.CrawlingException;
import com.richslide.atesearch.crawler.EquipmentWebCrawler;
import com.richslide.atesearch.crawler.helper.enumkey.CrawlingSiteKey;
import com.richslide.atesearch.crawler.helper.utilities.JsoupUtil;
import com.richslide.atesearch.crawler.helper.enumkey.EquipmentKey;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import lombok.extern.slf4j.Slf4j;

import static com.richslide.atesearch.crawler.helper.utilities.JsoupUtil.putOn;

@Slf4j
public class SurplusGlobalCrawler extends EquipmentWebCrawler<String, Map<String, Object>> {
    // www.surplusglobal.com/webapi/Help
    private static final CrawlingSiteKey SITE = CrawlingSiteKey.SURPLUSGLOBAL;
    private static final String JSON_FILE_PATH = "surplusglobal.json";
    private static final Consumer<Equipment> DEFAULT_ACTION = System.out::println;
    private static final Consumer<Equipment> SAVE_AS_FILE = equipment -> {
        JsonMapper.writeAndAppendJsonToFile(new File(JSON_FILE_PATH), equipment, JsonMapper::bean2JsonPretty, false);
    };

    private final Function<org.w3c.dom.NodeList, ArrayList<org.w3c.dom.Node>> convertArrayList = nodeList -> {
        ArrayList<org.w3c.dom.Node> list = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i));
        }
        return list;
    };

    public SurplusGlobalCrawler() {
        this(DEFAULT_ACTION, null, null);
    }

    public SurplusGlobalCrawler(final ExecutorService es) {
        this(DEFAULT_ACTION, null, es);
    }

    public SurplusGlobalCrawler(final Long latency) {
        this(DEFAULT_ACTION, latency, null);
    }

    public SurplusGlobalCrawler(final Consumer<Equipment> action) {
        this(action, null, null);
    }

    public SurplusGlobalCrawler(final Consumer<Equipment> action, final ExecutorService es) {
        this(action, null, es);
    }

    public SurplusGlobalCrawler(final long latency, final ExecutorService es) {
        this(DEFAULT_ACTION, latency, es);
    }

    public SurplusGlobalCrawler(final Consumer<Equipment> action, final Long latency, final ExecutorService executorService) {
        super(SITE, action, latency, executorService);
    }

//    @Override
//    public Iterable<Equipment> call() throws Exception {
//        log.info("call(): Crawling started.");
//        final Consumer<Equipment> action = System.out::println;
//
//        final Document mainDoc;
//        final Optional<Document> maybeDoc = JsoupUtil.getDocument(BASE_URL, BASE_URL, LATENCY);
//        if (maybeDoc.isPresent()) {
//            mainDoc = maybeDoc.get();
//        } else {
//            throw new IOException("Document not found. request url: " + BASE_URL);
//        }
//
//        final List<String> catCodeList = mainDoc.select(".left_category ul li a")
//                                            .stream()
//                                            .map(element -> element.attr("href"))
//                                            .map(url -> url.replaceAll("-", "_").replaceAll(" ", "_"))
//                                            .map(url -> {
//                                                // Get sub-category name.
//                                                String[] splited = url.split("/");
//                                                return splited[splited.length - 2].trim() + "/" + splited[splited.length - 1].trim().toUpperCase();
//                                            })
//                                            .collect(Collectors.toList());
//
////        final List<List<Map<String, Object>>> list = catCodeList
////                .stream()
////                .map(catCode -> parseMarketplaceXML(catCode, action))
////                .collect(Collectors.toList());
//        Optional<List<Future<Map<String, Object>>>> crawlResult = catCodeList.stream().parallel()
//                .map(this::parseMarketplaceXML)
//                .map(maps -> maps.stream().map(map -> parse(map, action)).collect(Collectors.toList()))
//                .reduce((list1, list2) -> {
//                    list1.addAll(list2);
//                    return list1;
//                });
//
//        log.info("call(): Crawling completed.");
//        EXECUTOR_SERVICE.shutdown();
//        return RESULT_LIST;
//    }

    @Override
    public Stream<String> getCategoryUrl() throws CrawlingException {
        final Document mainDoc;
        final Optional<Document> maybeDoc = JsoupUtil.getDocument(BASE_URL, BASE_URL, LATENCY);
        if (maybeDoc.isPresent()) {
            mainDoc = maybeDoc.get();
        } else {
            throw new CrawlingException("Document not found. request url: " + BASE_URL);
        }

        return mainDoc.select(".left_category ul li a")
                .stream()
                .map(element -> element.attr("href"))
                .map(url -> url.replaceAll("-", "_").replaceAll(" ", "_"))
                .map(url -> {
                    // Get sub-category name.
                    String[] splited = url.split("/");
                    return splited[splited.length - 2].trim() + "/" + splited[splited.length - 1].trim().toUpperCase();
                });
    }

    @Override
    public Stream<Map<String, Object>> getCategorizedList(final String catCode) {
        final String[] categories = catCode.split("/");
        final String docUrl = "http://www.surplusglobal.com/webapi/api/MarketplaceList/?" +
                "CatCode=" + categories[1] + "&role=&maker=&model=&wafer=&gubun=&q=&status=&" +
                "BuySell=&_search=false&nd=1528279610676&rows=&page=&sidx=&sord=desc";
        final List<Map<String, Object>> marketplaceListMaps = new ArrayList<>();

        try {
            final org.w3c.dom.Document xml;
            final Document xmlPage = Jsoup.connect(docUrl)
                    .header("Content-Type", "application/xml")
                    .header("charset", "utf-8")
                    .get();

            // XML Parsing
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            xml = builder.parse(new InputSource(new StringReader(xmlPage.toString())));

            final List<String> authorInfo = new ArrayList<>();

            final List<org.w3c.dom.Node> marketplaceList = convertArrayList.apply(xml.getElementsByTagName("MarketplaceList"));
            marketplaceList.stream()
                    .map(org.w3c.dom.Node::getChildNodes) // get child nodes
                    .map(convertArrayList)         // NodeList -> ArrayList<Node>
                    .forEach(children -> {
                        Map<String, Object> map = new HashMap<>();
                        children.forEach(node -> {
                            putOn(map, EquipmentKey.BASE_URL.getText(), BASE_URL, String.class);
                            List<String> category = new ArrayList<>(Arrays.asList(categories));
                            map.put(EquipmentKey.CATEGORY_FIELD_NAME, category);
                            if (node.getNodeName().equals("Item_NO")) {
                                putOn(map, EquipmentKey.AD_NUMBER.getText(), node.getTextContent(), Integer.class);
                                putOn(map, EquipmentKey.URL.getText(), BASE_URL + "marketplace/" + map.get(EquipmentKey.AD_NUMBER.getText()) + "/", String.class);
                            }
                            if (node.getNodeName().equals("BuySell"))
                                putOn(map, EquipmentKey.BUY_SELL.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("Recom_YN"))
                                putOn(map, EquipmentKey.RECOM_YN.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("Status"))
                                putOn(map, EquipmentKey.STATUS.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("Writer_Company"))
                                putOn(map, EquipmentKey.AUTHOR_ORG.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("Item_Name"))
                                putOn(map, EquipmentKey.TITLE.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("PostType"))
                                putOn(map, EquipmentKey.POST_TYPE.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("WriterID"))
                                putOn(map, EquipmentKey.AUTHOR.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("UpdID"))
                                putOn(map, EquipmentKey.UPD_ID.getText(), node.getTextContent(), String.class);
                            if (node.getNodeName().equals("WriteDate")) {
                                //Date date = DateTimeUtil.parseDate(node.getTextContent());
                                LocalDate date = DateTimeUtil.parseToLocalDate(node.getTextContent());
                                if (Objects.nonNull(date))
                                    map.put(EquipmentKey.WRITE_DATE.getText(), date);
                            }
                            if (node.getNodeName().equals("UpdDate")) {
                                LocalDate date = DateTimeUtil.parseToLocalDate(node.getTextContent());
                                if (Objects.nonNull(date))
                                    map.put(EquipmentKey.UPD_DATE.getText(), date);
                            }
                        });
                        map.put("referrer", docUrl);
                        marketplaceListMaps.add(map);
//                        try {
//                            marketplaceListMaps.add(parse(map, action).get());
//                        } catch (InterruptedException e) {
//                            log.error("#{}, {}", TOTAL_COUNT.get(), EXECUTOR_SERVICE.toString());
//                            Thread.currentThread().interrupt();
//                        } catch (ExecutionException e) {
//                            e.printStackTrace();
//                            log.error("parse() exit with ExecutionException[{}]", e.getMessage());
//                        }
//                        log.info("Parse xml [{}: {}]", catCode, map.get(SurplusGlobalKey.URL.getText()));
                    });
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.error("parseMarketplaceXML() exit with Exception[{}]", e.getMessage());
            e.printStackTrace();
        }
        log.info("parseMarketplaceXML() MarketplaceList Xml parse done.[{}]", marketplaceListMaps.size());
        return marketplaceListMaps.stream();
    }

//    public List<Map<String, Object>> parseMarketplaceXML(final String catCode) {
//        final String[] categories = catCode.split("/");
//        final String docUrl = "http://www.surplusglobal.com/webapi/api/MarketplaceList/?" +
//                "CatCode=" + categories[1] + "&role=&maker=&model=&wafer=&gubun=&q=&status=&" +
//                "BuySell=&_search=false&nd=1528279610676&rows=&page=&sidx=&sord=desc";
//        final List<Map<String, Object>> marketplaceListMaps = new ArrayList<>();
//
//        try {
//            final org.w3c.dom.Document xml;
//            final Document xmlPage = Jsoup.connect(docUrl)
//                    .header("Content-Type", "application/xml")
//                    .header("charset", "utf-8")
//                    .get();
//
//            // XML Parsing
//            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            xml = builder.parse(new InputSource(new StringReader(xmlPage.toString())));
//
//            final List<org.w3c.dom.Node> marketplaceList = convertArrayList.apply(xml.getElementsByTagName("MarketplaceList"));
//            marketplaceList.stream()
//                    .map(org.w3c.dom.Node::getChildNodes) // get child nodes
//                    .map(convertArrayList)         // NodeList -> ArrayList<Node>
//                    .forEach(children -> {
//                        Map<String, Object> map = new HashMap<>();
//                        children.forEach(node -> {
//                            putOn(map, SurplusGlobalKey.BASE_URL.getText(), BASE_URL, String.class);
//                            map.put(SurplusGlobalKey.CATEGORIES.getText(), categories);
//                            if (node.getNodeName().equals("Item_NO")) {
//                                putOn(map, SurplusGlobalKey.AD_NUMBER.getText(), node.getTextContent(), Integer.class);
//                                putOn(map, SurplusGlobalKey.URL.getText(), BASE_URL + "marketplace/" + map.get(SurplusGlobalKey.AD_NUMBER.getText()) + "/", String.class);
//                            }
//                            if (node.getNodeName().equals("BuySell"))
//                                putOn(map, SurplusGlobalKey.BUY_SELL.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("Recom_YN"))
//                                putOn(map, SurplusGlobalKey.RECOM_YN.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("Status"))
//                                putOn(map, SurplusGlobalKey.STATUS.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("Writer_Company"))
//                                putOn(map, SurplusGlobalKey.WRITER_COMPANY.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("Item_Name"))
//                                putOn(map, SurplusGlobalKey.ITEM_NAME.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("PostType"))
//                                putOn(map, SurplusGlobalKey.POST_TYPE.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("WriterID"))
//                                putOn(map, SurplusGlobalKey.WRITER_ID.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("UpdID"))
//                                putOn(map, SurplusGlobalKey.UPD_ID.getText(), node.getTextContent(), String.class);
//                            if (node.getNodeName().equals("WriteDate")) {
//                                Date date = CommonsUtil.parseDate(node.getTextContent());
//                                if (Objects.nonNull(date))
//                                    map.put(SurplusGlobalKey.WRITE_DATE.getText(), date);
//                            }
//                            if (node.getNodeName().equals("UpdDate")) {
//                                Date date = CommonsUtil.parseDate(node.getTextContent());
//                                if (Objects.nonNull(date))
//                                    map.put(SurplusGlobalKey.UPD_DATE.getText(), date);
//                            }
//                        });
//                        map.put("referrer", docUrl);
//                        marketplaceListMaps.add(map);
////                        try {
////                            marketplaceListMaps.add(parse(map, action).get());
////                        } catch (InterruptedException e) {
////                            log.error("#{}, {}", TOTAL_COUNT.get(), EXECUTOR_SERVICE.toString());
////                            Thread.currentThread().interrupt();
////                        } catch (ExecutionException e) {
////                            e.printStackTrace();
////                            log.error("parse() exit with ExecutionException[{}]", e.getMessage());
////                        }
////                        log.info("Parse xml [{}: {}]", catCode, map.get(SurplusGlobalKey.URL.getText()));
//                    });
//        } catch (IOException | SAXException | ParserConfigurationException e) {
//            log.error("parseMarketplaceXML() exit with Exception[{}]", e.getMessage());
//            e.printStackTrace();
//        }
//        log.info("parseMarketplaceXML() MarketplaceList Xml parse done.[{}]", marketplaceListMaps.size());
//        return marketplaceListMaps;
//    }
    @Override
    public Map<String, Object> parseDocument(final Map<String, Object> map, final Consumer<Equipment> insertFunction) {
        final Document doc;
        final Object referrer = map.remove("referrer");
        final Optional<Document> maybeDoc = JsoupUtil.getDocument(String.valueOf(map.get(EquipmentKey.URL.getText())), Objects.nonNull(referrer) ? String.valueOf(referrer) : BASE_URL, LATENCY);
        try {
            if (!maybeDoc.isPresent()) {
                    throw new CrawlingException(String.valueOf(EquipmentKey.URL.getText()));
            }
            doc = maybeDoc.get();
            final Elements specInfo = doc.select(".spec_area dl").get(1).children();
            final Elements keys = specInfo.tagName("dt");
            final Elements values = specInfo.tagName("dd");
            for (int i = 0; i < keys.size() / 2; i++) {
                final int idx = i * 2;
                final String key = keys.get(idx).text().trim();
                final String value = values.get(idx + 1).text().trim();

                if (key.equals("Wafer Size"))
                    putOn(map, EquipmentKey.WAFER_SIZE.getText(), value, Integer.class);
                if (key.equals("Configuration"))
                    putOn(map, EquipmentKey.ADDITIONAL_INFO.getText(), value, String.class);
                if (key.equals("Serial Number"))
                    putOn(map, EquipmentKey.SERIAL_NO.getText(), value, String.class);
                if (key.equals("Vintage"))
                    putOn(map, EquipmentKey.VINTAGE.getText(), value, Integer.class);
            }
            putOn(map, EquipmentKey.PRICE.getText(), doc.select(".last dd").get(0).text(), String.class);
            List<String> relatedItems = new ArrayList<>(doc.select(".pro_info a").stream().map(a -> a.text().trim()).collect(Collectors.toList()));
            map.put(EquipmentKey.RELATED_ITEMS_FIELD_NAME, relatedItems);

            final Optional<Equipment> maybeEquipment = DocumentMapper.map2Bean(map, Equipment.class);
            maybeEquipment.ifPresent(RESULT_LIST::add);
            maybeEquipment.ifPresent(insertFunction);

        } catch (CrawlingException e) {
            e.printStackTrace();
            log.error("parseDocument() exit with Exception[{}]", e.getMessage());
            FAILURE_URL_LIST.add(String.valueOf(map.get(EquipmentKey.URL.getText())));
        }
        log.info("parseDocument(): completed [#{} works done.]", TOTAL_COUNT.getAndIncrement());
        return map;
    }

//    public static void main(String[] args) {
//        ExecutorService es = Executors.newFixedThreadPool(5);
//        SurplusGlobalCrawler c = new SurplusGlobalCrawler(SurplusGlobalCrawler.SAVE_AS_FILE, es);
//        es.submit(c);
//        //es.shutdown();
//    }
}
