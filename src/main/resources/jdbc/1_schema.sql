drop table if exists author;
drop table if exists genre;
drop table if exists book;

create table author(
  id bigserial not null
    constraint author_pkey
      primary key,
  name varchar(255) not null,
--  book bigint not null,
);

create table genre(
  id bigserial not null
    constraint genre_pkey
      primary key,
  name varchar(255) not null,
  book bigint not null,
);

create table book_author(
  book bigint not null,
  author bigint not null,
);

create table book(
  id bigserial not null
    constraint book_pkey
      primary key,
  last_modified timestamp null,
  aggregate_id varchar(36) not null,
  content varchar(4096) not null,
);
