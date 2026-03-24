package com.lokesh.ecommerce.product.service;

import com.lokesh.ecommerce.product.dto.ProductRequest;
import com.lokesh.ecommerce.product.dto.ProductResponse;
import com.lokesh.ecommerce.product.dto.ProductSearchRequest;
import com.lokesh.ecommerce.product.entity.Product;
import com.lokesh.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .build();
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for sku: " + sku));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductSearchRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), resolveSort(req));
        Page<Product> page = productRepository.search(
                blankToNull(req.getKeyword()),
                blankToNull(req.getCategory()),
                blankToNull(req.getBrand()),
                req.getMinPrice(),
                req.getMaxPrice(),
                pageable
        );
        return page.map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        return productRepository.findByCategory(category, pageable).map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> bulkGetProducts(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productRepository.findByIdInAndActiveTrue(ids).stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setImageUrl(request.getImageUrl());
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    private static Sort resolveSort(ProductSearchRequest req) {
        String field = req.getSortBy() == null || req.getSortBy().isBlank() ? "name" : req.getSortBy();
        Sort.Direction dir = "DESC".equalsIgnoreCase(req.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
