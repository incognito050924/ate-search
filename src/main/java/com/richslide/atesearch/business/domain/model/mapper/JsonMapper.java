package com.richslide.atesearch.business.domain.model.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class JsonMapper {
    private static final String ARRAY_START_CHAR = "[";
    private static final String ARRAY_END_CHAR = "]";
    private static final String ARRAY_ELEMENT_DELIMITER_CHAR = ",";

    private static ObjectMapper obtainMapper() {
        return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    };

    public static <T> Optional<String> bean2Json(final T t) {
        try {
            return Optional.of(obtainMapper().writeValueAsString(t));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static <T> Optional<String> bean2JsonPretty(final T t) {
        try {
            return Optional.of(obtainMapper().writerWithDefaultPrettyPrinter().writeValueAsString(t));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public synchronized static <T> void writeAndAppendJsonToFile(final File file, final T t, final Function<T, Optional<String>> mapper, final boolean isLast) {
        if (isLast) {
            try {
                if (file.exists())
                    Files.write(file.toPath(), Arrays.asList(ARRAY_END_CHAR), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        final Optional<String> jsonString = mapper.apply(t);
        if (jsonString.isPresent()) {
            final String[] input;
            final StandardOpenOption openOption;
            if (file.exists()) {
                openOption = StandardOpenOption.APPEND;
                input = new String[] { ARRAY_ELEMENT_DELIMITER_CHAR, jsonString.get() };
            } else {
                openOption = StandardOpenOption.CREATE;
                input = new String[] { ARRAY_START_CHAR, jsonString.get() };
            }
            try {
                Files.write(file.toPath(), Arrays.asList(input), StandardCharsets.UTF_8, openOption);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static <T> void writeJsonToFile(final File file, final T t) {
        try (SequenceWriter writer = obtainMapper().writer().withDefaultPrettyPrinter().writeValuesAsArray(new FileWriter(file, true))) {
            writer.write(t);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> void writeJsonListToFile(final File file, final List<T> tList) {
        try {
            obtainMapper().writerWithDefaultPrettyPrinter().writeValue(file, tList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> List<T> readJsonListFile(final File file, final Class<T[]> clazz) {
        try {
            return Arrays.asList(obtainMapper().readValue(file, clazz));
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
