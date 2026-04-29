package io.marcus.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Marcus Backend API",
                description = "Marcus signal trading backend API",
                version = "v1",
                contact = @Contact(name = "Marcus Team", email = "dev@marcus.io")
        ),
        servers = @Server(url = "/")
)
public class OpenApiConfig {
}
