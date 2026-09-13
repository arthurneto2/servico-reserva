package brenner.edu.reservas.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ResultTest {

    static final class Indisponivel extends RuntimeException {
        Indisponivel(Throwable cause) {
            super("indisponível", cause);
        }
    }

    @Test
    void sucessoComValorRetornaOptionalPreenchido() {
        var result = Result.of("valor");

        var opt = result.getAvailableValueOrElseThrow(Indisponivel::new);

        assertThat(opt).contains("valor");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.isFailure()).isFalse();
    }

    @Test
    void sucessoVazioRetornaOptionalVazio() {
        var result = Result.<String>empty();

        var opt = result.getAvailableValueOrElseThrow(Indisponivel::new);

        assertThat(opt).isEmpty();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void falhaLancaExcecaoMapeadaPreservandoACausa() {
        var causa = new IOException("banco fora");
        var result = Result.<String>failure(causa);

        assertThat(result.isFailure()).isTrue();
        assertThatThrownBy(() -> result.getAvailableValueOrElseThrow(Indisponivel::new))
                .isInstanceOf(Indisponivel.class)
                .hasCause(causa);
    }

    @Test
    void naoAceitaValorNuloEmOf() {
        assertThatThrownBy(() -> Result.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void naoAceitaErroNuloEmFailure() {
        assertThatThrownBy(() -> Result.failure(null)).isInstanceOf(NullPointerException.class);
    }
}
