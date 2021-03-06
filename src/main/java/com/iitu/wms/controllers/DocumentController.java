package com.iitu.wms.controllers;

import com.iitu.wms.entities.OrderEntity;
import com.iitu.wms.entities.ProductEntity;
import com.iitu.wms.helpers.ExcelHelper;
import com.iitu.wms.repositories.OrderRepository;
import com.iitu.wms.repositories.ContainerRepository;
import com.iitu.wms.repositories.ProductRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.util.IOUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

@RestController
@Api(tags = "Документы", description = "Контроль документов")
public class DocumentController {

    private final ContainerRepository containerRepository;
    private final OrderRepository batchRepository;
    private final ProductRepository productRepository;

    public DocumentController(ContainerRepository containerRepository,
                              OrderRepository batchRepository,
                              ProductRepository productRepository) {
        this.containerRepository = containerRepository;
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
    }

    @ApiOperation("Документация к поступающему товару")
    @PostMapping("/docExpectedProduct")
    public ResponseEntity<?> docExpectedProduct(@RequestParam MultipartFile file) throws IOException {
        StringBuilder info = new StringBuilder();
        for (ProductEntity product : ExcelHelper.readFromExcelFile(file)) {
            info.append(product.toString()).append("\n\n");
        }
        return ResponseEntity.ok(info.toString());
    }

    @ApiOperation("Сгенерировать документацию к отгружаемому товару")
    @PostMapping("/docShippingProduct")
    public HttpEntity<byte[]> docShippingProduct(@RequestParam Long order_number) throws IOException {
        Map<Integer, Object[]> list = new TreeMap<>();

        list.put(0, new Object[]{"Id", "Name", "Id_container", "Count", "Price"});
        for (OrderEntity order : batchRepository.getBatchByOrderNumber(order_number)) {
            list.put(order.getId().intValue(), new Object[]{order.getId().toString(), order.getProduct().getProduct_name(),
                    order.getAmount(), String.valueOf(order.getProduct().getPrice() * order.getAmount())});
        }

        File file = ExcelHelper.createExelFile(list);

        FileInputStream input = new FileInputStream(file);
        MultipartFile multipartFile = new MockMultipartFile("file",
                file.getName(), "text/plain", IOUtils.toByteArray(input));

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.MULTIPART_FORM_DATA);
        header.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + multipartFile.getOriginalFilename());
        header.setContentLength(multipartFile.getSize());

        return new HttpEntity<byte[]>(multipartFile.getBytes(),
                header);

//        return new FileSystemResource();
    }
}
