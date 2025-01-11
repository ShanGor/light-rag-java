package cn.gzten.rag.service;

import cn.gzten.rag.data.pojo.*;
import cn.gzten.rag.data.storage.BaseGraphStorage;
import cn.gzten.rag.data.storage.BaseKVStorage;
import cn.gzten.rag.data.storage.BaseVectorStorage;
import cn.gzten.rag.data.storage.DocStatusStorage;
import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.util.LightRagUtils;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.gzten.rag.config.LightRagConfig.PROMPTS;
import static cn.gzten.rag.util.LightRagUtils.pythonTemplateFormat;

@Slf4j
@Service
public class LightRagService {
    @Resource
    private BaseGraphStorage graphStorageService;
    @Resource
    @Qualifier("entityStorage")
    private BaseVectorStorage entityStorageService;
    @Resource
    @Qualifier("relationshipStorage")
    private BaseVectorStorage RelationshipStorageService;
    @Resource
    @Qualifier("vectorForChunksStorage")
    private BaseVectorStorage vectorForChunksStorageService;
    @Resource
    @Qualifier("docFullStorage")
    private BaseKVStorage docFullStorageService;
    @Resource
    @Qualifier("textChunkStorage")
    private BaseKVStorage textChunkStorageService;
    @Resource
    @Qualifier("llmCacheStorage")
    private BaseKVStorage llmCacheStorageService;
    @Resource
    @Qualifier("docStatusStorage")
    private DocStatusStorage docStatusStorageService;

    @Resource
    private LlmCompletionFunc llmCompletionFunc;

    @Value("${rag.addon-params.example-number:1000}")
    private int exampleNumber;
    @Value("${rag.addon-params.language:English}")
    private String language;

    public void insert(String doc) {


    }

    public void query(String query, QueryParam param) {
        switch (param.getMode()) {
            case GLOBAL, LOCAL, HYBRID -> {
                var str = knowledgeGraphQuery(query, param);
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
        Optional<LlmCache> cacheOpt = llmCacheStorageService.getByModeAndId(param.getMode().name(), argsHash);
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

        return "";
    }

    private String buildQueryContext(String lowLevelKeywords, String highLevelKeywords, QueryParam param) {
        switch (param.getMode()) {
            case LOCAL -> {
                // get node data

            }
            case GLOBAL -> {
                // get edge data

            }
            case HYBRID -> {
                // get node data and edge data then merge
            }
        }
        return "";
    }


    public QueryContext getNodeData(String lowLevelKeywords, QueryParam param) {
        var results = entityStorageService.query(lowLevelKeywords, param.getTopK());
        if (results == null || results.isEmpty()) {
            return QueryContext.builder()
                    .textUnitsContext("")
                    .entitiesContext("")
                    .relationsContext("")
                    .build();
        }
        var someNodesAreDamaged = new AtomicBoolean(false);
        var nodeData = new ConcurrentLinkedQueue<Map<String, Object>>();
        results.stream().parallel().forEach(result -> {
            String entityName = (String) result.get("entity_name");
            var node = graphStorageService.getNode(entityName);
            if (node == null || node.isEmpty()) {
                someNodesAreDamaged.set(true);
                return;
            }
            // Get entity degree
            var degree = graphStorageService.nodeDegree(entityName);

            // Compose a new dict for the node data
            var o = new HashMap<>(node);
            o.put("entity_name", entityName);
            o.put("rank", degree);
            nodeData.add(o);
        });

        if (someNodesAreDamaged.get()) {
            log.warn("Some nodes are missing, maybe the storage is damaged");
        }

        // get entity text chunk

        return null;
    }

    public QueryContext getEdgeData(String highLevelKeywords, QueryParam param) {
        return null;
    }

    public void findMostRelatedTextUnitFromEntities(List<Map<String, Object>> nodeData, QueryParam param) {

    }

    /**
     * Split a string by multiple markers
     */
    public List<String> splitStringByMultiMarkers(String content, List<String> markers) {
        if (markers == null || markers.isEmpty()) {
            return List.of(content);
        }
        var escapedMarkers = new ArrayList<String>(markers.size());
        for (var marker : markers) {
            escapedMarkers.add(Pattern.quote(marker));
        }
        var regex = String.join("|", escapedMarkers);
        var results = content.split(regex);
        if (results.length == 0) return List.of(content);

        return Arrays.stream(results).map(String::trim).filter(StringUtils::isNotBlank).toList();
    }
}
