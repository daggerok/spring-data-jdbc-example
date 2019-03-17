package com.github.daggerok;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class JdbcAggregateApplicationTests {

  @Autowired
  RestTemplate restTemplate;

  @LocalServerPort
  Integer port;

  @Test
  void contextLoads() {
    String baseUrl = format("http://127.0.0.1:%s", port);
    ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, singletonMap("data", "hello"), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map map = response.getBody();
    assertThat(map).isNotNull();
    assertThat(map).hasSize(6);
    assertThat(map.containsKey("id")).isTrue();
    assertThat(map.containsKey("lastModified")).isTrue();
    assertThat(map.containsKey("aggregateId")).isTrue();
    assertThat(map.containsKey("mySubEntity")).isTrue();
    assertThat(map.get("data")).isEqualTo("hello");

    Object mySubEntity = map.get("mySubEntity");
    Map subMap = (Map) mySubEntity;
    assertThat(subMap.containsKey("id")).isTrue();
    assertThat(subMap.containsKey("status")).isTrue();
    assertThat(subMap.get("status")).isEqualTo("state hello");

    ResponseEntity<List> response2 = restTemplate.getForEntity(baseUrl + "/" + map.get("aggregateId"), List.class);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

    List list = Objects.requireNonNull(response2.getBody());
    assertThat(list).hasSize(1);

    Iterator iterator = Objects.requireNonNull(list.iterator());
    Object o = iterator.next();
    Map myEntity = (Map) o;
    assertThat(myEntity).isNotNull();
    assertThat(myEntity).hasSize(6);
    assertThat(myEntity.containsKey("data")).isTrue();
    assertThat(myEntity.get("data")).isEqualTo(map.get("data"));
  }

  @Configuration
  @TestConfiguration
  static class TestCfg {

    @Bean
    public RestTemplate restTemplate() {
      return new RestTemplate();
    }
  }
}
