package brenner.edu.reservas.shared;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Envelope obrigatório de retorno de TODO port de saída.
 *
 * <p>Um {@code Result} está em exatamente um de três estados:
 * <ul>
 *   <li><b>falha</b>: o adapter capturou uma exceção técnica (banco fora, timeout, erro de
 *       mapeamento) e a encapsulou. {@link #getAvailableValueOrElseThrow(Function)} lança a
 *       exceção que o chamador escolher.</li>
 *   <li><b>sucesso vazio</b>: o adapter funcionou e não encontrou nada. Retorna
 *       {@code Optional.empty()}.</li>
 *   <li><b>sucesso com valor</b>: retorna {@code Optional.of(valor)}.</li>
 * </ul>
 *
 * <p>Regras do projeto (verificadas por teste de arquitetura):
 * <ul>
 *   <li>Somente adapters (pacote {@code output}) podem criar um {@code Result}
 *       via {@link #of(Object)}, {@link #empty()} ou {@link #failure(Throwable)}.</li>
 *   <li>O core apenas consome, sempre via {@link #getAvailableValueOrElseThrow(Function)}.</li>
 *   <li>O domínio ({@code core.domain}) não conhece esta classe.</li>
 * </ul>
 *
 * <p>Uso esperado dentro de um UseCase:
 * <pre>{@code
 * final var reserva = getReservaPort.getById(reservaId)
 *         .getAvailableValueOrElseThrow(GetReservaUnavailableException::new)
 *         .orElseThrow(() -> new ReservaNotFoundException(reservaId));
 * }</pre>
 *
 * @param <T> tipo do valor carregado
 */
public final class Result<T> {

    private final T value;
    private final Throwable error;

    private Result(T value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    /** Sucesso com valor. O valor não pode ser nulo; para ausência use {@link #empty()}. */
    public static <T> Result<T> of(T value) {
        return new Result<>(Objects.requireNonNull(value, "value não pode ser nulo; use Result.empty()"), null);
    }

    /** Sucesso sem valor (ex.: registro não encontrado). */
    public static <T> Result<T> empty() {
        return new Result<>(null, null);
    }

    /** Falha técnica capturada pelo adapter. */
    public static <T> Result<T> failure(Throwable error) {
        return new Result<>(null, Objects.requireNonNull(error, "error não pode ser nulo"));
    }

    /**
     * Único caminho de acesso ao valor.
     *
     * @param errorMapper converte a exceção capturada pelo adapter na exceção que o chamador
     *                    quer propagar (tipicamente uma {@code *UnavailableException})
     * @return {@code Optional} com o valor, ou vazio se o adapter não encontrou nada
     * @throws X se este Result representa uma falha
     */
    public <X extends Throwable> Optional<T> getAvailableValueOrElseThrow(
            Function<? super Throwable, ? extends X> errorMapper) throws X {
        Objects.requireNonNull(errorMapper, "errorMapper não pode ser nulo");
        if (error != null) {
            throw errorMapper.apply(error);
        }
        return Optional.ofNullable(value);
    }

    public boolean isFailure() {
        return error != null;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isEmpty() {
        return error == null && value == null;
    }

    @Override
    public String toString() {
        if (error != null) {
            return "Result.failure(" + error.getClass().getSimpleName() + ": " + error.getMessage() + ")";
        }
        return value == null ? "Result.empty()" : "Result.of(" + value + ")";
    }
}
