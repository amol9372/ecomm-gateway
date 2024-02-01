package org.ecomm.ecommgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
    return http.csrf()
        .disable()
        .authorizeExchange(
            authorizeExchangeSpec -> {
              authorizeExchangeSpec
                  .pathMatchers("/**")
//                  .access((authentication, object) -> Mono.just(new AuthorizationDecision(true)))
//                  .anyExchange()
                  .permitAll();
            })
        .build();
  }
}
