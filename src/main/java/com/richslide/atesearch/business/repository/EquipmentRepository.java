package com.richslide.atesearch.business.repository;

import com.richslide.atesearch.business.domain.model.Equipment;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EquipmentRepository extends ElasticsearchRepository<Equipment, String> {
    Equipment findByTitle(final String title);
    Equipment findByUrl(final String url);
}
