DROP TABLE IF EXISTS statistics_entry;
DROP TABLE IF EXISTS uniprotkb_statistics_entry;
DROP TABLE IF EXISTS statistics_category;
CREATE TABLE statistics_category
(
    id           SERIAL PRIMARY KEY,
    category     varchar(128) NOT NULL,
    db_type      varchar(64) NULL,
    label        varchar(64) NULL,
    search_field varchar(64) NULL
);



CREATE TABLE uniprotkb_statistics_entry
(
    id                     SERIAL PRIMARY KEY,
    attribute_name         varchar(500) NOT NULL,
    statistics_category_id int8         NOT NULL,
    value_count            int8 NULL,
    entry_count            int8 NULL,
    description            varchar NULL,
    release_name           varchar(32)  NOT NULL,
    entry_type             varchar(32) NULL,
    CONSTRAINT uniprotkb_statistics_entry_un UNIQUE (attribute_name, statistics_category_id, release_name, entry_type)
);

ALTER TABLE uniprotkb_statistics_entry
    ADD CONSTRAINT uniprotkb_statistics_entry_fk FOREIGN KEY (statistics_category_id) REFERENCES statistics_category (id);

INSERT INTO statistics_category (id, category, db_type, label, search_field)
VALUES (39, 'EUKARYOTA', 'UNIPROTKB', 'Eukaryota', 'sf Eukaryota');
INSERT INTO statistics_category (id, category, db_type, label, search_field)
VALUES (45, 'SEQUENCE_AMINO_ACID', 'UNIPROTKB', 'Sequence Amino Acid', 'sf Sequence Amino Acid');
INSERT INTO statistics_category (id, category, db_type, label, search_field)
VALUES (52, 'TOP_ORGANISM', 'UNIPROTKB', 'Top Organism', 'sf Organism');


INSERT INTO uniprotkb_statistics_entry (id, attribute_name, statistics_category_id, value_count, entry_count,
                                        description, release_name, entry_type)
VALUES (47549, 'Fungi', 39, 35360, 35360, null, '2021_03', 'SWISSPROT');
INSERT INTO uniprotkb_statistics_entry (id, attribute_name, statistics_category_id, value_count, entry_count,
                                        description, release_name, entry_type)
VALUES (47550, 'Insecta', 39, 9457, 9457, null, '2021_03', 'SWISSPROT');
INSERT INTO uniprotkb_statistics_entry (id, attribute_name, statistics_category_id, value_count, entry_count,
                                        description, release_name, entry_type)
VALUES (50289, 'Fungi', 39, 12793422, 12793422, null, '2021_03', 'TREMBL');
INSERT INTO uniprotkb_statistics_entry (id, attribute_name, statistics_category_id, value_count, entry_count,
                                        description, release_name, entry_type)
VALUES (47175, 'AMINO_ACID_U', 45, 329, 254, null, '2021_03', 'SWISSPROT');
INSERT INTO uniprotkb_statistics_entry (id, attribute_name, statistics_category_id, value_count, entry_count,
                                        description, release_name, entry_type)
VALUES (47206, 'Salmonella paratyphi B (strain ATCC BAA-1250 / SPB7)', 52, 716, 716, null, '2021_03', 'SWISSPROT');