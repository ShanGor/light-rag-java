--liquibase formatted sql
--changeset Samuel Chan:v1
CREATE TABLE if not exists public.LIGHTRAG_DOC_CHUNKS (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    full_doc_id varchar(256) NULL,
    chunk_order_index int4 NULL,
    tokens int4 NULL,
    "content" text NULL,
    content_vector public.vector NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT LIGHTRAG_DOC_CHUNKS_PK PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.LIGHTRAG_DOC_FULL (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    doc_name varchar(1024) NULL,
    "content" text NULL,
    meta jsonb NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT LIGHTRAG_DOC_FULL_PK PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.LIGHTRAG_DOC_STATUS (
    workspace varchar(255) NOT NULL,
    id varchar(255) NOT NULL,
    content_summary varchar(255) NULL,
    content_length int4 NULL,
    chunks_count int4 NULL,
    status varchar(64) NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT LIGHTRAG_DOC_STATUS_PK PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.LIGHTRAG_LLM_CACHE (
    workspace varchar(255) NOT NULL,
    id varchar(255) NOT NULL,
    "mode" varchar(32) NOT NULL,
    original_prompt text NULL,
    return_value text NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT LIGHTRAG_LLM_CACHE_PK PRIMARY KEY (workspace, mode, id)
);
CREATE TABLE if not exists public.LIGHTRAG_VDB_ENTITY (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    entity_name varchar(255) NULL,
    "content" text NULL,
    content_vector public.vector NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT LIGHTRAG_VDB_ENTITY_PK PRIMARY KEY (workspace, id)
);
CREATE TABLE if not exists public.LIGHTRAG_VDB_RELATION (
    id varchar(255) NOT NULL,
    workspace varchar(255) NOT NULL,
    source_id varchar(256) NULL,
    target_id varchar(256) NULL,
    "content" text NULL,
    content_vector public.vector NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    update_time timestamp NULL,
    CONSTRAINT LIGHTRAG_VDB_RELATION_PK PRIMARY KEY (workspace, id)
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