package com.richslide.atesearch.business.crawler;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.domain.model.EquipmentKey;
import com.richslide.atesearch.business.domain.model.mapper.JsonMapper;
import com.richslide.atesearch.business.helper.CrawlingException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class EquipmentWebCrawler<CATEGORY_INFO, DOC_INFO> implements WebCrawler<Equipment> {

    protected final String BASE_URL;
    protected final long LATENCY;
    protected final AtomicInteger TOTAL_COUNT;
    protected final ExecutorService EXECUTOR_SERVICE;
    protected final List<String> FAILURE_URL_LIST;
    protected final List<Equipment> RESULT_LIST;
    protected final Consumer<Equipment> ACTION;
    protected final Optional<File> JSON_FILE;
    protected final BiConsumer<File, Equipment> SAVE_AS_FILE = (file, equipment) -> {
        if (Objects.isNull(equipment)) {
            // Append "]"
            JsonMapper.writeAndAppendJsonToFile(file, equipment, JsonMapper::bean2JsonPretty, true);
        } else {
            JsonMapper.writeAndAppendJsonToFile(file, equipment, JsonMapper::bean2JsonPretty, false);
        }
    };

    public EquipmentWebCrawler(final InitOption option) {
        RESULT_LIST = new ArrayList<>();
        FAILURE_URL_LIST = new ArrayList<>();
        TOTAL_COUNT = new AtomicInteger();
        BASE_URL = Objects.requireNonNull(option.getBaseUrl(), "WebSite URL Missing");
        final String jsonFile = option.getSaveFile();
        JSON_FILE = Objects.nonNull(jsonFile) ? Optional.of(new File(jsonFile)) : Optional.empty();
        ACTION = equipment -> {
            RESULT_LIST.add(equipment);
            TOTAL_COUNT.getAndIncrement();
            JSON_FILE.ifPresent(file -> SAVE_AS_FILE.accept(file, equipment));
            if (Objects.nonNull(option.action))
                option.action.accept(equipment);
        };
        LATENCY = option.getLatency();
        EXECUTOR_SERVICE = option.getExecutorService();
        log.info("EquipmentWebCrawler\n\tWeb site: {}\n\tLatency(Interval): {} sec(s).", BASE_URL, option.getTimeUnit().toSeconds(LATENCY));
    }

//    private EquipmentWebCrawler(final String site, final Consumer<Equipment> action, final Long latency, final ExecutorService executorService, final String jsonFile) {
//        BASE_URL = site;
//        ACTION = action;
//        LATENCY = latency;
//        EXECUTOR_SERVICE = executorService;
//        RESULT_LIST = new ArrayList<>();
//        FAILURE_URL_LIST = new ArrayList<>();
//        TOTAL_COUNT = new AtomicInteger();
//        JSON_FILE = Objects.nonNull(jsonFile) ? Optional.of(new File(jsonFile)) : Optional.empty();
//        log.info("EquipmentWebCrawler\n\tWeb site: {}\n\tLatency(Interval): {} sec(s).", BASE_URL, LATENCY);
//    }

    /**
     * EquipmatchingCrawler {@link Callable#call()} 크롤링을 시작한다.
     *
     * @return
     * @throws Exception
     */
    @Override
    public Iterable<Equipment> call() throws Exception {
        log.info("call(): Crawling started [{}]", BASE_URL);

        //final List<Map<String, Object>> crawlResult = new ArrayList<>();
        getCategoryList()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(this::getEquipmentList)
            //.peek(equipmentUrls -> log.info("getEquipmentList() return: [#{}] {}", equipmentUrls.count(), equipmentUrls))
            .forEach(maybeEquipmentUrlStream -> maybeEquipmentUrlStream
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(docInfo -> parse(docInfo, ACTION))
                    .forEach(future -> {
                        try {
                            Optional<Map<String, Object>> maybeEquipment = future.get(); // Blocking Method
                            maybeEquipment.ifPresent(equipment -> {
                                log.info("Succeed in parsing data [URL: {}]", equipment.get(EquipmentKey.URL.getText()));
                                //crawlResult.add(equipment);
                                // JSON_FILE.ifPresent(file -> SAVE_AS_FILE.accept(file, equipment)); // Save to File
                            });
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
//                    .collect(Collectors.toList()))
//            .reduce(crawlResult, (list1, list2) -> {
//                        list1.addAll(list2);
//                        return list1;
//                    });

        EXECUTOR_SERVICE.shutdown();
        JSON_FILE.ifPresent(file -> SAVE_AS_FILE.accept(file, null));
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
    public abstract Stream<Optional<CATEGORY_INFO>> getCategoryList() throws CrawlingException;

    /**
     * 파라미터로 입력 받은 리스트 페이지에서 각 아이템(장비) 상세 페이지에 대한 참조를 담은 {@link Stream}객체를 리턴한다.
     * 리스트 -> 상세 페이지
     *
     * @param category - 각 카테고리 별 아이템(장비) 리스트 페이지에 대한 참조
     * @return - 아이템(장비) 상세 페이지에 대한 참조
     */
    public abstract Stream<Optional<DOC_INFO>> getEquipmentList(final CATEGORY_INFO category);

    /**
     * 파라미터로 입력 받은 아이템(장비) 상세 페이지를 파싱하여 해당 정보를 담은 {@link Map}객체를 리턴한다.
     * 상세 페이지 파싱
     *
     * @param docInfo - 아이템(장비) 상세 페이지에 대한 참조
     * @param action - 파싱 후 해당 데이터에 적용할 리턴값이 없는 함수 <== void action(Equipment e)
     * @return - 상세 페이지를 파싱한 데이터{@link Map}
     */
    public abstract Optional<Map<String, Object>> parseDocument(final DOC_INFO docInfo, final Consumer<Equipment> action);

    /**
     * {@link this#parseDocument(Object, Consumer)}(상세 페이지 파싱 작업)를 지연시간을 적용하고 새로운 쓰레드를 할당 받아 실행한다.
     *
     * @param docInfo - 아이템(장비) 상세 페이지에 대한 참조
     * @param consumer - 파싱 후 해당 데이터에 적용할 리턴값이 없는 함수 <== void action(Equipment e)
     * @return - 상세 페이지를 파싱한 데이터{@link Map}
     */
    public Future<Optional<Map<String, Object>>> parse(final DOC_INFO docInfo, final Consumer<Equipment> consumer) {
        try {
            //log.info("Thread Pool Status: {}", EXECUTOR_SERVICE.toString());
            TimeUnit.SECONDS.sleep(LATENCY);
        }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        //return parse2Map(EXECUTOR_SERVICE, docInfo, consumer, this::parseDocument);
        return parse2Map(EXECUTOR_SERVICE, docInfo, consumer, this::parseDocument);
    }

    public static final class InitOptionBuilder {
        private final InitOption option;

        public InitOptionBuilder() {
            option = new InitOption();
            option.setExecutorService(Executors.newSingleThreadExecutor());
            option.setLatency(2);
            option.setTimeUnit(TimeUnit.SECONDS);
        }

        public InitOptionBuilder withWebSiteUrl(final String url) {
            option.setBaseUrl(url);
            return this;
        }

        public InitOptionBuilder withSaveFilePath(final String path) {
            option.setSaveFile(path);
            return this;
        }

        public InitOptionBuilder withLatencyAsSeconds(final long latency) {
            withLatency(latency, TimeUnit.SECONDS);
            return this;
        }

        public InitOptionBuilder withLatency(final long latency, final TimeUnit timeUnit) {
            option.setLatency(latency);
            option.setTimeUnit(timeUnit);
            return this;
        }

        public InitOptionBuilder withExecutor(final ExecutorService executorService) {
            option.setExecutorService(executorService);
            return this;
        }

        public InitOptionBuilder withEquipmentConsumer(final Consumer<Equipment> action) {
            option.setAction(action);
            return this;
        }

        public InitOption build() {
            return option;
        }
    }

    @Data
    @NoArgsConstructor
    @ToString
    private static final class InitOption {
        private String baseUrl;
        private String saveFile;
        private long latency;
        private TimeUnit timeUnit;
        private ExecutorService executorService;
        private Consumer<Equipment> action;
    }
}
