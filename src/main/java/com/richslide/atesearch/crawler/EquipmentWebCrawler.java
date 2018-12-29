package com.richslide.atesearch.crawler;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.helper.CrawlingException;
import com.richslide.atesearch.crawler.helper.enumkey.CrawlingSiteKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class EquipmentWebCrawler<CATEGORY_INFO, DOC_INFO> implements WebCrawler<Equipment> {
    private static final long DEFAULT_LATENCY_AS_SECONDS = 2;
    private static final Consumer<Equipment> DEFAULT_ACTION = System.out::println;
    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newFixedThreadPool(5);

    protected final String BASE_URL;
    protected final long LATENCY;
    protected final AtomicInteger TOTAL_COUNT;
    protected final ExecutorService EXECUTOR_SERVICE;
    protected final List<String> FAILURE_URL_LIST;
    protected final List<Equipment> RESULT_LIST;
    protected final Consumer<Equipment> ACTION;


    public EquipmentWebCrawler(final CrawlingSiteKey site, final Consumer<Equipment> action, final Long latency, final ExecutorService executorService) {
        BASE_URL = Objects.requireNonNull(site).getText();
        ACTION = Objects.nonNull(action) ? action : DEFAULT_ACTION;
        LATENCY = Objects.nonNull(latency) ? latency : DEFAULT_LATENCY_AS_SECONDS;
        EXECUTOR_SERVICE = Objects.nonNull(executorService) ? executorService : DEFAULT_EXECUTOR;
        RESULT_LIST = new ArrayList<>();
        FAILURE_URL_LIST = new ArrayList<>();
        TOTAL_COUNT = new AtomicInteger();
        log.info("EquipmentWebCrawler\n\tWeb site: {}\n\tLatency(Interval): {} sec(s).", BASE_URL, LATENCY);
    }

    /**
     * EquipmatchingCrawler {@link Callable#call()} 크롤링을 시작한다.
     *
     * @return
     * @throws Exception
     */
    @Override
    public Iterable<Equipment> call() throws Exception {
        log.info("call(): Crawling started [{}]", BASE_URL);

        final List<Future<Map<String, Object>>> crawlResult = new ArrayList<>();
        getCategoryUrl()
            .map(this::getCategorizedList)
            .map(categorizedList -> categorizedList.map(map -> parse(map, ACTION)).collect(Collectors.toList()))
            .reduce(crawlResult, (list1, list2) -> {
                        list1.addAll(list2);
                        return list1;
                    });

        EXECUTOR_SERVICE.shutdown();
        log.info("call(): Crawling completed.");
        return RESULT_LIST;
    }

    /**
     * 카테고리 별 아이템(장비) 리스트 페이지에 대한 참조를 담은 {@link Stream}객체를 리턴한다.
     * 카테고리 정보 -> 카테고리 별 리스트 페이지
     *
     * @return
     * @throws CrawlingException
     */
    public abstract Stream<CATEGORY_INFO> getCategoryUrl() throws CrawlingException;

    /**
     * 파라미터로 입력 받은 리스트 페이지에서 각 아이템(장비) 상세 페이지에 대한 참조를 담은 {@link Stream}객체를 리턴한다.
     * 리스트 -> 상세 페이지
     *
     * @param category - 각 카테고리 별 아이템(장비) 리스트 페이지에 대한 참조
     * @return - 아이템(장비) 상세 페이지에 대한 참조
     */
    public abstract Stream<DOC_INFO> getCategorizedList(final CATEGORY_INFO category);

    /**
     * 파라미터로 입력 받은 아이템(장비) 상세 페이지를 파싱하여 해당 정보를 담은 {@link Map}객체를 리턴한다.
     * 상세 페이지 파싱
     *
     * @param docInfo - 아이템(장비) 상세 페이지에 대한 참조
     * @param action - 파싱 후 해당 데이터에 적용할 리턴값이 없는 함수 <== void action(Equipment e)
     * @return - 상세 페이지를 파싱한 데이터{@link Map}
     */
    public abstract Map<String, Object> parseDocument(final DOC_INFO docInfo, final Consumer<Equipment> action);

    /**
     * {@link this#parseDocument(Object, Consumer)}(상세 페이지 파싱 작업)를 지연시간을 적용하고 새로운 쓰레드를 할당 받아 실행한다.
     *
     * @param docInfo - 아이템(장비) 상세 페이지에 대한 참조
     * @param consumer - 파싱 후 해당 데이터에 적용할 리턴값이 없는 함수 <== void action(Equipment e)
     * @return - 상세 페이지를 파싱한 데이터{@link Map}
     */
    public Future<Map<String, Object>> parse(final DOC_INFO docInfo, final Consumer<Equipment> consumer) {
        try {
            log.info("Thread Pool Status: {}", EXECUTOR_SERVICE.toString());
            TimeUnit.SECONDS.sleep(LATENCY);
        }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return parse2Map(EXECUTOR_SERVICE, docInfo, consumer, this::parseDocument);
    }
}
