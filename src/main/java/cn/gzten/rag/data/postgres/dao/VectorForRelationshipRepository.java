package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface VectorForRelationshipRepository extends ReactiveCrudRepository<VectorForRelationshipEntity, String> {

    @Modifying
    @Query(value = """
        INSERT INTO lightrag_vdb_relation (workspace, id, source_id,
         target_id, content, content_vector)
         VALUES (:ws, :id, :sid, :tid, :ct, :cv\\:\\:vector)
         ON CONFLICT (workspace,id) DO UPDATE
         SET source_id=EXCLUDED.source_id,
         target_id=EXCLUDED.target_id,
         content=EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         update_time = CURRENT_TIMESTAMP""")
    Mono<Void> upsert(@Param("ws") String workspace,
                      @Param("id") String id,
                      @Param("sid") String sourceId,
                      @Param("tid") String targetId,
                      @Param("ct") String content,
                      @Param("cv") String contentVector);

    @Modifying
    @Query(value = """
        INSERT INTO lightrag_vdb_relation (workspace, id, source_id,
         target_id, content, content_vector, graph_properties)
         VALUES (:ws, :id, :sid, :tid, :ct, :cv\\:\\:, :gp)
         ON CONFLICT (workspace,id) DO UPDATE
         SET source_id=EXCLUDED.source_id,
         target_id=EXCLUDED.target_id,
         content=EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         graph_properties=EXCLUDED.graph_properties,
         update_time = CURRENT_TIMESTAMP""")
    Mono<Void> upsert(@Param("ws") String workspace,
                      @Param("id") String id,
                      @Param("sid") String sourceId,
                      @Param("tid") String targetId,
                      @Param("ct") String content,
                      @Param("cv") String contentVector,
                      @Param("gp") String graphProperties);

    @Query(value = """
        SELECT source_id, target_id FROM
         (SELECT id, source_id,target_id, 1 - (content_vector <=> :embedding\\:\\:vector) as distance
         FROM lightrag_vdb_relation where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""")
    Flux<VectorForRelationshipQueryResult> query(@Param("ws") String workspace,
                                                 @Param("distance") float distance,
                                                 @Param("embedding") String embedding,
                                                 @Param("tk") int topK);

}
