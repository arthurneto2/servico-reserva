package brenner.edu.reservas.config;

import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservaPort;
import brenner.edu.reservas.core.port.GetReservasAtivasDaSalaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.port.NotificarOrganizadorPort;
import brenner.edu.reservas.core.port.SaveReservaPort;
import brenner.edu.reservas.core.usecase.CancelarReservaUseCase;
import brenner.edu.reservas.core.usecase.ConsultarDisponibilidadeUseCase;
import brenner.edu.reservas.core.usecase.CriarReservaUseCase;
import brenner.edu.reservas.core.usecase.RealizarCheckInUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O core não pode importar Spring (exceto {@code @Transactional}), portanto os UseCases
 * NÃO são anotados com {@code @Service}/{@code @Component}: eles são registrados aqui.
 *
 * <p>Note que os parâmetros são <b>ports</b>, não adapters. É o Spring que encontra a única
 * implementação de cada um no pacote {@code output}; trocar o adapter de notificação por um que
 * envia e-mail, ou o de tempo por um que consulta um serviço externo, não toca nesta classe nem
 * no core.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    CriarReservaUseCase criarReservaUseCase(GetSalaPort getSalaPort,
                                            GetDataHoraAtualPort getDataHoraAtualPort,
                                            GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort,
                                            SaveReservaPort saveReservaPort,
                                            NotificarOrganizadorPort notificarOrganizadorPort) {
        return new CriarReservaUseCase(getSalaPort, getDataHoraAtualPort, getReservasAtivasDaSalaPort,
                saveReservaPort, notificarOrganizadorPort);
    }

    @Bean
    CancelarReservaUseCase cancelarReservaUseCase(GetReservaPort getReservaPort,
                                                  GetSalaPort getSalaPort,
                                                  GetDataHoraAtualPort getDataHoraAtualPort,
                                                  SaveReservaPort saveReservaPort) {
        return new CancelarReservaUseCase(getReservaPort, getSalaPort, getDataHoraAtualPort, saveReservaPort);
    }

    @Bean
    RealizarCheckInUseCase realizarCheckInUseCase(GetReservaPort getReservaPort,
                                                  GetSalaPort getSalaPort,
                                                  GetDataHoraAtualPort getDataHoraAtualPort,
                                                  SaveReservaPort saveReservaPort) {
        return new RealizarCheckInUseCase(getReservaPort, getSalaPort, getDataHoraAtualPort, saveReservaPort);
    }

    @Bean
    ConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase(
            GetSalaPort getSalaPort,
            GetDataHoraAtualPort getDataHoraAtualPort,
            GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort) {
        return new ConsultarDisponibilidadeUseCase(getSalaPort, getDataHoraAtualPort, getReservasAtivasDaSalaPort);
    }
}
