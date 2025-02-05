# LightRAG Java Implementation

LightRAG is fabulus which is implemented in Python. This is a Java implementation of LightRAG.

## Requirement
- Java 21+. (You may download from https://www.azul.com/downloads/?package=jdk#zulu) 

## For Postgres
[PGVector](https://github.com/pgvector/pgvector) and Apache AGE should be used.  
Windows [Release](https://github.com/ShanGor/apache-age-windows/releases/tag/PG17%2Fv1.5.0-rc0) as it is easy to install for Linux/Mac.  
If you prefer docker, please start with this image if you are a beginner to avoid hiccups (DO read the overview): https://hub.docker.com/r/shangor/postgres-for-rag
### Create index for AGE:
```sql
load 'age';
SET search_path = ag_catalog, "$user", public;
CREATE INDEX CONCURRENTLY entity_p_idx ON dickens."Entity" (id);
CREATE INDEX CONCURRENTLY vertex_p_idx ON dickens."_ag_label_vertex" (id);
CREATE INDEX CONCURRENTLY directed_p_idx ON dickens."DIRECTED" (id);
CREATE INDEX CONCURRENTLY directed_eid_idx ON dickens."DIRECTED" (end_id);
CREATE INDEX CONCURRENTLY directed_sid_idx ON dickens."DIRECTED" (start_id);
CREATE INDEX CONCURRENTLY directed_seid_idx ON dickens."DIRECTED" (start_id,end_id);
CREATE INDEX CONCURRENTLY edge_p_idx ON dickens."_ag_label_edge" (id);
CREATE INDEX CONCURRENTLY edge_sid_idx ON dickens."_ag_label_edge" (start_id);
CREATE INDEX CONCURRENTLY edge_eid_idx ON dickens."_ag_label_edge" (end_id);
CREATE INDEX CONCURRENTLY edge_seid_idx ON dickens."_ag_label_edge" (start_id,end_id);
create INDEX CONCURRENTLY vertex_idx_node_id ON dickens."_ag_label_vertex" (ag_catalog.agtype_access_operator(properties, '"node_id"'::agtype));
create INDEX CONCURRENTLY entity_idx_node_id ON dickens."Entity" (ag_catalog.agtype_access_operator(properties, '"node_id"'::agtype));
CREATE INDEX CONCURRENTLY entity_node_id_gin_idx ON dickens."Entity" using gin(properties);
ALTER TABLE dickens."DIRECTED" CLUSTER ON directed_sid_idx;

-- drop if necessary
drop INDEX entity_p_idx;
drop INDEX vertex_p_idx;
drop INDEX directed_p_idx;
drop INDEX directed_eid_idx;
drop INDEX directed_sid_idx;
drop INDEX directed_seid_idx;
drop INDEX edge_p_idx;
drop INDEX edge_sid_idx;
drop INDEX edge_eid_idx;
drop INDEX edge_seid_idx;
drop INDEX vertex_idx_node_id;
drop INDEX entity_idx_node_id;
drop INDEX entity_node_id_gin_idx;
```
### Create index for pgvector:
You need to know your vector dimension for your embedding model. 
For example, the dimension of `nomic-embed-text` is `768`, `bge-m3` is `1024`.
```sql
-- vector indexes, you have to define dimension before building indexes
CREATE INDEX ON public.lightrag_vdb_entity USING hnsw (content_vector vector_cosine_ops);
CREATE INDEX ON public.lightrag_vdb_relation USING hnsw (content_vector vector_cosine_ops);
CREATE INDEX ON public.lightrag_doc_chunks USING hnsw (content_vector vector_cosine_ops);
-- drop them if necessary (for example, change dimension)
drop index public.lightrag_vdb_entity_content_vector_idx;
drop index public.lightrag_vdb_relation_content_vector_idx;
drop index public.lightrag_doc_chunks_content_vector_idx;
```
#### How to define / change the dimension?
By default, there is no dimension. and you cannot create index. When you have data, and you want to define dimension and build index, you can simply run below SQLs;
```sql
-- Change the 1024 to fit your dimension.
ALTER TABLE lightrag_vdb_entity ALTER COLUMN content_vector TYPE vector(1024);
ALTER TABLE lightrag_vdb_relation ALTER COLUMN content_vector TYPE vector(1024);
ALTER TABLE lightrag_doc_chunks ALTER COLUMN content_vector TYPE vector(1024);
```
If you want to change the dimension, and you've got data, you need to remove all the existing vector data, and then you can change the dimension.
- Do the following SQLs
  ```sql
  -- If your data is already has the same dimension, you can just change the dimension without data cleanup.
  update lightrag_vdb_entity set content_vector= null; commit;
  update lightrag_vdb_relation set content_vector= null; commit;
  update lightrag_doc_chunks set content_vector= null; commit;

  ALTER TABLE lightrag_vdb_entity ALTER COLUMN content_vector TYPE vector(1024);
  ALTER TABLE lightrag_vdb_relation ALTER COLUMN content_vector TYPE vector(1024);
  ALTER TABLE lightrag_doc_chunks ALTER COLUMN content_vector TYPE vector(1024);
  ```
-  Call the API to re-embed all the data.
  > curl -i http[s]://{server}:{port}/vector/update