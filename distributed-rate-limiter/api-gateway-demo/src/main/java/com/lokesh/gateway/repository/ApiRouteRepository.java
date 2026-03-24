package com.lokesh.gateway.repository;

import com.lokesh.gateway.model.ApiRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {

    Optional<ApiRoute> findByPathAndMethod(String path, String method);

    List<ApiRoute> findByActiveTrue();
}
