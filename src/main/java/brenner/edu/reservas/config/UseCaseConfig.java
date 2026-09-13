package brenner.edu.reservas.config;

import org.springframework.context.annotation.Configuration;

/**
 * O core não pode importar Spring (exceto {@code @Transactional}), portanto os UseCases
 * NÃO são anotados com {@code @Service}/{@code @Component}.
 *
 * <p>Registre cada UseCase aqui como {@code @Bean}, injetando os ports pelos construtores:
 * <pre>{@code
 * @Bean
 * CriarReservaUseCase criarReservaUseCase(GetSalaPort getSala,
 *                                         GetReservasAtivasDaSalaPort getReservas,
 *                                         SaveReservaPort saveReserva,
 *                                         NotificarOrganizadorPort notificar,
 *                                         GetDataHoraAtualPort relogio) {
 *     return new CriarReservaUseCase(getSala, getReservas, saveReserva, notificar, relogio);
 * }
 * }</pre>
 *
 * <p>Os adapters (pacote {@code output}) podem ser anotados com {@code @Component} normalmente.
 */
@Configuration
public class UseCaseConfig {
}
