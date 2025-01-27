--liquibase formatted sql
--changeset Samuel Chan:v1.0.2
--comment: add surrogate keys
alter TABLE lightrag_doc_full add column surrogate_id bigserial;
alter TABLE lightrag_doc_status add column surrogate_id bigserial;
alter table lightrag_llm_cache add column surrogate_id bigserial;
alter TABLE lightrag_vdb_entity add column surrogate_id bigserial;
alter TABLE lightrag_vdb_relation add column surrogate_id bigserial;
alter table lightrag_doc_chunks add column surrogate_id bigserial;

ALTER TABLE lightrag_doc_chunks DROP CONSTRAINT lightrag_doc_chunks_pk;
ALTER TABLE lightrag_doc_chunks ADD PRIMARY KEY (surrogate_id);
CREATE UNIQUE INDEX lightrag_doc_chunks_mk ON lightrag_doc_chunks (workspace, id);

ALTER TABLE lightrag_doc_full DROP CONSTRAINT lightrag_doc_full_pk;
ALTER TABLE lightrag_doc_full ADD PRIMARY KEY (surrogate_id);
CREATE UNIQUE INDEX lightrag_doc_full_mk on lightrag_doc_full (workspace, id);


ALTER TABLE lightrag_doc_status DROP CONSTRAINT lightrag_doc_status_pk;
ALTER TABLE lightrag_doc_status ADD PRIMARY KEY (surrogate_id);
CREATE UNIQUE INDEX lightrag_doc_status_mk on lightrag_doc_status(workspace, id);

ALTER TABLE lightrag_llm_cache DROP CONSTRAINT lightrag_llm_cache_pk;
ALTER TABLE lightrag_llm_cache ADD PRIMARY KEY (surrogate_id);
CREATE UNIQUE INDEX lightrag_llm_cache_mk on lightrag_llm_cache(workspace, mode, id);

ALTER TABLE lightrag_vdb_entity DROP CONSTRAINT lightrag_vdb_entity_pk;
ALTER TABLE lightrag_vdb_entity ADD PRIMARY KEY (surrogate_id);
CREATE UNIQUE INDEX lightrag_vdb_entity_mk on lightrag_vdb_entity(workspace, id);

ALTER TABLE lightrag_vdb_relation DROP CONSTRAINT lightrag_vdb_relation_pk;
ALTER TABLE lightrag_vdb_relation ADD PRIMARY KEY (surrogate_id);
CREATE UNIQUE INDEX lightrag_vdb_relation_mk on lightrag_vdb_relation(workspace, id);
--rollback;