# Spring Data JDBC
Quickstart with spring-data-jdbc. Fast and light relational database access almost with no overhead (still many opened questions here and a lot of stuff to learn...)

## run

```bash
./mvnw spring-boot:run 
```

## test

_available APIs_

```bash
http :8080
HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Date: Sun, 17 Mar 2019 00:45:30 GMT
Transfer-Encoding: chunked
# output:
```

```json
[
    "   get books:  http get    :8080/books",
    "   find book:  http get    :8080/book/{id}",
    " delete book:  http delete :8080/book/{id}",
    " create book:  http post   :8080 content={content}"
]
```

_create a book_

```bash
http :8080 content="my book"
HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Date: Sun, 17 Mar 2019 00:46:39 GMT
Transfer-Encoding: chunked
# output:
```

```json
{
    "aggregateId": "7f5d54ce-543b-4d60-9b3d-667aa0f5fcc8",
    "content": "my book",
    "genre": {
        "id": 1,
        "name": "Undefined"
    },
    "id": 1,
    "lastModified": "2019-03-17T02:46:39.828"
}
```

_list of genres_

```bash
http :8080/genres
HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Date: Sun, 17 Mar 2019 00:47:18 GMT
Transfer-Encoding: chunked
# output:
```

```json
[
    {
        "id": 1,
        "name": "Undefined"
    }
]
```

_list of books_

```bash
http :8080/books
HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Date: Sun, 17 Mar 2019 00:47:50 GMT
Transfer-Encoding: chunked
# output:
```

```json
[
    {
        "aggregateId": "7f5d54ce-543b-4d60-9b3d-667aa0f5fcc8",
        "content": "my book",
        "genre": {
            "id": 1,
            "name": "Undefined"
        },
        "id": 1,
        "lastModified": "2019-03-17T02:46:39.828"
    }
]
```

* [Part 1: Spring Data JDBC - Getting started introduction](https://spring.io/blog/2018/09/17/introducing-spring-data-jdbc)
* [Part 2: Spring Data JDBC - References and Aggregates](https://spring.io/blog/2018/09/24/spring-data-jdbc-references-and-aggregates)
* [Domain-Driven Design and Spring](http://static.olivergierke.de/lectures/ddd-and-spring/)
* [YouTube: Spring Data JDBC - One to One & One to Many Relationships](https://www.youtube.com/watch?v=ccxBXDAPdmo)
