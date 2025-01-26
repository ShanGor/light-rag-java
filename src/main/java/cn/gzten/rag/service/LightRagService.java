package cn.gzten.rag.service;

import cn.gzten.rag.data.pojo.*;
import cn.gzten.rag.data.storage.*;
import cn.gzten.rag.data.storage.pojo.*;
import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.util.CsvUtil;
import cn.gzten.rag.util.LightRagUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cn.gzten.rag.config.LightRagConfig.GRAPH_FIELD_SEP;
import static cn.gzten.rag.config.LightRagConfig.PROMPTS;
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
    private LlmCompletionFunc llmCompletionFunc;

    @Value("${rag.addon-params.example-number:1000}")
    private int exampleNumber;
    @Value("${rag.addon-params.language:English}")
    private String language;

    public void insert(String doc) {


    }

    public String query(String query, QueryParam param) {
        switch (param.getMode()) {
            case GLOBAL, LOCAL, HYBRID -> {
                return knowledgeGraphQuery(query, param);
            }
            default -> {
                log.error("knowledgeGraphQuery not support mode {}", param.getMode().name());
                return (String) PROMPTS.get("fail_response");
            }
        }
    }

    public String knowledgeGraphQuery(String query, QueryParam param) {
        switch (param.getMode()) {
            case GLOBAL, LOCAL, HYBRID ->
                log.info("knowledgeGraphQuery starting for mode {}", param.getMode().name());

            default -> {
                log.error("knowledgeGraphQuery not support mode {}", param.getMode().name());
                return (String) PROMPTS.get("fail_response");
            }
        }

        var argsHash = LightRagUtils.computeMd5(query);
        var cacheOpt = llmCacheStorageService.getByModeAndId(param.getMode().name(), argsHash);
        if (cacheOpt.isPresent()) {
            var cache = cacheOpt.get();
            return cache.getReturnValue();
        }

        List<String> configExamples = (List<String>) PROMPTS.get("keywords_extraction_examples");
        if (exampleNumber < configExamples.size()) {
            configExamples = configExamples.subList(0, exampleNumber);
        }
        String examples = String.join("\n", configExamples);

        // LLM Generate Keywords
        String kw_prompt_temp = (String) PROMPTS.get("keywords_extraction");

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
            return (String) PROMPTS.get("fail_response");
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
            return (String) PROMPTS.get("fail_response");
        }

        String sys_prompt_temp = (String) PROMPTS.get("rag_response");
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

        llmCacheStorageService.upsert(LlmCache.builder()
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
                context = getNodeData(lowLevelKeywords, param);

            }
            case GLOBAL -> {
                // get edge data
                context = getEdgeData(lowLevelKeywords, param);

            }
            case HYBRID -> {
                // get node data and edge data then merge
                var lowLevelContext = getNodeData(lowLevelKeywords, param);
                log.info("lowLevelContext: {}", lowLevelContext);
                var highLevelContext = getEdgeData(highLevelKeywords, param);
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
                return (String) PROMPTS.get("fail_response");
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


    public QueryContext getNodeData(String lowLevelKeywords, QueryParam param) {
        var results = entityStorageService.query(lowLevelKeywords, param.getTopK());
        if (isEmptyCollection(results)) {
            return QueryContext.builder()
                    .textUnitsContext("")
                    .entitiesContext("")
                    .relationsContext("")
                    .build();
        }
        var someNodesAreDamaged = new AtomicBoolean(false);
        var nodeData = new ConcurrentLinkedQueue<RagGraphNodeData>();
        results.stream().parallel().forEach(entityName -> {
            var node = graphStorageService.getNode(entityName);
            if (node == null) {
                someNodesAreDamaged.set(true);
                return;
            }
            // Get entity degree
            var degree = graphStorageService.nodeDegree(entityName);

            // Compose a new dict for the node data
            var o = new RagGraphNodeData(node);
            o.setEntityName(entityName);
            o.setRank(degree);
            nodeData.add(o);
        });

        if (someNodesAreDamaged.get()) {
            log.warn("Some nodes are missing, maybe the storage is damaged");
        }

        // get entity text chunk
        var use_text_units = findMostRelatedTextUnitFromEntities(nodeData, param);

        // get relate edges
        var use_relations = findMostRelatedEdgesFromEntities(nodeData, param);

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
                    node.getEntityName(),
                    node.getEntityType(),
                    node.getDescription(),
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
                    edge.getLeft(),
                    edge.getRight(),
                    e.getDescription(),
                    e.getKeywords(),
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
                    textUnit.getContent()
            ));
            i++;
        }
        var text_units_context = CsvUtil.convertToCSV(text_units_section_list);

        return QueryContext.builder()
                .textUnitsContext(text_units_context)
                .relationsContext(relations_context)
                .entitiesContext(entities_context).build();
    }

    public QueryContext getEdgeData(String highLevelKeywords, QueryParam param) {
        var results = relationshipStorage.query(highLevelKeywords, param.getTopK());
        if (isEmptyCollection(results)) {
            return QueryContext.builder()
                    .textUnitsContext("")
                    .relationsContext("")
                    .entitiesContext("")
                    .build();
        }
        List<RagGraphEdgeData> edges = new LinkedList<>();
        for (var result : results) {
            String srcId = result.getLeft();
            String tgtId = result.getRight();
            var edge = graphStorageService.getEdge(srcId, tgtId);
            if (edge == null) {
                log.warn("Some edges are missing, maybe the storage is damaged: {}->{}", srcId, tgtId);
                continue;
            }
            var degree = graphStorageService.edgeDegree(srcId, tgtId);

            var edgeData = new RagGraphEdgeData(edge);
            edgeData.setRank(degree);
            edgeData.setSourceTarget(new NullablePair<>(srcId, tgtId));
            edges.add(edgeData);
        }
        // Equivalent python sorted(edge_datas, key=lambda x: (x["rank"], x["weight"]), reverse=True)
        edges.sort((o1, o2) -> {
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
        edges = truncateListByTokenSize(edges, RagGraphEdge::getDescription, param.getMaxTokenForGlobalContext());

        var use_entities = findMostRelatedEntitiesFromRelationships(edges, param);
        var use_text_units = findRelatedTextUnitFromRelationships(edges, param);
        log.info("Global query uses {} entities, {} relations, {} text units",
                use_entities.size(), edges.size(), use_text_units.size());
        var relations_section_list = new LinkedList<List<Object>>();
        relations_section_list.add(List.of("id", "source", "target", "description", "keywords", "weight", "rank"));
        int i=0;
        for (var edge : edges) {
            i++;
            relations_section_list.add(List.of(i,
                    edge.getSourceTarget().getLeft(),
                    edge.getSourceTarget().getRight(),
                    edge.getDescription(), edge.getKeywords(), edge.getWeight(), edge.getRank()));
        }
        // relations_context = list_of_list_to_csv(relations_section_list)
        var relations_context = CsvUtil.convertToCSV(relations_section_list);

        var entities_section_list = new LinkedList<List<Object>>();
        entities_section_list.add(List.of("id", "entity", "type", "description", "rank"));
        i=0;
        for (var node : use_entities) {
            i++;
            entities_section_list.add(List.of(i,
                    node.getEntityName(), node.getEntityType(), node.getDescription(), node.getRank()));
        }
        var entities_context = CsvUtil.convertToCSV(entities_section_list);
        var text_units_section_list = new LinkedList<List<Object>>();
        text_units_section_list.add(List.of("id", "content"));
        i=0;
        for (var textUnit : use_text_units) {
            i++;
            text_units_section_list.add(List.of(i, textUnit.getContent()));
        }
        var text_units_context = CsvUtil.convertToCSV(text_units_section_list);

        return QueryContext.builder()
                .relationsContext(relations_context)
                .textUnitsContext(text_units_context)
                .entitiesContext(entities_context).build();
    }

    public List<TextChunk> findMostRelatedTextUnitFromEntities(Collection<RagGraphNodeData> nodeData, QueryParam param) {
        var all_one_hop_text_units_lookup = new HashMap<String, List<String>>();
        var allOneHopNodes = new HashMap<>();
        var all_text_units_lookup = new HashMap<String, TextUnit>();
        List<NullablePair<List<String>, List<NullablePair<String, String>>>> textUnitsAndEdges = new ArrayList<>();
        for (var node : nodeData) {
            var textUnits = splitStringByMultiMarkers(node.getSourceId(), List.of(GRAPH_FIELD_SEP));

            var edges = graphStorageService.getNodeEdges(node.getEntityName());
            textUnitsAndEdges.add(new NullablePair<>(textUnits, edges));

            if (!isEmptyCollection(edges)) {
                for (var edge : edges) {
                    if (allOneHopNodes.containsKey(edge.getRight())) continue;

                    var nodeDetail = graphStorageService.getNode(edge.getRight());
                    allOneHopNodes.put(edge.getRight(), nodeDetail);
                    all_one_hop_text_units_lookup.put(edge.getRight(),textUnits);
                }
            }

        }


        for (int idx=0; idx < textUnitsAndEdges.size(); idx++) {
            NullablePair<List<String>, List<NullablePair<String, String>>> textUnitAndEdge = textUnitsAndEdges.get(idx);
            var textUnit = textUnitAndEdge.getLeft();
            var edges = textUnitAndEdge.getRight();
            for (var c_id : textUnit) {
                if (!all_text_units_lookup.containsKey(c_id)) {
                    var chunk = textChunkStorageService.getById(c_id);
                    if (chunk.isEmpty()) continue;
                    var o = TextUnit.builder().data(chunk.get()).order(idx).relationCounts(0).build();
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
                },
                param.getMaxTokenForTextUnit()
        );

        return all_text_units.stream().map(TextUnit::getData).toList();
    }

    public List<RagGraphEdgeData> findMostRelatedEdgesFromEntities(Collection<RagGraphNodeData> nodeData, QueryParam param) {
        var seen = new HashSet<>();
        List<RagGraphEdgeData> allEdges = new ArrayList<>();
        for (var dp : nodeData) {
            var edges = graphStorageService.getNodeEdges(dp.getEntityName());
            for (var edge : edges) {
                NullablePair<String, String> sortedEdge = edge.sorted();
                if (!seen.contains(sortedEdge)) {
                    seen.add(sortedEdge);

                    //TODO: maybe should not use the sorted edge to get edge details and degree.
                    var edgeDetail = graphStorageService.getEdge(sortedEdge.getLeft(), sortedEdge.getRight());

                    var o = new RagGraphEdgeData(edgeDetail);
                    var degree = graphStorageService.edgeDegree(sortedEdge.getLeft(), sortedEdge.getRight());
                    o.setSourceTarget(sortedEdge);
                    o.setRank(degree);

                    allEdges.add(o);
                }
            }
        }
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

    private void enrichNodeData(String entityName, List<RagGraphNodeData> nodeDatas) {
        var node = graphStorageService.getNode(entityName);
        var degree = graphStorageService.nodeDegree(entityName);
        var nodeData = new RagGraphNodeData(node);
        nodeData.setEntityName(entityName);
        nodeData.setRank(degree);
        nodeDatas.add(nodeData);
    }

    public List<RagGraphNodeData> findMostRelatedEntitiesFromRelationships(List<RagGraphEdgeData> edgeData, QueryParam param) {
        var seen = new HashSet<String>();

        List<RagGraphNodeData> nodeDatas = new LinkedList<>();
        for(var edge : edgeData) {
            var source = edge.getSourceTarget().getLeft();
            var target = edge.getSourceTarget().getRight();
            if(!seen.contains(source)) {
                seen.add(source);
                enrichNodeData(source, nodeDatas);
            }
            if(!seen.contains(target)) {
                seen.add(target);
                enrichNodeData(target, nodeDatas);
            }
        }
        return truncateListByTokenSize(nodeDatas,
                RagGraphNodeData::getDescription,
                param.getMaxTokenForLocalContext());
    }

    public List<TextChunk> findRelatedTextUnitFromRelationships(List<RagGraphEdgeData> edgeData, QueryParam param) {
        var count = new AtomicInteger(0);
        var all_text_units_lookup = new HashMap<String, TextUnit>();
        for (var edge : edgeData) {
            var source = edge.getSourceId();
            var textUnits = splitStringByMultiMarkers(source, List.of(GRAPH_FIELD_SEP));

            for (var textUnit : textUnits) {
                var index = count.getAndIncrement();
                var textUnitData = textChunkStorageService.getById(textUnit);
                if (textUnitData.isEmpty()) {
                    continue;
                }
                var data = textUnitData.get();
                if (StringUtils.isBlank(data.getContent())) {
                    continue;
                }

                var unit = TextUnit.builder()
                        .id(textUnit)
                        .data(data)
                        .order(index).build();
                all_text_units_lookup.put(textUnit, unit);
            }
        }
        if (all_text_units_lookup.isEmpty()) {
            log.warn("No valid text chunks found or after filtering");
            return List.of();
        }

        List<TextUnit> all_text_units = new LinkedList<>(all_text_units_lookup.values());
        all_text_units.sort(Comparator.comparingInt(TextUnit::getOrder));
        all_text_units = truncateListByTokenSize(all_text_units, o -> o.getData().getContent(), param.getMaxTokenForTextUnit());

        return all_text_units.stream().map(TextUnit::getData).collect(Collectors.toList());
    }
}
