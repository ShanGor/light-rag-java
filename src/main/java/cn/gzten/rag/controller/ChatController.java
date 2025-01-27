package cn.gzten.rag.controller;

import cn.gzten.rag.data.pojo.QueryMode;
import cn.gzten.rag.data.pojo.QueryParam;
import cn.gzten.rag.service.LightRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ChatController {
    @Autowired
    private LightRagService lightRagService;

    @GetMapping("/query")
    public String query() {
        var param = new QueryParam();
        param.setMode(QueryMode.HYBRID);
        return null;
//        return lightRagService.query("What are the top themes in this story?", param);
    }
}
