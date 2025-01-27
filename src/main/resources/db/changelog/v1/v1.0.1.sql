--liquibase formatted sql
--changeset Samuel Chan:v1.0.1
--comment: add graph properties column
alter TABLE lightrag_vdb_entity add column graph_properties text NULL;
alter TABLE lightrag_vdb_relation add column graph_properties text NULL;

--rollback;