package com.github.daggerok;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Transactional // do not forget
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class BookRepositoryTest {

  @LocalServerPort
  int port;

  @Autowired
  BookRepository bookRepository;

  @Autowired
  RestTemplate restTemplate;

  @Test
  void jdbc() {
    Book noGenreBook = bookRepository.save(Book.of("no genre"));
    assertThat(noGenreBook.getAggregateId()).isNotNull();

    Long id = noGenreBook.getId();
    LocalDateTime lastModified = noGenreBook.getLastModified();
    Book bookWithGenre = bookRepository.save(noGenreBook.withGenre(Genre.UNDEFINED)
                                                        .withLastModifiedUpdated());
    assertThat(bookWithGenre.getGenre()).isNotNull();
    assertThat(bookWithGenre.getId()).isEqualTo(id);
    assertThat(bookWithGenre.getLastModified()).isNotEqualTo(lastModified)
                                               .isAfter(lastModified);
  }

  @Test
  void rest() {
    String baseUrl = format("http://127.0.0.1:%s/", port);
    ResponseEntity<List> resp = restTemplate.postForEntity(baseUrl, singletonMap("content", "test text"), List.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    List list = resp.getBody();
    assertThat(list).hasSize(2);

    for (Object o : list) {
      Map map = Map.class.cast(o);
      Object id = Objects.requireNonNull(map.get("id"), "id not found.");
      restTemplate.delete(format("%s/book/%s", baseUrl, id));
    }

    ResponseEntity<List> books = restTemplate.getForEntity(format("%s/books", baseUrl), List.class);
    assertThat(books.getStatusCode()).isEqualTo(HttpStatus.OK);
    List listOfBooks = books.getBody();
    assertThat(listOfBooks).hasSize(0);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    RestTemplate restTemplate() {
      return new RestTemplate();
    }
  }
}
