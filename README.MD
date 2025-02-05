# LightRAG Java Implementation

LightRAG is fabulus which is implemented in Python. This is a Java implementation of LightRAG.

## For Postgres
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
```