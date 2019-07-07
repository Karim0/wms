package com.example.wms.wms.controllers;

import com.example.wms.wms.entities.ProductEntity;
import com.example.wms.wms.entities.StillageEntity;
import com.example.wms.wms.repositories.ProductRepository;
import com.example.wms.wms.repositories.StillageRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api(tags = "Статистика", description = "Api для выдачи статистики на складе")
@RequestMapping("/statistics")
public class StatisticsController {
    private final StillageRepository stillageRepository;
    private final ProductRepository productRepository;

    public StatisticsController(StillageRepository stillageRepository,
                                ProductRepository productRepository) {
        this.stillageRepository = stillageRepository;
        this.productRepository = productRepository;
    }

    @ApiOperation("Вывод информации по стеллажам")
    @PostMapping("/stillageInfo")
    public ResponseEntity<?> getStillageInfo() {
        List<StillageEntity> stillageEntities = stillageRepository.findAll();
        int looseStillage = 0,
                withContainer = 0;

        for (StillageEntity stillage : stillageEntities) {
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