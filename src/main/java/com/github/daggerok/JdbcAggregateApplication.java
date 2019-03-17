package com.github.daggerok;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Wither;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;

@Configuration
class MyDS {

  @Bean
  DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                                        //.addScripts("jdbc/1_schema.sql", "jdbc/2_data.sql")
                                        .addScripts("jdbc/1_schema.sql")
                                        .build();
  }
}

/**
 * See 1_schema.sql script. we have added my_table column in referencing entity in my_sub_table table
 * <p>
 * References to other entities. They are considered a one-to-one relationship. It is optional
 * for such entities to have an id attribute. The table of the referenced entity is expected
 * to have an additional column named the same as the table of the referencing entity. You can
 * change this name by implementing NamingStrategy.getReverseColumnName(RelationalPersistentProperty property).
 */
@Wither // Wither is important! We need it, because we are using immutable value objects...
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
class Genre {

  public static final Genre UNDEFINED = new Genre(null, "Undefined");

  @Id
  private final Long id;
  private final String name;
}

interface GenreRepository extends CrudRepository<Genre, Long> {}

@Wither
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
class Book { // AggregateRoot

  @Id
  private final Long id;

  @LastModifiedDate // But not ZonedDateTime...
  private final LocalDateTime lastModified;

  private final UUID aggregateId; // org.springframework.core.convert.support.StringToUUIDConverter
  private final String content;
  private final Genre genre; // one-to-one

  public static Book of(String content) {
    return new Book(null, LocalDateTime.now(), UUID.randomUUID(), content, Genre.UNDEFINED);
  }
}

interface BookRepository extends CrudRepository<Book, Long> {}

/* // Seems like this one os optional...
@Configuration
@EnableJdbcRepositories
@RequiredArgsConstructor
class MyJDBC extends JdbcConfiguration {

  final DataSource dataSource;

  @Bean
  NamedParameterJdbcOperations operations() {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  @Bean
  PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource);
  }
}
*/

@Log4j2
@RestController
@RequiredArgsConstructor
class MyREST {

  final BookRepository books;
  final GenreRepository genres;

  @DeleteMapping("/book/{id}")
  public CompletableFuture<Void> deleteBook(@PathVariable("id") Long id) {
    return CompletableFuture.supplyAsync(() -> {
      books.deleteById(id);
      return null;
    });
  }

  @GetMapping("/books")
  public Iterable<Book> getBooks() {
    return books.findAll();
  }

  @GetMapping("/genres")
  public Iterable<Genre> getGenres() {
    return genres.findAll();
  }

  @PostMapping
  public CompletableFuture<Book> post(@RequestBody Map<String, String> request) {
    return CompletableFuture.supplyAsync(() -> {
      String content = Objects.requireNonNull(request.get("content"), "content parameter is required.");
      //Book book = new Book(UUID.randomUUID(), data);
      Book book = books.save(Book.of(content));
      log.info("book created: {}", book);
      return book;
    });
  }

  /*@PostMapping
  public ResponseEntity<?> post(@RequestBody Map<String, String> request, HttpServletRequest req) {
    String data = Objects.requireNonNull(request.get("data"), "data parameter is required.");
    Book myEntity = new Book(UUID.randomUUID(), data);
    Book saved = myEntityRepository.save(myEntity);
    log.info("saved: {}", saved);
    return ResponseEntity.created(URI.create(format("/%s", saved.getId())))
                         .body(saved);
  }*/

  @RequestMapping
  public ResponseEntity<List> apiFallback() {
    return ResponseEntity.ok(
        asList(
            "   get books:  http get  :8080/books",
            "  get genres:  http get  :8080/genres",
            " create book:  http post :8080 content={content}"
        )
    );
  }
}

@SpringBootApplication
public class JdbcAggregateApplication {

  public static void main(String[] args) {
    SpringApplication.run(JdbcAggregateApplication.class, args);
  }
}
