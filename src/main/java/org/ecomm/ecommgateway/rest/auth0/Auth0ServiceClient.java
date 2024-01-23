package org.ecomm.ecommgateway.rest.auth0;

import lombok.extern.slf4j.Slf4j;
import org.ecomm.ecommgateway.rest.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class Auth0ServiceClient {

  @Autowired RestTemplate restTemplate;

  @Value("${auth0.userinfo.url}")
  String userInfoUrl;

  public UserInfo getUserInfo(String token) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    HttpEntity<String> entity = new HttpEntity<>("body", headers);
    ResponseEntity<UserInfo> response =
        restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, UserInfo.class);

    return response.getBody();
  }
}
