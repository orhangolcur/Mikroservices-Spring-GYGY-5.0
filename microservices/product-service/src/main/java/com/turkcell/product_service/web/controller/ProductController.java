package com.turkcell.product_service.web.controller;

import com.turkcell.product_service.entity.TestClass;
import com.turkcell.product_service.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/test")
    public TestClass test2() {
        return new TestClass("Hello from Product-Service");
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello";
    }

    @GetMapping
    public String sendTestEvent(@RequestParam String message) {
        productService.sendTestEvent(message);
        return "Başarılı";
    }
}
