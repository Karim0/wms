package com.example.wms.wms.controllers;

import com.example.wms.wms.base.BaseType;
import com.example.wms.wms.entities.*;
import com.example.wms.wms.repositories.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Api(tags = {"Заказ"}, description = "API для заказа товара со склада")
@RestController
@RequestMapping(value = "/batch")
public class BatchController {
    private final ProductRepository productRepository;
    private final ContainerRepository containerRepository;
    private final BatchRepository batchRepository;
    private final TaskRepository taskRepository;
    private final StillageRepository stillageRepository;

    public BatchController(ProductRepository productRepository,
                           ContainerRepository containerRepository,
                           BatchRepository batchRepository,
                           TaskRepository taskRepository,
                           StillageRepository stillageRepository) {
        this.productRepository = productRepository;
        this.containerRepository = containerRepository;
        this.batchRepository = batchRepository;
        this.taskRepository = taskRepository;
        this.stillageRepository = stillageRepository;
    }

    @ApiOperation("Создать batch")
    @PostMapping("/addBatch")
    public ResponseEntity<?> addBatch(@RequestParam Long id_product,
                                      @RequestParam int count,
                                      @RequestParam String company_name) {
        //Проверка возможнасти создать batch. Если товара недостатачно прекращаем создание batch.
        ProductEntity productEntity = productRepository.getOne(id_product);

        if (productEntity.getCount_on_warehouse() < count) {
            return ResponseEntity.ok("Недостаточно товара на складе");
        }

        //Если уже есть batch уже созданый для данной компании, то загружаем продукты в этот batch.
        //Иначе создаем новый.

        List<ContainerEntity> list = containerRepository.getContainersByProductIdAndNotLifeCycle(id_product, BaseType.LifeCycle.shipping);
        list.sort(new Comparator<ContainerEntity>() {
            @Override
            public int compare(ContainerEntity o1, ContainerEntity o2) {
                return o2.getCount_product() - o1.getCount_product();
            }
        });

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setPriority(BaseType.PriorityOfExecution.midle);
        taskEntity.setCreated(new Date());
        taskEntity.setTask("");

        int count_shipping = productEntity.getCount_on_shipping();
        int count_warehouse = productEntity.getCount_on_warehouse();

        for (ContainerEntity entity : list) {
            if (entity.getCount_product() <= count) {
                BatchEntity batchEntity = new BatchEntity();

                batchEntity.setCompany_name(company_name);
                batchEntity.setCount(entity.getCount_product());
                batchEntity.setContainer_id(entity.getId());
                count -= entity.getCount_product();

                count_shipping += entity.getCount_product();
                count_warehouse -= entity.getCount_product();

                containerRepository.updateLifeCyrcleById(entity.getId(), BaseType.LifeCycle.shipping);

                StillageEntity stillageEntity = stillageRepository.getOne(entity.getStillageId());

                taskEntity.setTask(taskEntity.getTask() + "Взять контейнер с id = " + entity.getId() + " " +
                        "со стеллажа " + stillageEntity.getStillage_index() + ", и ячейки " +
                        stillageEntity.getShelf_index() + "\n");
                batchRepository.save(batchEntity);
            }

        }

        list = containerRepository.getContainersByProductIdAndNotLifeCycle(id_product, BaseType.LifeCycle.shipping);

        list.sort(new Comparator<ContainerEntity>() {
            @Override
            public int compare(ContainerEntity o1, ContainerEntity o2) {
                return o1.getCount_product() - o2.getCount_product();
            }
        });

        if (count != 0) {
            BatchEntity batchEntity = new BatchEntity();
            batchEntity.setCompany_name(company_name);
            batchEntity.setCount(count);
            batchEntity.setContainer_id(list.get(0).getId());
            count_shipping += count;
            count_warehouse -= count;

            containerRepository.updateCountProducts(list.get(0).getId(), list.get(0).getCount_product() - count);

            StillageEntity stillageEntity = stillageRepository.getOne(list.get(0).getStillageId());

            taskEntity.setTask(taskEntity.getTask() + "Взять " + count + " шт. из контейнера с id = " + list.get(0).getId() + " " +
                    "стеллаж " + stillageEntity.getStillage_index() + ", и ячейка " +
                    stillageEntity.getShelf_index() + "\n");
            batchRepository.save(batchEntity);
        }

        productRepository.updateById(id_product, count_shipping, count_warehouse);

        taskEntity.setTask(taskEntity.getTask() + " Упаковать все в batch для компании" + company_name);
        taskRepository.save(taskEntity);
        return ResponseEntity.ok("Партия сформирована и готова к отправке для компании " + company_name);
    }

    @PostMapping("/all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(batchRepository.findAll());
    }
}