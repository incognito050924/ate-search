package com.richslide.atesearch.business.crawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@FunctionalInterface
public interface WebCrawler<E> extends Callable<Iterable<E>> {

    default <T, E> Future<Optional<Map<String, Object>>> parse2Map
            (final ExecutorService service, final T docInfo, final Consumer<E> action
            , final BiFunction<T, Consumer<E>, Optional<Map<String, Object>>> parser) {

        return service.submit(() -> parser.apply(docInfo, action));
    }

//
//    default <E> Future<Map<String, Object>> parse2Map
//            (final ExecutorService service, final Map<String, Object> map, final Consumer<E> action
//                    , final BiFunction<Map<String, Object>, Consumer<E>, Map<String, Object>> parser) {
//
//        return service.submit(() -> parser.apply(map, action));
//    }

    default <E> Future<E> parse2Bean
            (final ExecutorService service, final String docURL, final Consumer<E> action
                    , final BiFunction<String, Consumer<E>, E> parser) {

        return service.submit(() -> parser.apply(docURL, action));
    }

    default <E> Consumer<E> parseWithPrintItem() {
        return System.out::println;
    }

    default <E> Consumer<E> parseWithInsertESItem() {
        return e -> {

        };
    }

    default <E> Consumer<E> saveResult(final File file) {
        return res -> {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(res);
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}
