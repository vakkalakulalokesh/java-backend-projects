package com.lokesh.ecommerce.product.service;

import com.lokesh.ecommerce.product.entity.Category;
import com.lokesh.ecommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category create(String name, String description, UUID parentCategoryId) {
        Category c = Category.builder()
                .name(name)
                .description(description)
                .parentCategoryId(parentCategoryId)
                .active(true)
                .build();
        return categoryRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<Category> listActive() {
        return categoryRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Category getByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + name));
    }

    @Transactional
    public Category update(UUID id, String name, String description, UUID parentCategoryId, boolean active) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        c.setName(name);
        c.setDescription(description);
        c.setParentCategoryId(parentCategoryId);
        c.setActive(active);
        return categoryRepository.save(c);
    }

    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeNode> getCategoryTree() {
        List<Category> all = categoryRepository.findByActiveTrue();
        Map<UUID, List<Category>> childrenByParent = all.stream()
                .filter(c -> c.getParentCategoryId() != null)
                .collect(Collectors.groupingBy(Category::getParentCategoryId));
        List<Category> roots = all.stream()
                .filter(c -> c.getParentCategoryId() == null)
                .toList();
        List<CategoryTreeNode> result = new ArrayList<>();
        for (Category root : roots) {
            result.add(buildNode(root, childrenByParent));
        }
        return result;
    }

    private static CategoryTreeNode buildNode(Category c, Map<UUID, List<Category>> childrenByParent) {
        List<CategoryTreeNode> childNodes = childrenByParent.getOrDefault(c.getId(), List.of()).stream()
                .map(ch -> buildNode(ch, childrenByParent))
                .toList();
        return new CategoryTreeNode(c.getId(), c.getName(), c.getDescription(), childNodes);
    }

    public record CategoryTreeNode(UUID id, String name, String description, List<CategoryTreeNode> children) {
    }
}
