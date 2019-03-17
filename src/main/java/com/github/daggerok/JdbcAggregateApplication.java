package com.github.daggerok;

import lombok.*;
import lombok.experimental.Wither;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Configuration
class MyDS {

  @Bean
  DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                                        .addScripts("jdbc/1_schema.sql"/*, "jdbc/2_data.sql"*/)
                                        .build();
  }
}

/**
 * See 1_schema.sql script. we have added my_table column in referencing entity in my_sub_table table
 *
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
class Author {

  @Id
  private final Long id;
  private final String name;

  public static Author of(String name) {
    return new Author(null, name);
  }
}

interface AuthorRepository extends CrudRepository<Author, Long> {}

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Table("Book_Author")
class AuthorRef {
  @NonNull private Long author;
}

@Wither
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
class Book {

  @Id
  private final Long id;

  @LastModifiedDate // But not ZonedDateTime...
  private final LocalDateTime lastModified;

  private final UUID aggregateId; // org.springframework.core.convert.support.StringToUUIDConverter
  private final String content;
  private final Genre genre;
  private final Set<AuthorRef> authors;

  Book withAuthor(Author author) {
    Objects.requireNonNull(author, "author may not be null.");
    Long authorId = Objects.requireNonNull(author.getId(), "author ID may not be null.");
    AuthorRef ref = new AuthorRef(authorId);
    authors.add(ref);
    return this.withLastModified(LocalDateTime.now());
  }

  public static Book of(String content) {
    Genre undefinedGenre = new Genre(null, "Undefined");
    return new Book(
        null, LocalDateTime.now(), UUID.randomUUID(), content, undefinedGenre, new HashSet<>()
    );
  }
}

interface BookRepository extends CrudRepository<Book, Long> {

  @Query("  select b.id, b.last_modified, b.aggregate_id, b.content " +
      "     from book b left outer join genre g on g.book = b.id    ")
  List<Book> findBooks();
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
@RestController
@RequiredArgsConstructor
class MyREST {

  final BookRepository books;
  final GenreRepository genres;
  final AuthorRepository authors;

  @GetMapping("/books")
  public Iterable<Book> getBooks() {
    return books.findBooks();
  }

  @GetMapping("/genres")
  public Iterable<Genre> getGenres() {
    return genres.findAll();
  }

  @GetMapping("/authors")
  public Iterable<Author> getAuthors() {
    return authors.findAll();
  }

  @PostMapping
  public CompletableFuture<Book> post(@RequestBody Map<String, String> request) {
    return CompletableFuture.supplyAsync(() -> {
      String content = Objects.requireNonNull(request.get("content"), "content parameter is required.");
      //Book book = new Book(UUID.randomUUID(), data);
      Book book = books.save(Book.of(content));
      log.info("book created: {}", book);
      Author author = authors.save(Author.of("me"));
      return books.save(book.withAuthor(author));
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
  public Iterable<Book> apiFallback() {
    return books.findAll();
  }
}

@SpringBootApplication
public class JdbcAggregateApplication {

  public static void main(String[] args) {
    SpringApplication.run(JdbcAggregateApplication.class, args);
  }
}
