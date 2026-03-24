package com.lokesh.gateway.service;

import com.lokesh.gateway.model.ApiRoute;
import com.lokesh.gateway.repository.ApiRouteRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final ApiRouteRepository repository;
    private volatile Map<String, ApiRoute> activeRouteCache = Map.of();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        activeRouteCache = repository.findByActiveTrue().stream()
                .collect(Collectors.toConcurrentMap(
                        r -> cacheKey(r.getPath(), r.getMethod()),
                        r -> r,
                        (a, b) -> b));
    }

    private String cacheKey(String path, String method) {
        return method.toUpperCase() + " " + path;
    }

    public Optional<ApiRoute> findActiveRoute(String path, String method) {
        return Optional.ofNullable(activeRouteCache.get(cacheKey(path, method)));
    }

    public List<ApiRoute> findAll() {
        return repository.findAll();
    }

    @Transactional
    public ApiRoute create(ApiRoute route) {
        ApiRoute saved = repository.save(route);
        refreshCache();
        return saved;
    }

    @Transactional
    public Optional<ApiRoute> update(Long id, ApiRoute update) {
        return repository.findById(id).map(existing -> {
            existing.setPath(update.getPath());
            existing.setTargetUrl(update.getTargetUrl());
            existing.setMethod(update.getMethod());
            existing.setRateLimitMaxRequests(update.getRateLimitMaxRequests());
            existing.setRateLimitWindowMs(update.getRateLimitWindowMs());
            existing.setRateLimitAlgorithm(update.getRateLimitAlgorithm());
            existing.setActive(update.isActive());
            existing.setDescription(update.getDescription());
            ApiRoute saved = repository.save(existing);
            refreshCache();
            return saved;
        });
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        refreshCache();
    }
}
