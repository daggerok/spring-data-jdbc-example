package com.github.daggerok;

import lombok.*;
import lombok.experimental.Wither;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PACKAGE;

@Configuration
class MyDS {

  @Bean
  public DataSource dataSource() {
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
@AllArgsConstructor(access = PACKAGE)
class Genre { // reference to Book AggregateRoot
  public static final Genre UNDEFINED = new Genre(null, "Undefined");

  @Id
  private final Long id;
  private final String name;
}

@Wither
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = PACKAGE)
class Book { // AggregateRoot

  @Id
  private final Long id;
  private final LocalDateTime lastModified; // But not ZonedDateTime...
  private final UUID aggregateId;
  private final String content;
  private final Genre genre; // one-to-one

  public Book withLastModifiedUpdated() {
    return this.withLastModified(LocalDateTime.now());
  }

  public static Book of(String content) {
    return new Book(null, LocalDateTime.now(), UUID.randomUUID(), content, null);
  }

  public static Book of(String content, Genre genre) {
    return new Book(null, LocalDateTime.now(), UUID.randomUUID(), content, genre);
  }
}

@Repository
interface BookRepository extends CrudRepository<Book, Long> {

//  @Query(
//      "   SELECT                                    " +
//      "       book.id AS id                         " +
//      "     , book.last_modified AS last_modified   " +
//      "     , book.aggregate_id AS aggregate_id     " +
//      "     , book.content AS content               " +
//      "   FROM book                                 "
//  )
//  Iterable<Book> findMeAllBooks();
}

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
@Transactional // do not forget use transaction
@RestController
@RequiredArgsConstructor
class MyREST {

  final BookRepository books;

  @DeleteMapping("/book/{id}")
  public CompletableFuture<String> deleteBook(@PathVariable("id") Long id) {
    return CompletableFuture.supplyAsync(() -> {
      books.findById(id)
           .map(book -> {
             log.info("going to remove a book: {}", book);
             return book;
           })
           .ifPresent(books::delete);
      return "Accepted.";
    });
  }

  @GetMapping("/book/{id}")
  public Book getBook(@PathVariable("id") Long id) {
    return books.findById(id).orElse(null);
  }

  @GetMapping("/books")
  public Iterable<Book> getBooks() {
    return books.findAll();
  }

  @PostMapping
  public CompletableFuture<Iterable<Book>> post(@RequestBody Map<String, String> request) {
    return CompletableFuture.supplyAsync(() -> {
      String content = Objects.requireNonNull(request.get("content"), "content parameter is required.");
      Book book1 = books.save(Book.of(content));
      log.info("created book without genre: {}", book1);
      Book book2 = books.save(Book.of(content, Genre.UNDEFINED));
      log.info("created book with genre: {}", book2);
      return asList(book1, book2);
    });
  }

  @RequestMapping
  public ResponseEntity<List> apiFallback() {
    return ResponseEntity.ok(
        asList(
            "   get books:  http get    :8080/books",
            "   find book:  http get    :8080/book/{id}",
            " delete book:  http delete :8080/book/{id}",
            " create book:  http post   :8080 content={content}"
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
