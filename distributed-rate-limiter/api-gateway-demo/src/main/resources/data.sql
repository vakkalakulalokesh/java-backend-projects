INSERT INTO api_routes (route_path, target_url, method, rate_limit_max_requests, rate_limit_window_ms, rate_limit_algorithm, active, description, created_at, updated_at)
VALUES ('/api/v1/external/demo', 'https://httpbin.org/get', 'GET', 5, 60000, 'FIXED_WINDOW', true, 'Sample route protected by fixed-window Redis limiter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO api_routes (route_path, target_url, method, rate_limit_max_requests, rate_limit_window_ms, rate_limit_algorithm, active, description, created_at, updated_at)
VALUES ('/api/v1/external/premium', 'https://httpbin.org/uuid', 'GET', 30, 60000, 'SLIDING_WINDOW_COUNTER', true, 'Higher-throughput route using sliding window counter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO api_routes (route_path, target_url, method, rate_limit_max_requests, rate_limit_window_ms, rate_limit_algorithm, active, description, created_at, updated_at)
VALUES ('/api/v1/external/burst', 'https://httpbin.org/headers', 'GET', 10, 10000, 'TOKEN_BUCKET', true, 'Burst-friendly token bucket policy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
