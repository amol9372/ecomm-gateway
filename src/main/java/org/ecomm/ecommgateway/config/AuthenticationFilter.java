package org.ecomm.ecommgateway.config;

import lombok.extern.slf4j.Slf4j;
import org.ecomm.ecommgateway.rest.auth0.Auth0ServiceClient;
import org.ecomm.ecommgateway.rest.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Function;

@Component
@RefreshScope
@Slf4j
public class AuthenticationFilter implements GlobalFilter {

  @Autowired RouterValidator routerValidator;

  @Autowired Auth0ServiceClient auth0ServiceClient;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    if (routerValidator.isSecured.test(request)) {
      if (this.isAuthMissing(request)) {
        log.info("Auth header is missing");
        return this.onError(exchange, Constants.AUTH_HEADER_MISSING);
      }

      /*
      Access token is already validated - we just need to get user details from the token
       */
      String authHeader = request.getHeaders().getOrEmpty("Authorization").get(0);
      UserInfo userInfo = auth0ServiceClient.getUserInfo(authHeader.split(" ")[1]);

      return chain.filter(
          exchange
              .mutate()
              .request(
                  exchange
                      .getRequest()
                      .mutate()
                      .header("x-auth0-user-email", userInfo.getEmail())
                      .build())
              .build());
    }
    return chain.filter(exchange);
  }

  BiFunction<UserInfo, ServerWebExchange, ServerHttpRequest> mutatedRequest =
      (userInfo, exchange) -> {
        ServerHttpRequest originalRequest = exchange.getRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(originalRequest.getHeaders());
        headers.add("x-auth0-user-email", userInfo.getEmail());

        return new ServerHttpRequestDecorator(originalRequest) {
          @Override
          public HttpHeaders getHeaders() {
            return headers;
          }
        };
      };

  private Mono<Void> onError(ServerWebExchange exchange, String err) {
    log.info("User is unauthorised ::: {}", err);
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    return response.setComplete();
  }

  private boolean isAuthMissing(ServerHttpRequest request) {
    return !request.getHeaders().containsKey("Authorization");
  }
}
