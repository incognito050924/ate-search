package com.richslide.atesearch.web.controller;

import com.richslide.atesearch.business.domain.model.Equipment;
import com.richslide.atesearch.business.service.EquipmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class EquipmentController {
    @Autowired
    private EquipmentService equipmentService;

    @RequestMapping("/")
    public ModelAndView root(final ModelAndView mav) {
        return new ModelAndView("redirect:/home", mav.getModel());
    }

    @RequestMapping("/home")
    public ModelAndView home(final ModelAndView mav) {
        mav.setViewName("home");
        return mav;
    }

    @RequestMapping("/search")
    public ModelAndView search(final ModelAndView mav
            , @RequestParam("keywords") final String keywords
            , @PageableDefault(size = 10) Pageable pageable) {
        mav.setViewName("list");
        mav.addObject("keywords", keywords);
        Page<Equipment> equipments;
        final Page<Equipment> totalEquipments = equipmentService.getAllEquipments(pageable);
        final long total = totalEquipments.getTotalElements();
        if (keywords != null && keywords.length() > 0) {
            equipments = equipmentService.search(keywords, pageable);
        } else {
            equipments = totalEquipments;
            //equipments =productService.getProductTemplateIndexAndType();
        }
        System.err.println(pageable);
        System.err.println("page: " + equipments.getTotalPages());
        //mav.addObject("equipments", equipments);
        mav.addObject("page", equipments);
        mav.addObject("searched", equipments.getTotalElements());
        mav.addObject("total", total);
        return mav;
    }


    @GetMapping("/search/{keywords}")
    public ModelAndView matchFields(final ModelAndView mav, @PathVariable("keywords") final String keywords) {
        final List<Equipment> equipmentList = equipmentService.search(keywords);
        mav.setViewName("list");
        mav.addObject("keywords", keywords);
        mav.addObject("products", equipmentList);
        mav.addObject("searched", equipmentList.size());
        mav.addObject("total", equipmentService.getAllEquipments().size());
        return mav;
    }

    @GetMapping("/search/t/{field}/{keywords}")
    public ModelAndView termField(final ModelAndView mav,
                                  @PathVariable("field") final String field, @PathVariable("keywords") final String keywords) {
        final List<Equipment> equipmentList = equipmentService.search(keywords, field);
        mav.setViewName("list");
        mav.addObject("keywords", keywords);
        mav.addObject("products", equipmentList);
        mav.addObject("searched", equipmentList.size());
        mav.addObject("total", equipmentService.getAllEquipments().size());
        return mav;
    }

    @GetMapping("/bulk/{filename}")
    public ModelAndView bulkIndex(final ModelAndView mav, @PathVariable("filename") final String filename) {
        final File file = new File(filename + ".json");
        System.out.println(file.getAbsoluteFile() + " - " + file.exists());
        equipmentService.bulkIndexFromJsonFile(file);
        return new ModelAndView("redirect:/home", mav.getModel());
    }
}
