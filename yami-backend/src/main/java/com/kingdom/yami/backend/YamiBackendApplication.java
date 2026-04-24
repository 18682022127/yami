package com.kingdom.yami.backend;

import com.kingdom.yami.yamicommon.YamiConnon;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.kingdom.yami.backend.common.config")
public class YamiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(YamiBackendApplication.class, args);
	}

}
