package cn.gzten.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableCaching
@EnableR2dbcRepositories
public class LightRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(LightRagApplication.class, args);
	}

}
