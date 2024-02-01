package org.ecomm.ecommgateway.config;

import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.interfaces.RSAPublicKey;

import lombok.extern.slf4j.Slf4j;
import org.ecomm.ecommgateway.rest.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwsFilter {

  @Value("${auth0.issuer}")
  String issuer;

  public UserInfo validateToken(String authToken) {
    JwkProvider provider = new UrlJwkProvider(issuer);
    UserInfo.UserInfoBuilder userInfoBuilder = UserInfo.builder();
    try {
      DecodedJWT jwt = JWT.decode(authToken);
      // Get the kid from received JWT token
      Jwk jwk = provider.get(jwt.getKeyId());

      Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

      JWTVerifier verifier =
          JWT.require(algorithm).withIssuer(issuer).build();

      jwt = verifier.verify(authToken);

      log.info("jwt is {}", jwt.getClaims());

      userInfoBuilder
          .sub(jwt.getClaims().get("sub").asString())
          .email(jwt.getClaims().get("email").asString())
          .name(jwt.getClaims().get("name").asString())
          .picture(jwt.getClaims().get("picture").asString());

    } catch (JWTVerificationException e) {
      // Invalid signature/claims
      log.error("Invalid JWT claim {}", e.getMessage());
    } catch (JwkException e) {
      // invalid JWT token
      log.error("Invalid JWT Token {}", e.getMessage());
    }
    return userInfoBuilder.build();
  }
}
