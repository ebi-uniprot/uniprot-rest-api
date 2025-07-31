DROP TABLE IF EXISTS map_to_job;
DROP TABLE IF EXISTS map_to_result;

-- auto-generated definition
create table map_to_job
(
    id               bigserial
        primary key,
    created          timestamp,
    code             integer,
    message          text,
    include_isoform  boolean,
    job_id           varchar(255) not null
        constraint uk_gmupn9ha4oee5msnbivofjbq3
            unique,
    query            varchar(255) not null,
    sourcedb         integer      not null,
    status           integer      not null,
    targetdb         integer      not null,
    total_target_ids bigint,
    updated          timestamp
);

create index idxpbd12bn3xy9pc0qufbe6x4e3e
    on map_to_job (job_id);



-- auto-generated definition
create table map_to_result
(
    id            bigserial
        primary key,
    target_id     varchar(255) not null,
    map_to_job_id bigint       not null
        constraint fk438dj87nxr8dok95mx58xduuk
            references map_to_job
);

create index idxa5j88975qoff4syv2th7hwm66
    on map_to_result (map_to_job_id);