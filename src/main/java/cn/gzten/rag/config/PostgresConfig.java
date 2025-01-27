package cn.gzten.rag.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;

@Configuration
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PostgresConfig {
    /**
     * Because the default named parameter expander is not compatible with Apache AGE, we need to use a custom one.
     */
    @Bean("noNamedExpanderDbClient")
    public DatabaseClient noNamedExpanderDbClient(ConnectionFactory factory) {
        return DatabaseClient.builder().connectionFactory(factory)
                .namedParameters(false)
                .bindMarkers(BindMarkersFactory.indexed("?", '1'))
                .build();
    }

}
