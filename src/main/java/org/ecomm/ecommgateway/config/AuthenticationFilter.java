package org.ecomm.ecommgateway.config;

import com.okta.spring.boot.oauth.http.Auth0ClientRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.ecomm.ecommgateway.rest.auth0.Auth0ServiceClient;
import org.ecomm.ecommgateway.rest.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

      this.populateRequestWithHeaders(exchange, userInfo);
    }
    return chain.filter(exchange);
  }

  private Mono<Void> onError(ServerWebExchange exchange, String err) {
    log.info("User is unauthorised ::: {}", err);
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    return response.setComplete();
  }

  private String getAuthHeader(ServerHttpRequest request) {
    return request.getHeaders().getOrEmpty("Authorization").get(0);
    // return request.getHeaders().getOrEmpty("Authorization").get(0).split(" ")[1];
  }

  private boolean isAuthMissing(ServerHttpRequest request) {
    return !request.getHeaders().containsKey("Authorization");
  }

  private void populateRequestWithHeaders(ServerWebExchange exchange, UserInfo userInfo) {

    ServerHttpRequest mutatedRequest =
        new ServerHttpRequestDecorator(exchange.getRequest()) {
          @Override
          public HttpHeaders getHeaders() {
            HttpHeaders httpHeaders = new HttpHeaders();
            //httpHeaders.addAll(super.getHeaders());
            httpHeaders.add("x-auth0-user-email", userInfo.getEmail());
            return httpHeaders;
          }
        };

    exchange.mutate().request(mutatedRequest).build();
  }
}
