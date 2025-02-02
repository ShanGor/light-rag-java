--liquibase formatted sql
--changeset Samuel Chan:v1.0.3
--comment: cache for node degree and edge details
alter TABLE LIGHTRAG_VDB_ENTITY add column graph_node_degree int NULL;
alter TABLE LIGHTRAG_VDB_RELATION add column graph_edge_degree int NULL;
alter TABLE LIGHTRAG_VDB_RELATION add column graph_start_node text NULL;
alter TABLE LIGHTRAG_VDB_RELATION add column graph_start_node_degree int NULL;
alter TABLE LIGHTRAG_VDB_RELATION add column graph_end_node text NULL;
alter TABLE LIGHTRAG_VDB_RELATION add column graph_end_node_degree int NULL;
--rollback;