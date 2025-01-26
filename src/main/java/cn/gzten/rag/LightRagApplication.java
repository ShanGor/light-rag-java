package cn.gzten.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LightRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(LightRagApplication.class, args);
	}

}
