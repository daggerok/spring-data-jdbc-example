drop table if exists genre;
drop table if exists book;

create table book(
  id bigserial not null primary key,
  last_modified timestamp not null,
  aggregate_id varchar(36) not null,
  content varchar(4096) not null,
);

create table genre(
  id bigserial not null primary key, -- this one is required for proper one to many FK (see Book.genre field)
  name varchar(255) not null,
  book bigint not null,
  foreign key (book) references book(id), -- this one is required for proper one to many FK (see Book.genre field)
);

-- alter table genre
--   add foreign key (book)
--   references book(id)
