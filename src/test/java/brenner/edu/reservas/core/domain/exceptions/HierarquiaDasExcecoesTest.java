package brenner.edu.reservas.core.domain.exceptions;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Toda exceção do domínio precisa herdar de {@link DomainException}.
 *
 * <p>Esta regra existe por causa de um bug real: {@code PeriodoInvalidoException},
 * {@code DataHoraInvalidaException} e {@code FusoHorarioInvalidoException} herdavam direto de
 * {@code RuntimeException}. Passavam no {@code ArchitectureTest} — que exige apenas
 * {@code RuntimeException} e o sufixo {@code Exception} — e passavam nos testes de unidade, que
 * verificam o tipo concreto lançado. Mas o {@code @ControllerAdvice} mapeia <b>DomainException</b>
 * para 422, então reservas com duração inválida, fora do horário de funcionamento ou com início
 * desalinhado respondiam <b>500</b>: regra de negócio disfarçada de falha do servidor.
 *
 * <p>É um teste separado, e não uma regra adicionada ao {@code ArchitectureTest}, porque aquele
 * arquivo é o contrato entregue com o esqueleto e não deve ser alterado.
 */
@AnalyzeClasses(packages = "brenner.edu.reservas.core.domain",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HierarquiaDasExcecoesTest {

    @ArchTest
    static final ArchRule toda_excecao_de_dominio_herda_de_domain_exception = classes()
            .that().resideInAPackage("..core.domain..")
            .and().areAssignableTo(Throwable.class)
            .and().doNotHaveSimpleName("DomainException")
            .should().beAssignableTo(DomainException.class)
            .because("o handler traduz DomainException em 422; herdar direto de RuntimeException "
                    + "faz a regra de negócio virar 500");
}
