package cn.gzten.rag.service;

import cn.gzten.rag.data.pojo.*;
import cn.gzten.rag.data.storage.*;
import cn.gzten.rag.data.storage.pojo.*;
import cn.gzten.rag.event.MessagePublisher;
import cn.gzten.rag.event.MessageSubscriber;
import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.util.CsvUtil;
import cn.gzten.rag.util.LightRagUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cn.gzten.rag.config.LightRagConfig.GRAPH_FIELD_SEP;
import static cn.gzten.rag.config.LightRagConfig.prompts;
import static cn.gzten.rag.util.LightRagUtils.*;

@Slf4j
@Service
public class LightRagService {
    @Resource
    private BaseGraphStorage graphStorageService;
    @Resource
    @Qualifier("entityStorage")
    private BaseVectorStorage<RagEntity, String> entityStorageService;
    @Resource
    @Qualifier("relationshipStorage")
    private BaseVectorStorage<RagRelation, NullablePair<String, String>> relationshipStorage;
    @Resource
    @Qualifier("vectorForChunksStorage")
    private BaseVectorStorage<RagVectorChunk, RagVectorChunk> vectorForChunksStorageService;
    @Resource
    @Qualifier("docFullStorage")
    private BaseKVStorage<? extends FullDoc> docFullStorageService;
    @Resource
    @Qualifier("textChunkStorage")
    private BaseTextChunkStorage<? extends TextChunk> textChunkStorageService;
    @Resource
    @Qualifier("llmCacheStorage")
    private LlmCacheStorage llmCacheStorageService;
    @Resource
    @Qualifier("docStatusStorage")
    private DocStatusStorage<? extends DocStatusStore> docStatusStorageService;

    @Resource
    private MessagePublisher messagePublisher;

    @Resource
    private LlmCompletionFunc llmCompletionFunc;

    @Value("${rag.addon-params.example-number:1000}")
    private int exampleNumber;
    @Value("${rag.addon-params.language:English}")
    private String language;

    public void insert(String doc) {


    }

    @PostConstruct
    public void init() {
        // handle messages
        new MessageSubscriber(messagePublisher.getMessages(),  msg -> {
            if (msg instanceof LlmCache cache) {
                log.info("Saving llmcache!");
                llmCacheStorageService.upsert(cache).subscribe();
            }
        });
    }

    public Mono<String> query(String query, QueryParam param) {
        switch (param.getMode()) {
            case GLOBAL, LOCAL, HYBRID -> {
                return knowledgeGraphQuery(query, param);
            }
            case NAIVE -> {
                return naiveQuery(query, param);
            }
            default -> {
                log.error("LightRAG does not support mode {}", param.getMode().name());
                return Mono.just(prompts.fail_response);
            }
        }
    }

    private Mono<String> naiveQuery(String query, QueryParam param) {
        return Mono.empty();
    }

    public Mono<String> knowledgeGraphQuery(String query, QueryParam param) {
        switch (param.getMode()) {
            case GLOBAL, LOCAL, HYBRID ->
                log.info("knowledgeGraphQuery starting for mode {}", param.getMode().name());

            default -> {
                log.error("knowledgeGraphQuery not support mode {}", param.getMode().name());
                return Mono.just(prompts.fail_response);
            }
        }

        var argsHash = LightRagUtils.computeMd5(query);
        var cacheOpt = llmCacheStorageService.getByModeAndId(param.getMode().name(), argsHash);
        return cacheOpt.defaultIfEmpty(LlmCache.EMPTY).map(cache -> {
            if (!cache.isEmpty()) return cache.getReturnValue();
            return knowledgeGraphQueryNotFoundLlmCache(query, param, argsHash);
        });
    }

    public String knowledgeGraphQueryNotFoundLlmCache(String query, QueryParam param, String argsHash) {
        List<String> configExamples = prompts.keywords_extraction_examples;
        if (exampleNumber < configExamples.size()) {
            configExamples = configExamples.subList(0, exampleNumber);
        }
        String examples = String.join("\n", configExamples);

        // LLM Generate Keywords
        String kw_prompt_temp = prompts.keywords_extraction;

        var kw_prompt = pythonTemplateFormat(kw_prompt_temp,
                Map.of("query", query, "examples", examples, "language", language)
        );
        var req = new LlmCompletionFunc.LightRagRequest();
        req.setPrompt(kw_prompt);
        // when this is true, expect the return result is GPTKeywordExtractionFormat.class
        req.setKeywordExtraction(true);
        GPTKeywordExtractionFormat result = (GPTKeywordExtractionFormat) llmCompletionFunc.complete(req);
        log.info("kw_prompt result: {}", result);

        // handle keywords missing
        if (result.getHighLevelKeywords().isEmpty() && result.getLowLevelKeywords().isEmpty()) {
            log.warn("low_level_keywords and high_level_keywords is empty");
            return prompts.fail_response;
        } else if (result.getLowLevelKeywords().isEmpty() &&
                (param.getMode() == QueryMode.LOCAL || param.getMode() == QueryMode.HYBRID)) {
            log.warn("low_level_keywords is empty, switching from {} mode to GLOBAL mode", param.getMode().name());
            param.setMode(QueryMode.GLOBAL);
        } else if (result.getHighLevelKeywords().isEmpty() &&
                (param.getMode() == QueryMode.GLOBAL || param.getMode() == QueryMode.HYBRID)) {
            log.warn("high_level_keywords is empty, switching from {} mode to local mode", param.getMode().name());
            param.setMode(QueryMode.LOCAL);
        }

        var lowLevelKeywords = result.getLowLevelKeywords().isEmpty() ? "" : String.join(", ", result.getLowLevelKeywords());
        var highLevelKeywords = result.getHighLevelKeywords().isEmpty() ? "" : String.join(", ", result.getHighLevelKeywords());
        log.info("Using {} mode for query processing", param.getMode().name());

        // Build context
        var context = buildQueryContext(lowLevelKeywords, highLevelKeywords, param);
        if (param.isOnlyNeedContext()) {
            return context;
        }
        if (StringUtils.isBlank(context)) {
            return prompts.fail_response;
        }

        String sys_prompt_temp = prompts.rag_response;
        String sys_prompt = pythonTemplateFormat(sys_prompt_temp,
                Map.of("context_data", context, "response_type", param.getResponseType())
        );
        if (param.isOnlyNeedPrompt()) {
            return sys_prompt;
        }
        var response = llmCompletionFunc.complete(query, List.of(LlmCompletionFunc.CompletionMessage.builder().role("system").content(sys_prompt).build()));
        var strResponse = response.getMessage().getContent();
        if (strResponse.length() > sys_prompt.length()) {
            strResponse = strResponse.replace(sys_prompt, "")
                    .replace("user", "")
                    .replace("model", "")
                    .replace(query, "").trim();
        }

        messagePublisher.publishMessage(LlmCache.builder()
                .id(argsHash)
                .workspace("default")
                .mode(param.getMode().name())
                .returnValue(strResponse)
                .originalPrompt(query).build());

        return strResponse;
    }

    private String buildQueryContext(String lowLevelKeywords, String highLevelKeywords, QueryParam param) {
        QueryContext context;
        switch (param.getMode()) {
            case LOCAL -> {
                // get node data
                context = monoBlock(getNodeData(lowLevelKeywords, param));
            }
            case GLOBAL -> {
                // get edge data
                context = monoBlock(getEdgeData(lowLevelKeywords, param));
            }
            case HYBRID -> {
                // get node data and edge data then merge
                var lowLevelContext = monoBlock(getNodeData(lowLevelKeywords, param));
                log.info("lowLevelContext: {}", lowLevelContext);
                var highLevelContext = monoBlock(getEdgeData(highLevelKeywords, param));
                if (highLevelContext == null || StringUtils.isBlank(highLevelContext.getEntitiesContext())) {
                    log.warn("high_level_keywords is empty, switching from HYBRID mode to LOCAL mode");
                    context = lowLevelContext;
                } else {
                    log.info("highLevelContext: {}", highLevelContext);
                    context = combineContexts(
                            new NullablePair<>(highLevelContext.getEntitiesContext(), lowLevelContext.getEntitiesContext()),
                            new NullablePair<>(highLevelContext.getRelationsContext(), lowLevelContext.getRelationsContext()),
                            new NullablePair<>(highLevelContext.getTextUnitsContext(), lowLevelContext.getTextUnitsContext())
                    );
                    log.info("combineContexts result: {}", context);
                }
            }
            default -> {
                log.error("buildQueryContext not support mode {}", param.getMode().name());
                return prompts.fail_response;
            }
        }
        return """
               -----Entities-----
               ```csv
               %s
               ```
               -----Relationships-----
               ```csv
               %s
               ```
               -----Sources-----
               ```csv
               %s
               ```
               """.formatted(context.getEntitiesContext(), context.getRelationsContext(), context.getTextUnitsContext());
    }


    public Mono<QueryContext> getNodeData(String lowLevelKeywords, QueryParam param) {
        return entityStorageService.query(lowLevelKeywords, param.getTopK()).collectList().flatMap(results -> {
            if (isEmptyCollection(results)) {
                return Mono.just(QueryContext.EMPTY);
            }

            return Flux.fromIterable(results).flatMap(entityName -> {
                var nodeFuture = graphStorageService.getNode(entityName);
                // Get entity degree
                var degreeFuture = graphStorageService.nodeDegree(entityName);

                return nodeFuture.defaultIfEmpty(RagGraphNode.EMPTY).zipWith(degreeFuture).mapNotNull(tuple -> {
                    var node = tuple.getT1();
                    var degree = tuple.getT2();
                    if (node.isEmpty()) {
                        log.warn("Node {} is missing, maybe the storage is damaged", entityName);
                        return RagGraphNodeData.EMPTY;
                    }

                    // Compose a new dict for the node data
                    var o = new RagGraphNodeData(node);
                    o.setEntityName(entityName);
                    o.setRank(degree);
                    return o;
                });
            }).collectList().flatMap((List<RagGraphNodeData> nodeData) -> {

                // get entity text chunk
                var use_text_units_mono = findMostRelatedTextUnitFromEntities(nodeData, param);
                // get relate edges
                var use_relations_mono = findMostRelatedEdgesFromEntities(nodeData, param);

                return use_text_units_mono.zipWith(use_relations_mono).map(pair -> {
                    var use_text_units = pair.getT1();
                    var use_relations = pair.getT2();
                    log.info("Local query uses {} entities, {} relations, {} text units", nodeData.size(),
                            use_relations.size(),
                            use_text_units.size()
                    );

                    // build prompt
                    List<List<Object>> entities_section_list = new LinkedList<>();
                    entities_section_list.add(List.of("id", "entity", "type", "description", "rank"));
                    var i = 0;
                    for(var node : nodeData) {
                        entities_section_list.add(List.of(
                                i,
                                unwrapJsonString(node.getEntityName()),
                                unwrapJsonString(node.getEntityType()),
                                unwrapJsonString(node.getDescription()),
                                node.getRank()
                        ));
                        i++;
                    }
                    var entities_context = CsvUtil.convertToCSV(entities_section_list);

                    List<List<Object>> relations_section_list = new LinkedList<>();
                    relations_section_list.add(List.of("id", "source", "target", "description", "keywords", "weight", "rank"));
                    i = 0;
                    for (var e : use_relations) {
                        NullablePair<String, String> edge = e.getSourceTarget();
                        relations_section_list.add(List.of(
                                i,
                                unwrapJsonString(edge.getLeft()),
                                unwrapJsonString(edge.getRight()),
                                unwrapJsonString(e.getDescription()),
                                unwrapJsonString(e.getKeywords()),
                                e.getWeight(),
                                e.getRank()
                        ));
                        i++;
                    }
                    var relations_context = CsvUtil.convertToCSV(relations_section_list);

                    List<List<Object>> text_units_section_list = new LinkedList<>();
                    text_units_section_list.add(List.of("id", "content"));
                    i = 0;
                    for (var textUnit : use_text_units) {
                        text_units_section_list.add(List.of(
                                i,
                                unwrapJsonString(textUnit.getContent())
                        ));
                        i++;
                    }
                    var text_units_context = CsvUtil.convertToCSV(text_units_section_list);

                    return QueryContext.builder()
                            .textUnitsContext(text_units_context)
                            .relationsContext(relations_context)
                            .entitiesContext(entities_context).build();
                });
            });
        });
    }

    public Mono<QueryContext> getEdgeData(String highLevelKeywords, QueryParam param) {
        return relationshipStorage.query(highLevelKeywords, param.getTopK()).collectList().flatMap(results -> {
            if (isEmptyCollection(results)) {
                return Mono.just(QueryContext.EMPTY);
            }

            return Flux.fromIterable(results).flatMap(result -> {
                String srcId = result.getLeft();
                String tgtId = result.getRight();
                return graphStorageService.getEdge(srcId, tgtId)
                        .defaultIfEmpty(RagGraphEdge.EMPTY)
                        .zipWith(graphStorageService.edgeDegree(srcId, tgtId)).mapNotNull(pair -> {
                            var edge = pair.getT1();
                            var degree = pair.getT2();
                            if (edge.isEmpty()) {
                                log.warn("Some edges are missing, maybe the storage is damaged: {}->{}", srcId, tgtId);
                                return null;
                            }

                            var edgeData = new RagGraphEdgeData(edge);
                            edgeData.setRank(degree);
                            edgeData.setSourceTarget(new NullablePair<>(srcId, tgtId));
                            return edgeData;
                });
            }).collectList().flatMap(rawEdges -> {
                // Equivalent python sorted(edge_datas, key=lambda x: (x["rank"], x["weight"]), reverse=True)
                rawEdges.sort((o1, o2) -> {
                    int rank1 = o1.getRank();
                    int rank2 = o2.getRank();
                    if (rank1 == rank2) {
                        double weight1 = o1.getWeight();
                        double weight2 = o2.getWeight();
                        return Double.compare(weight2, weight1);
                    } else {
                        return Integer.compare(rank2, rank1);
                    }
                });
                var edges = truncateListByTokenSize(rawEdges, RagGraphEdge::getDescription, param.getMaxTokenForGlobalContext());

                var mono_use_entities = findMostRelatedEntitiesFromRelationships(edges, param);
                var mono_use_text_units = findRelatedTextUnitFromRelationships(edges, param);
                return mono_use_entities.zipWith(mono_use_text_units).map(tuple -> {
                    var use_entities = tuple.getT1();
                    var use_text_units = tuple.getT2();
                    log.info("Global query uses {} entities, {} relations, {} text units",
                            use_entities.size(), edges.size(), use_text_units.size());
                    var relations_section_list = new LinkedList<List<Object>>();
                    relations_section_list.add(List.of("id", "source", "target", "description", "keywords", "weight", "rank"));
                    int i=0;
                    for (var edge : edges) {
                        i++;
                        relations_section_list.add(List.of(i,
                                unwrapJsonString(edge.getSourceTarget().getLeft()),
                                unwrapJsonString(edge.getSourceTarget().getRight()),
                                unwrapJsonString(edge.getDescription()),
                                unwrapJsonString(edge.getKeywords()),
                                edge.getWeight(), edge.getRank()));
                    }
                    // relations_context = list_of_list_to_csv(relations_section_list)
                    var relations_context = CsvUtil.convertToCSV(relations_section_list);

                    var entities_section_list = new LinkedList<List<Object>>();
                    entities_section_list.add(List.of("id", "entity", "type", "description", "rank"));
                    i=0;
                    for (var node : use_entities) {
                        i++;
                        entities_section_list.add(List.of(i,
                                unwrapJsonString(node.getEntityName()),
                                unwrapJsonString(node.getEntityType()),
                                unwrapJsonString(node.getDescription()),
                                node.getRank()));
                    }
                    var entities_context = CsvUtil.convertToCSV(entities_section_list);
                    var text_units_section_list = new LinkedList<List<Object>>();
                    text_units_section_list.add(List.of("id", "content"));
                    i=0;
                    for (var textUnit : use_text_units) {
                        i++;
                        text_units_section_list.add(List.of(i, unwrapJsonString(textUnit.getContent())));
                    }
                    var text_units_context = CsvUtil.convertToCSV(text_units_section_list);

                    return QueryContext.builder()
                            .relationsContext(relations_context)
                            .textUnitsContext(text_units_context)
                            .entitiesContext(entities_context).build();
                });
            });
        });
    }

    private Mono<Pair<Map<String, List<String>>, List<NullablePair<List<String>, List<NullablePair<String, String>>>>>>
    findNodeEdges(Collection<RagGraphNodeData> nodeData) {
        var all_one_hop_text_units_lookup = new ConcurrentHashMap<String, List<String>>();
        List<NullablePair<List<String>, List<NullablePair<String, String>>>> textUnitsAndEdges = new CopyOnWriteArrayList<>();

        var allOneHopNodes = new ConcurrentHashMap<String, RagGraphNode>();

        return Flux.fromIterable(nodeData).filter(node -> !node.isEmpty()).flatMap(node -> {
            var textUnits = splitStringByMultiMarkers(node.getSourceId(), List.of(GRAPH_FIELD_SEP));

            return graphStorageService.getNodeEdges(node.getEntityName()).collectList().flatMap(edges -> {
                textUnitsAndEdges.add(new NullablePair<>(textUnits, edges));
                if (!isEmptyCollection(edges)) {
                    return Flux.fromIterable(edges).flatMap(edge -> {
                        if (allOneHopNodes.containsKey(edge.getRight())) return Mono.empty();

                        return graphStorageService.getNode(edge.getRight()).flatMap(nodeDetail -> {
                            allOneHopNodes.put(edge.getRight(), nodeDetail);
                            all_one_hop_text_units_lookup.put(edge.getRight(), textUnits);
                            return Mono.empty();
                        });
                    }).collectList();
                }
                return Mono.empty();
            });
        }).collectList().thenReturn(Pair.of(all_one_hop_text_units_lookup, textUnitsAndEdges));
    }

    public Mono<List<TextChunk>> findMostRelatedTextUnitFromEntities(Collection<RagGraphNodeData> nodeData, QueryParam param) {
        var all_text_units_lookup = new ConcurrentHashMap<String, TextUnit>();
        var nodeEdges = findNodeEdges(nodeData);
        return nodeEdges.map(triple -> {
            var all_one_hop_text_units_lookup = triple.getLeft();
            var textUnitsAndEdges = triple.getRight();

            for (int idx=0; idx < textUnitsAndEdges.size(); idx++) {
                NullablePair<List<String>, List<NullablePair<String, String>>> textUnitAndEdge = textUnitsAndEdges.get(idx);
                var textUnit = textUnitAndEdge.getLeft();
                var edges = textUnitAndEdge.getRight();
                for (var c_id : textUnit) {
                    if (!all_text_units_lookup.containsKey(c_id)) {
                        var chunk = monoBlock(textChunkStorageService.getById(c_id));
                        if (chunk == null) continue;
                        var o = TextUnit.builder().data(chunk).order(idx).relationCounts(0).build();
                        all_text_units_lookup.put(c_id, o);
                    }
                    if (!isEmptyCollection(edges)) {
                        for (var edge : edges) {
                            if (all_one_hop_text_units_lookup.containsKey(edge.getRight())) {
                                var o = all_one_hop_text_units_lookup.get(edge.getRight());
                                if (o.contains(c_id)) {
                                    all_text_units_lookup.get(c_id).increaseRelationCounts();
                                }
                            }
                        }
                    }
                }
            }

            List<TextUnit> all_text_units = new LinkedList<>();
            for (var entry : all_text_units_lookup.entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue();
                if (v == null) continue;
                var data = v.getData();
                if (StringUtils.isBlank(data.getContent())) continue;
                v.setId(k);
                all_text_units.add(v);
            }

            if (all_text_units.isEmpty()) {
                log.warn("No valid text units found");
            }

                    // Sort by order asc, relationCounts desc
            all_text_units.sort((o1, o2) -> {
                var order1 = o1.getOrder();
                var relationCounts1 = o1.getRelationCounts();
                var order2 = o2.getOrder();
                var relationCounts2 = o2.getRelationCounts();
                if (order1 == order2) {
                    return Integer.compare(relationCounts2, relationCounts1);
                }
                return Integer.compare(order1, order2);
            });

            all_text_units = truncateListByTokenSize(all_text_units,
                    (v) -> {
                var data = v.getData();
                return data.getContent();
                }, param.getMaxTokenForTextUnit()
            );

            return all_text_units.stream().map(TextUnit::getData).toList();
        });
    }

    public Mono<List<RagGraphEdgeData>> findMostRelatedEdgesFromEntities(Collection<RagGraphNodeData> nodeData, QueryParam param) {
        var seen = new HashSet<>();
        List<RagGraphEdgeData> allEdges = new ArrayList<>();
        return Flux.fromIterable(nodeData).map(dp ->
                graphStorageService.getNodeEdges(dp.getEntityName()).map(edge -> {
                    NullablePair<String, String> sortedEdge = edge.sorted();
                    if (seen.contains(sortedEdge)) return Mono.empty();

                    seen.add(sortedEdge);

                    //TODO: maybe should not use the sorted edge to get edge details and degree.
                    var m1 = graphStorageService.getEdge(sortedEdge.getLeft(), sortedEdge.getRight());
                    var m2 = graphStorageService.edgeDegree(sortedEdge.getLeft(), sortedEdge.getRight());
                    return m1.zipWith(m2).flatMap(pair -> {
                        var edgeDetail = pair.getT1();
                        var degree = pair.getT2();
                        var o = new RagGraphEdgeData(edgeDetail);
                        o.setSourceTarget(sortedEdge);
                        o.setRank(degree);

                        allEdges.add(o);
                        return Mono.empty();
                    });
        })).collectList().map(ignore -> {
            // Sort by rank and weight, all descending.
            allEdges.sort((o1, o2) -> {
                var rank1 = o1.getRank();
                var rank2 = o2.getRank();
                var weight1 = o1.getWeight();
                var weight2 = o1.getWeight();
                if (rank1 == rank2) {
                    return Double.compare(weight2, weight1);
                } else {
                    return Integer.compare(rank2, rank1);
                }
            });
            return truncateListByTokenSize(allEdges,
                    RagGraphEdge::getDescription,
                    param.getMaxTokenForGlobalContext()
            );
        });
    }

    private QueryContext combineContexts(NullablePair<String, String> entities, NullablePair<String, String> relationships, NullablePair<String, String> sources) {
        // Function to extract entities, relationships, and sources from context strings
        var hl_entities = entities.getLeft();
        var ll_entities = entities.getRight();
        var hl_relationships = relationships.getLeft();
        var ll_relationships = relationships.getRight();
        var hl_sources = sources.getLeft();
        var ll_sources = sources.getRight();

        // Combine and deduplicate the entities
        var combined_entities = processCombineContexts(hl_entities, ll_entities);

        // Combine and deduplicate the relationships
        var combined_relationships = processCombineContexts(hl_relationships, ll_relationships);

        // Combine and deduplicate the sources
        var combined_sources = processCombineContexts(hl_sources, ll_sources);

        return QueryContext.builder()
                .entitiesContext(combined_entities)
                .relationsContext(combined_relationships)
                .textUnitsContext(combined_sources).build();
    }

    private Mono<RagGraphNodeData> enrichNodeData(String entityName) {
        return graphStorageService.getNode(entityName).defaultIfEmpty(RagGraphNode.EMPTY)
                .zipWith(graphStorageService.nodeDegree(entityName)).mapNotNull(pair -> {
            var node = pair.getT1();
            if (node.isEmpty()) return null;

            var degree = pair.getT2();
            var nodeData = new RagGraphNodeData(node);
            nodeData.setEntityName(entityName);
            nodeData.setRank(degree);
            return nodeData;
        });
    }

    public Mono<List<RagGraphNodeData>> findMostRelatedEntitiesFromRelationships(List<RagGraphEdgeData> edgeData, QueryParam param) {
        var seen = new ConcurrentSkipListSet<String>();

        return Flux.fromIterable(edgeData).flatMap(edge -> {
            var source = edge.getSourceTarget().getLeft();
            var target = edge.getSourceTarget().getRight();
            if(!seen.contains(source)) {
                seen.add(source);
                return enrichNodeData(source);
            }
            if(!seen.contains(target)) {
                seen.add(target);
                return enrichNodeData(target);
            }
            return Mono.empty();
        }).collectList().map(nodeDatas -> truncateListByTokenSize(nodeDatas,
                RagGraphNodeData::getDescription,
                param.getMaxTokenForLocalContext()));
    }

    public Mono<List<TextChunk>> findRelatedTextUnitFromRelationships(List<RagGraphEdgeData> edgeData, QueryParam param) {
        var count = new AtomicInteger(0);

        //TODO do parallel lookup to improve the performance.
        return Flux.fromIterable(edgeData).flatMap(edge -> {
            var source = edge.getSourceId();
            var textUnits = splitStringByMultiMarkers(source, List.of(GRAPH_FIELD_SEP));

            return Flux.fromIterable(textUnits).flatMap(textUnit -> {

                var index = count.getAndIncrement();
                return textChunkStorageService.getById(textUnit).mapNotNull(textUnitData -> {
                    if (StringUtils.isBlank(textUnitData.getContent())) {
                        return null;
                    }

                    var unit = TextUnit.builder()
                            .id(textUnit)
                            .data(textUnitData)
                            .order(index).build();
                    return Map.entry(textUnit, unit);
                });
            });
        }).collectList().map((List<Map.Entry<String, TextUnit>> list) -> {
            if (list.isEmpty()) {
                log.warn("No valid text chunks found or after filtering");
                return List.of();
            }
            var all_text_units_lookup = new HashMap<String, TextUnit>();
            for (var entry : list) {
                var textUnit = entry.getKey();
                var unit = entry.getValue();
                all_text_units_lookup.put(textUnit, unit);
            }

            List<TextUnit> all_text_units = new LinkedList<>(all_text_units_lookup.values());
            all_text_units.sort(Comparator.comparingInt(TextUnit::getOrder));
            all_text_units = truncateListByTokenSize(all_text_units, o -> o.getData().getContent(), param.getMaxTokenForTextUnit());

            return all_text_units.stream().map(TextUnit::getData).collect(Collectors.toList());
        });

    }
}
