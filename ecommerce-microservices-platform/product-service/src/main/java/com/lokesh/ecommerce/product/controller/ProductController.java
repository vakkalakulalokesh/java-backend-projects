package com.lokesh.ecommerce.product.controller;

import com.lokesh.ecommerce.product.dto.ProductRequest;
import com.lokesh.ecommerce.product.dto.ProductResponse;
import com.lokesh.ecommerce.product.dto.ProductSearchRequest;
import com.lokesh.ecommerce.product.service.CategoryService;
import com.lokesh.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id")
    public ProductResponse get(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products")
    public Page<ProductResponse> search(ProductSearchRequest request) {
        return productService.search(request);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ProductResponse bySku(@PathVariable String sku) {
        return productService.getBySku(sku);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    @Operation(summary = "Category tree")
    public List<CategoryService.CategoryTreeNode> categories() {
        return categoryService.getCategoryTree();
    }

    @GetMapping("/bulk")
    @Operation(summary = "Bulk get by ids")
    public List<ProductResponse> bulk(@RequestParam List<UUID> ids) {
        return productService.bulkGetProducts(ids);
    }
}
