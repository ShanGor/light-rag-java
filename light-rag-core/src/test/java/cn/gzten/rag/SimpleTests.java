package cn.gzten.rag;

import cn.gzten.rag.data.pojo.QueryMode;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class SimpleTests {
    @Test
    void testString() {
        var str = """
                Hello
                World!
                """;
        System.out.println(str);

        str = "hello world";
        var ss = str.split(",");
        System.out.println(ss.length);
    }

    @Test
    void testMono() throws InterruptedException {
        var o = Mono.empty();
        o.or(null).map(x -> {
           System.out.println("o value is: " + x);
           return x;
        }).subscribe();

        var o1 = Mono.just("hello");
        o1.map(x -> {
            System.out.println("o1 value is: " + x);
            return x;
        }).subscribe();

        var l = Flux.empty();
        l.collectList().subscribe(list -> {
            System.out.println("list size is: " + list.size());
        });

        Mono<String> os = Mono.empty();
        var osv = os.block();
        System.out.println("osv is: " + osv);

        Thread.sleep(1000);


        System.out.println("Done");
    }

    @Test
    void testString2() {
        System.out.println(QueryMode.GLOBAL.name());
    }
}
