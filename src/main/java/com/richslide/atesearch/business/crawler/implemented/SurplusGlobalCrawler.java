package com.richslide.atesearch.business.crawler.implemented;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.helper.DateTimeUtil;
import com.richslide.atesearch.business.domain.model.mapper.DocumentMapper;
import com.richslide.atesearch.business.helper.CrawlingException;
import com.richslide.atesearch.business.crawler.EquipmentWebCrawler;
import com.richslide.atesearch.business.crawler.CrawlingSiteKey;
import com.richslide.atesearch.business.crawler.utilities.JsoupUtil;
import com.richslide.atesearch.business.domain.model.EquipmentKey;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import lombok.extern.slf4j.Slf4j;

import static com.richslide.atesearch.business.crawler.utilities.JsoupUtil.putOn;

@Slf4j
public class SurplusGlobalCrawler extends EquipmentWebCrawler<String, Map<String, Object>> {
    // www.surplusglobal.com/webapi/Help
    public static final CrawlingSiteKey SITE = CrawlingSiteKey.SURPLUSGLOBAL;
    public static final String JSON_FILE_PATH = "surplusglobal.json";

    private final Function<org.w3c.dom.NodeList, ArrayList<org.w3c.dom.Node>> convertArrayList = nodeList -> {
        ArrayList<org.w3c.dom.Node> list = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i));
        }
        return list;
    };

    public SurplusGlobalCrawler() {
        this(new InitOptionBuilder().withWebSiteUrl(SITE.getText()));
    }

    public SurplusGlobalCrawler(final InitOptionBuilder initOptionBuilder) {
        super(initOptionBuilder.build());
    }

    public List<String> getFailures() {return FAILURE_URL_LIST;}

    @Override
    public Stream<Optional<String>> getCategoryList() throws CrawlingException {
        log.info("getCategoryList(): started");
        final Document mainDoc;
        final Optional<Document> maybeDoc = JsoupUtil.getDocument(BASE_URL, BASE_URL, LATENCY);
        if (maybeDoc.isPresent()) {
            mainDoc = maybeDoc.get();
        } else {
            throw new CrawlingException("Document not found. request url: " + BASE_URL);
        }

        final Stream<Optional<String>> result = mainDoc.select(".left_category ul li a")
                .stream()
                .map(element -> element.attr("href"))
                .map(url -> url.replaceAll("-", "_").replaceAll(" ", "_"))
                .map(url -> {
                    // Get sub-category name.
                    final String[] splited = url.split("/");
                    return splited[splited.length - 2].trim() + "/" + splited[splited.length - 1].trim().toUpperCase();
                })
                .map(Optional::of);

        log.info("getCategoryList(): completed");
        return result;
    }

    @Override
    public Stream<Optional<Map<String, Object>>> getEquipmentList(final String catCode) {
        log.info("getEquipmentList(): start");
        final String[] categories = catCode.split("/");
        final String docUrl = "http://www.surplusglobal.com/webapi/api/MarketplaceList/?" +
                "CatCode=" + categories[1] + "&role=&maker=&model=&wafer=&gubun=&q=&status=&" +
                "BuySell=&_search=false&nd=1528279610676&rows=&page=&sidx=&sord=desc";
        final List<Optional<Map<String, Object>>> marketplaceListMaps = new ArrayList<>();
        Document xmlPage = null;
        try {
             xmlPage = Jsoup.connect(docUrl)
                    .header("Content-Type", "application/xml")
                    .header("charset", "utf-8")
                    .get();

            // XML Parsing
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final org.w3c.dom.Document xml = builder.parse(new InputSource(new StringReader(xmlPage.toString())));


            final List<org.w3c.dom.Node> marketplaceList = convertArrayList.apply(xml.getElementsByTagName("MarketplaceList"));
            marketplaceList.stream()
                    .map(org.w3c.dom.Node::getChildNodes) // get child nodes
                    .map(convertArrayList)         // NodeList -> ArrayList<Node>
                    .forEach(children -> {
                        Map<String, Object> map = new HashMap<>();
                        children.forEach(node -> {
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
                        if (map.isEmpty()) {
                            marketplaceListMaps.add(Optional.empty());
                        } else {
                            putOn(map, EquipmentKey.BASE_URL.getText(), BASE_URL, String.class);
                            List<String> category = new ArrayList<>(Arrays.asList(categories));
                            map.put(EquipmentKey.CATEGORY_FIELD_NAME, category);
                            map.put("referrer", docUrl);
                            marketplaceListMaps.add(Optional.of(map));
                        }
                    });
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.error("getEquipmentList() exit with Exception[{}]", e.getMessage());
            if (Objects.isNull(xmlPage)) {
                FAILURE_URL_LIST.add(docUrl);
            } else {
                marketplaceListMaps.add(Optional.empty());
            }
            e.printStackTrace();
        }
        log.info("getEquipmentList(): completed Result: {}", marketplaceListMaps.size());
        //log.info("parseMarketplaceXML() MarketplaceList Xml parse done.[{}]", marketplaceListMaps.size());
        return marketplaceListMaps.stream();
    }

    @Override
    public Optional<Map<String, Object>> parseDocument(final Map<String, Object> map, final Consumer<Equipment> action) {
        log.info("parseDocument(): started");
        log.info("parseDocument(): Parse HTML URL= {}", map.get(EquipmentKey.URL.getText()));
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
                    putOn(map, EquipmentKey.VINTAGE.getText(), value, String.class);
            }
            putOn(map, EquipmentKey.PRICE.getText(), doc.select(".last dd").get(0).text(), String.class);
            List<String> relatedItems = new ArrayList<>(doc.select(".pro_info a").stream().map(a -> a.text().trim()).collect(Collectors.toList()));
            map.put(EquipmentKey.RELATED_ITEMS_FIELD_NAME, relatedItems);

            final Optional<Equipment> maybeEquipment = DocumentMapper.map2Bean(map, Equipment.class);
            maybeEquipment.ifPresent(action);
//            maybeEquipment.ifPresent(RESULT_LIST::add);

        } catch (CrawlingException e) {
            e.printStackTrace();
            //log.info("parseDocument() exit with Exception[{}]", e.getMessage());
            FAILURE_URL_LIST.add(String.valueOf(map.get(EquipmentKey.URL.getText())));
            log.error("parseDocument(): Parsed result is invalid {}\n\tException: {}",map.get(EquipmentKey.URL.getText()), e.getMessage());
            log.error("parseDocument(): Failure Data = #{}", FAILURE_URL_LIST.size());
            return Optional.empty();
        }
        log.info("parseDocument(): completed [#{} works done.]", TOTAL_COUNT.get());
        return Optional.of(map);
    }

//    public static void main(String[] args) {
//        ExecutorService es = Executors.newFixedThreadPool(5);
//        final InitOptionBuilder builder = new InitOptionBuilder()
//                .withLatencyAsSeconds(2)
//                .withWebSiteUrl(SITE.getText())
//                .withSaveFilePath(JSON_FILE_PATH)
//                .withExecutor(es);
//
//        SurplusGlobalCrawler c = new SurplusGlobalCrawler(builder);
//        es.submit(c);
//        //es.shutdown();
//    }
}
