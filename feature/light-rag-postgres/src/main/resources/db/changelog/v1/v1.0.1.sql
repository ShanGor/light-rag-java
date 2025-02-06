--liquibase formatted sql
--changeset Samuel Chan:v1.0.1
--comment: add graph properties column
alter TABLE LIGHTRAG_VDB_ENTITY add column graph_properties text NULL;
alter TABLE LIGHTRAG_VDB_RELATION add column graph_properties text NULL;

--rollback;