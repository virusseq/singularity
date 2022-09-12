CREATE TABLE if not exists total_count
(
    id                          uuid        NOT NULL DEFAULT uuid_generate_v4(),
    files                       bigint      NOT NULL,
    samples                     bigint      NOT NULL,
    studies                     int         NOT NULL,
    file_size_bytes             bigint      NOT NULL,
    file_size_human_readable    VARCHAR     NOT NULL CHECK (file_size_human_readable <> ''),
    timestamp                   bigint      NOT NULL,
    PRIMARY KEY (id)
);

