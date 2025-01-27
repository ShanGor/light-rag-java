--liquibase formatted sql
--changeset Samuel Chan:v1
CREATE TABLE if not exists public.lightrag_doc_chunks (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    full_doc_id varchar(256) NULL,
    chunk_order_index int4 NULL,
    tokens int4 NULL,
    "content" text NULL,
    content_vector public.vector NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT lightrag_doc_chunks_pk PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.lightrag_doc_full (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    doc_name varchar(1024) NULL,
    "content" text NULL,
    meta jsonb NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT lightrag_doc_full_pk PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.lightrag_doc_status (
    workspace varchar(255) NOT NULL,
    id varchar(255) NOT NULL,
    content_summary varchar(255) NULL,
    content_length int4 NULL,
    chunks_count int4 NULL,
    status varchar(64) NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT lightrag_doc_status_pk PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.lightrag_llm_cache (
    workspace varchar(255) NOT NULL,
    id varchar(255) NOT NULL,
    "mode" varchar(32) NOT NULL,
    original_prompt text NULL,
    return_value text NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT lightrag_llm_cache_pk PRIMARY KEY (workspace, mode, id)
);
CREATE TABLE if not exists public.lightrag_vdb_entity (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    entity_name varchar(255) NULL,
    "content" text NULL,
    content_vector public.vector NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT lightrag_vdb_entity_pk PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.lightrag_vdb_relation (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    source_id varchar(256) NULL,
    target_id varchar(256) NULL,
    "content" text NULL,
    content_vector public.vector NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT lightrag_vdb_relation_pk PRIMARY KEY (workspace, id)
);
create extension if not exists vector;
create extension if not exists age;

DO $$
    BEGIN
        LOAD 'age';
        SET search_path = public, ag_catalog, "$user";
        select create_graph('dickens');
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END $$;
--rollback;