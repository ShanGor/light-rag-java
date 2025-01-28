package cn.gzten.rag.controller;

import cn.gzten.rag.data.pojo.LlmCache;
import cn.gzten.rag.data.pojo.QueryMode;
import cn.gzten.rag.data.pojo.QueryParam;
import cn.gzten.rag.data.postgres.impl.PGKVForLlmCacheStorage;
import cn.gzten.rag.service.LightRagService;
import cn.gzten.rag.util.LightRagUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {
    private final LightRagService lightRagService;
    private final PGKVForLlmCacheStorage llmCacheStorage;

    @GetMapping("/query")
    public Mono<String> query() {
        var param = new QueryParam();
        param.setMode(QueryMode.HYBRID);
//        return null;
        return lightRagService.query("What are the top themes in this story?", param);
    }

    @GetMapping("/test")
    public LlmCache test() throws InterruptedException {
        var x = llmCacheStorage.getByModeAndId("default", "204a671ce6749432f987fdef9fced8b7");

        return LightRagUtils.monoBlock(x);
    }
}
