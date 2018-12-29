package com.richslide.atesearch.business.scheduler;

import com.richslide.atesearch.business.crawler.EquipmentWebCrawler;
import com.richslide.atesearch.business.crawler.implemented.EquipmatchingCrawler;
import com.richslide.atesearch.business.crawler.implemented.SurplusGlobalCrawler;
import com.richslide.atesearch.business.domain.model.Equipment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TempRunner {
    public static void main(String[] args) {
        final File failures = new File("D:\\workspace\\java8\\ate-search\\failures.txt");
        System.err.println(failures.exists());
        if(!failures.exists())
            return;

        final EquipmentWebCrawler.InitOptionBuilder emBuilder = new EquipmentWebCrawler.InitOptionBuilder()
            .withLatencyAsSeconds(3)
            .withSaveFilePath(EquipmatchingCrawler.JSON_FILE_PATH)
            .withWebSiteUrl(EquipmatchingCrawler.SITE.getText());

        final EquipmatchingCrawler emCrawler = new EquipmatchingCrawler(emBuilder);

        final EquipmentWebCrawler.InitOptionBuilder sgBuilder = new EquipmentWebCrawler.InitOptionBuilder()
                .withLatencyAsSeconds(3)
                .withSaveFilePath(SurplusGlobalCrawler.JSON_FILE_PATH)
                .withWebSiteUrl(SurplusGlobalCrawler.SITE.getText());

        final SurplusGlobalCrawler sgCrawler = new SurplusGlobalCrawler(sgBuilder);

        final ExecutorService service = Executors.newFixedThreadPool(5);
        Future<Iterable<Equipment>> emResult = service.submit(emCrawler);
        //Future<Iterable<Equipment>> sgResult = service.submit(sgCrawler);

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (emResult.isDone()) { writeFailures(emCrawler.getFailures()); break;}
//            if (sgResult.isDone()) writeFailures(sgCrawler.getFailures());
//            if (emResult.isDone() && sgResult.isDone()) break;
        }

        service.shutdown();
    }

    private static void writeFailures(final List<String> failList) {
        final File failures = new File("D:\\workspace\\java8\\ate-search\\failures.txt");
        try {
            if (failures.exists()) {
                Files.write(failures.toPath(), failList, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } else {
                Files.write(failures.toPath(), failList, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
