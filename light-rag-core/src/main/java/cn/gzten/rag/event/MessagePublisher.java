package cn.gzten.rag.event;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class MessagePublisher {
    private final AtomicReference<FluxSink<Object>> sinkReference = new AtomicReference<>();
    private final Flux<Object> flux;

    public MessagePublisher() {
        this.flux = Flux.create(sinkReference::set, FluxSink.OverflowStrategy.BUFFER);
    }

    public void publishMessage(Object message) {
        FluxSink<Object> sink = sinkReference.get();
        if (sink != null) {
            sink.next(message);
        }
    }

    public Flux<Object> getMessages() {
        return flux;
    }
}