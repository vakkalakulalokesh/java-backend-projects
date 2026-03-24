package com.lokesh.gateway.controller;

import com.lokesh.gateway.model.ApiRoute;
import com.lokesh.gateway.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
@Tag(name = "Gateway routes", description = "CRUD for proxied API routes and rate limit configuration")
public class GatewayController {

    private final RouteService routeService;

    @PostMapping("/routes")
    @Operation(summary = "Create route")
    public ResponseEntity<ApiRoute> create(@RequestBody ApiRoute route) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.create(route));
    }

    @GetMapping("/routes")
    @Operation(summary = "List routes")
    public List<ApiRoute> list() {
        return routeService.findAll();
    }

    @PutMapping("/routes/{id}")
    @Operation(summary = "Update route")
    public ResponseEntity<ApiRoute> update(@PathVariable Long id, @RequestBody ApiRoute route) {
        return routeService.update(id, route)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/routes/{id}")
    @Operation(summary = "Delete route")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        routeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
