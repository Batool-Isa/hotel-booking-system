package com.ga.apigateway.security;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {


    @Value("${jwt-secret}")
    private String jwtSecret;

    private static final String[] OPEN_PATHS = {
            "/auth/users/login",
            "/auth/users/register",
            "/auth/users/verify",
            "/auth/users/forgot",
            "/auth/users/reset",
            "/webhook",
            "/swagger-ui",
            "/v3/api-docs",
            "/uploads"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // if public path let guest in it
        if (isOpenPage(request, path)) {
            return chain.filter(exchange);
        }

        //if not , check JWT token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");
        if (!hasToken) {
            return sendUnauthorized(exchange);
        }

        // check token validity
        String token = authHeader.substring(7);
        try {
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token);
        } catch (JwtException | IllegalArgumentException e) {
            return sendUnauthorized(exchange);
        }

        return chain.filter(exchange);
    }

    private boolean isOpenPage(ServerHttpRequest request, String path) {

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return true;
        }


        for (String openPath : OPEN_PATHS) {
            if (path.startsWith(openPath)) {
                return true;
            }
        }

        boolean isGet = HttpMethod.GET.equals(request.getMethod());
        boolean isHotelPath = path.startsWith("/api/hotels");
        boolean isPendingList = path.startsWith("/api/hotels/pending");
        return isGet && isHotelPath && !isPendingList;
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String message = "{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid token.\"}";
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    // this filter runs first, before the request is sent to a service
    @Override
    public int getOrder() {
        return -1;
    }
}