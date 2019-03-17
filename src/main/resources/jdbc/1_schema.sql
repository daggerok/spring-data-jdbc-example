drop table if exists genre;
drop table if exists book;

create table genre(
  id bigserial not null
    constraint genre_pkey
      primary key,
  name varchar(255) not null,
  book bigint not null, -- this one is required for proper one to many FK (see Book.genre field)
);

create table book(
  id bigserial not null
    constraint book_pkey
      primary key,
  last_modified timestamp null,
  aggregate_id varchar(36) not null,
  content varchar(4096) not null,
);
