package com.richslide.atesearch.business.service;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.domain.model.mapper.JsonMapper;
import com.richslide.atesearch.business.repository.EquipmentRepository;
import com.richslide.atesearch.business.domain.model.EquipmentKey;

import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Slf4j
@Service
public class EquipmentServiceImpl extends DefaultService<Equipment> implements EquipmentService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Override
    public Equipment save(final Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    @Override
    public void saveAndNoReturn(final Equipment equipment) {
        equipmentRepository.save(equipment);
    }

    @Override
    public void bulkIndexFromJsonFile(final File file) {
        if (Objects.requireNonNull(file, "File cannot be null").exists()) {
            bulkIndexWithBean(JsonMapper.readJsonListFile(file, Equipment[].class));
        }
    }

    @Override
    public Page<Equipment> getAllEquipments(final Pageable pageable) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withIndices(EquipmentKey.INDEX_NAME)
                .withTypes(EquipmentKey.TYPE_NAME)
                .withPageable(pageable)
                .build();
        System.out.println("Page No: " + pageable.getPageNumber() + ":" + pageable.toString());
        log.info(searchQuery.getQuery().toString());
        return elasticsearchTemplate.queryForPage(searchQuery, Equipment.class);
    }

    @Override
    public List<Equipment> getAllEquipments() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withIndices(EquipmentKey.INDEX_NAME)
                .withTypes(EquipmentKey.TYPE_NAME)
                .build();

        log.info(searchQuery.getQuery().toString());
        return elasticsearchTemplate.queryForList(searchQuery, Equipment.class);
    }

    @Override
    public void bulkIndexWithBean(final List<Equipment> equipmentList) {
        elasticsearchTemplate.bulkIndex(equipmentList.stream()
                .map(this::createIndexRequest)
                .collect(Collectors.toList()));

        elasticsearchTemplate.refresh(EquipmentKey.INDEX_NAME);
    }

    @Override
    public void bulkIndexWithJsonFile(final File file) {
//        final int max_size = 10000;
        if (Objects.requireNonNull(file).exists()) {
//            final List<Equipment> list = JsonMapper.readJsonListFile(file, Equipment[].class);
//            final int count = (int) Math.ceil(1.0 * list.size() / max_size);
//
//            for (int i = 0; i < count; i++) {
//                final int start = idx * max_size;
//                final int length = list.size() - start > max_size ? list.size() - start : max_size;
//                System.out.println(start + "/" + (start+length));
//                //this.bulkIndexWithBean(list.subList(start, start + length));
//            }
            this.bulkIndexWithBean(JsonMapper.readJsonListFile(file, Equipment[].class));
        } else {
            log.error("bulkIndexWithJsonFile() File not exist.");
        }
    }

    private IndexQuery createIndexRequestAsJson(final String source) {
        return new IndexQueryBuilder()
                .withIndexName(EquipmentKey.INDEX_NAME)
                .withType(EquipmentKey.TYPE_NAME)
                .withSource(source)
                .build();
    }

    private IndexQuery createIndexRequest(final Equipment equipment) {
        return new IndexQueryBuilder()
                .withIndexName(EquipmentKey.INDEX_NAME)
                .withType(EquipmentKey.TYPE_NAME)
                .withObject(equipment)
                .build();
    }

    @Override
    public Page<Equipment> search(final String keyword, final Pageable pageable) {
        final SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword)
                        .field(EquipmentKey.TITLE.getText())
                        .field(EquipmentKey.AUTHOR.getText())
                        .field(EquipmentKey.MODEL.getText())
                        .field(EquipmentKey.SERIAL_NO.getText())
                        .field(EquipmentKey.CATEGORY_PRIMARY.getText())
                        .field(EquipmentKey.CATEGORY_SECONDARY.getText())
                        .field(EquipmentKey.RELATED_ITME_1.getText())
                        .field(EquipmentKey.RELATED_ITME_2.getText())
                        .field(EquipmentKey.RELATED_ITME_3.getText())
                        .field(EquipmentKey.RELATED_ITME_4.getText())
                        .field(EquipmentKey.RELATED_ITME_5.getText())
                        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                        .fuzziness(Fuzziness.TWO)
                )
                .withIndices(EquipmentKey.INDEX_NAME)
                .withTypes(EquipmentKey.TYPE_NAME)
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.queryForPage(searchQuery, Equipment.class);
    }

    @Override
    public List<Equipment> search(final String keyword) {
        final SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword)
                        .field(EquipmentKey.TITLE.getText())
                        .field(EquipmentKey.AUTHOR.getText())
                        .field(EquipmentKey.MODEL.getText())
                        .field(EquipmentKey.SERIAL_NO.getText())
                        .field(EquipmentKey.CATEGORY_PRIMARY.getText())
                        .field(EquipmentKey.CATEGORY_SECONDARY.getText())
                        .field(EquipmentKey.RELATED_ITME_1.getText())
                        .field(EquipmentKey.RELATED_ITME_2.getText())
                        .field(EquipmentKey.RELATED_ITME_3.getText())
                        .field(EquipmentKey.RELATED_ITME_4.getText())
                        .field(EquipmentKey.RELATED_ITME_5.getText())
                        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                        .fuzziness(Fuzziness.TWO)
                )
                .withIndices(EquipmentKey.INDEX_NAME)
                .withTypes(EquipmentKey.TYPE_NAME)
                .build();

        return elasticsearchTemplate.queryForList(searchQuery, Equipment.class);
    }

    @Override
    public List<Equipment> search(final String keyword, final String... fields) {
        final SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, fields))
                .withIndices(EquipmentKey.INDEX_NAME)
                .withTypes(EquipmentKey.TYPE_NAME)
                .build();

        return elasticsearchTemplate.queryForList(searchQuery, Equipment.class);
    }
}
