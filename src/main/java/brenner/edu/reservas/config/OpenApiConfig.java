package brenner.edu.reservas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI reservasOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Reservas de Salas")
                .version("v1")
                .description("""
                        Mini projeto didático: UseCase + Hexagonal (Ports & Adapters) + DDD + Result + Command.
                        Documente cada endpoint com @Operation / @ApiResponse nos controllers.
                        """));
    }
}
