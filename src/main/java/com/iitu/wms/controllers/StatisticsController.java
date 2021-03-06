package com.iitu.wms.controllers;

import com.iitu.wms.entities.CellEntity;
import com.iitu.wms.entities.ProductEntity;
import com.iitu.wms.entities.User;
import com.iitu.wms.repositories.ProductRepository;
import com.iitu.wms.repositories.CellRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
@Api(tags = "Статистика", description = "Api для выдачи статистики на складе")
@RequestMapping("/statistics")
public class StatisticsController {
    private final CellRepository cellRepository;
    private final ProductRepository productRepository;

    public StatisticsController(CellRepository stillageRepository,
                                ProductRepository productRepository) {
        this.cellRepository = stillageRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("")
    public ModelAndView main(@AuthenticationPrincipal User user) {
        Map<String, Object> root = new TreeMap<>();

        List<CellEntity> stillageEntities = cellRepository.findAll();
        int looseStillage = 0,
                withContainer = 0;

        for (CellEntity stillage : stillageEntities) {
            if (stillage.getCount_object() == 0) {
                looseStillage++;
            } else if (stillage.getCount_object() != stillage.getMax_count_object()) {
                withContainer++;
            }
        }

        root.put("count", stillageEntities.size());
        root.put("looseStillage", looseStillage);
        root.put("withContainer", withContainer);
        root.put("busy", (stillageEntities.size() - looseStillage - withContainer));
        root.put("user", user);

        return new ModelAndView("statistics", root);
    }

    @ApiOperation("Вывод информации по стеллажам")
    @PostMapping("/stillageInfo")
    public ResponseEntity<?> getStillageInfo() {
        List<CellEntity> stillageEntities = cellRepository.findAll();
        int looseStillage = 0,
                withContainer = 0;

        for (CellEntity stillage : stillageEntities) {
            if (stillage.getCount_object() == 0) {
                looseStillage++;
            } else if (stillage.getCount_object() != stillage.getMax_count_object()) {
                withContainer++;
            }
        }

        return ResponseEntity.ok("На складе " + stillageEntities.size() + " стеллажных ячеек\n" +
                looseStillage + " свободных ячеек\n" +
                withContainer + " полусвободных ячеек\n" +
                (stillageEntities.size() - looseStillage - withContainer) + " стеллажей занято");
    }
//
//    @ApiOperation("Вывод информации по стеллажам")
//    @PostMapping("/countStillage")
//    public ResponseEntity<?> getCountStillage(){
//        return ResponseEntity.ok(cellRepository.findAll().size());
//    }
//
//    @ApiOperation("Вывод информации по стеллажам")
//    @PostMapping("/countLooseStillage")
//    public ResponseEntity<?> getCountLooseStillage(){
//        int looseStillage = 0,
//                withContainer = 0;
//
//        for(CellEntity stillage : cellRepository.getLooseStillage()){
//            if (stillage.getCount_object() == 0) {
//                looseStillage++;
//            }
//        }
//
//        return ResponseEntity.ok(looseStillage);
//    }


    @ApiOperation("Вывод информации по товарам")
    @PostMapping("/productInfo")
    public ResponseEntity<?> getProductInfo() {
        StringBuilder info = new StringBuilder();

        List<ProductEntity> productEntities = productRepository.findAll();
        for (ProductEntity product : productEntities) {
            info.append(product.getProduct_name()).append(": \n")
                    .append("\t").append("На складе: ").append(product.getCount_on_warehouse()).append("шт.\n")
                    .append("\t").append("На этапе отправке: ").append(product.getCount_on_shipping()).append("шт.\n")
                    .append("\t").append("Ожидается прибытие: ").append(product.getCount_expected()).append("шт.\n")
                    .append("\t").append("Цена: ").append(product.getPrice()).append("тг.\n")
                    .append("\t").append("Стоимость : ").append(product.getPrice() * product.getCount_on_warehouse()).append("тг.\n");
        }

        return ResponseEntity.ok(info);
    }
}
