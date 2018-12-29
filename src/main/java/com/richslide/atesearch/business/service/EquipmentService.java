package com.richslide.atesearch.business.service;

import com.richslide.atesearch.business.domain.model.Equipment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.util.List;

public interface EquipmentService {
    Equipment save(Equipment equipment);

    void saveAndNoReturn(Equipment equipment);

    void bulkIndexFromJsonFile(File file);

    Page<Equipment> getAllEquipments(Pageable pageable);

    List<Equipment> getAllEquipments();

    void bulkIndexWithBean(List<Equipment> equipmentList);

    void bulkIndexWithJsonFile(File file);

    Page<Equipment> search(String keyword, Pageable pageable);

    List<Equipment> search(String keyword);

    List<Equipment> search(String keyword, String... fields);
}
