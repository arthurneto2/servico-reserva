package brenner.edu.reservas.core.domain.valueObjects;

import brenner.edu.reservas.core.domain.exceptions.PeriodoInvalidoException;

import java.time.Duration;
import java.time.LocalTime;

public record Periodo(DataHora inicio, DataHora fim) {

    private static final Duration DURACAO_MINIMA = Duration.ofMinutes(30);
    private static final Duration DURACAO_MAXIMA = Duration.ofHours(4);
    private static final LocalTime HORARIO_ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORARIO_FECHAMENTO = LocalTime.of(20, 0);
    private static final int GRANULARIDADE_MINUTOS = 15;

    public Periodo {
        if (inicio == null)
            throw new PeriodoInvalidoException("Início não pode ser nulo");
        if (fim == null)
            throw new PeriodoInvalidoException("Fim não pode ser nulo");

        if (!inicio.fusoHorario().equals(fim.fusoHorario()))
            throw new PeriodoInvalidoException(
                    "Início e fim devem estar no mesmo fuso horário: "
                            + inicio.fusoHorario().value() + " e " + fim.fusoHorario().value());

        if (!inicio.antesDe(fim))
            throw new PeriodoInvalidoException("Fim deve ser estritamente depois do início");

        Duration duracao = Duration.between(inicio.instante(), fim.instante());
        if (duracao.compareTo(DURACAO_MINIMA) < 0 || duracao.compareTo(DURACAO_MAXIMA) > 0)
            throw new PeriodoInvalidoException(
                    "Duração deve estar entre 30 minutos e 4 horas: " + duracao.toMinutes() + " minutos");

        if (!inicio.data().equals(fim.data()))
            throw new PeriodoInvalidoException(
                    "Início e fim devem ocorrer no mesmo dia local: "
                            + inicio.data() + " e " + fim.data());

        LocalTime horaInicio = inicio.hora();
        if (horaInicio.getMinute() % GRANULARIDADE_MINUTOS != 0
                || horaInicio.getSecond() != 0
                || horaInicio.getNano() != 0)
            throw new PeriodoInvalidoException(
                    "Início deve estar alinhado a múltiplos de " + GRANULARIDADE_MINUTOS
                            + " minutos: " + horaInicio);

        LocalTime horaFim = fim.hora();
        if (horaInicio.isBefore(HORARIO_ABERTURA) || horaFim.isAfter(HORARIO_FECHAMENTO))
            throw new PeriodoInvalidoException(
                    "Período deve estar contido no horário de funcionamento ("
                            + HORARIO_ABERTURA + "–" + HORARIO_FECHAMENTO + "): "
                            + horaInicio + "–" + horaFim);
    }

    public Duration duracao() {
        return Duration.between(inicio.instante(), fim.instante());
    }

    public boolean sobrepoe(Periodo outroPeriodo) {
        if (outroPeriodo == null)
            throw new PeriodoInvalidoException("Período comparado não pode ser nulo");

        return inicio.antesDe(outroPeriodo.fim) && outroPeriodo.inicio.antesDe(fim);
    }
}
