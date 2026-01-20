package com.temperature.proxy.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Temperature Proxy API")
                        .version("1.0.0")
                        .description("REST API proxy for fetching current temperature from Open-Meteo. "
                                + "Returns normalized weather data including temperature and wind speed.")
                        .contact(new Contact().name("API Support")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local development server")));
    }
}
