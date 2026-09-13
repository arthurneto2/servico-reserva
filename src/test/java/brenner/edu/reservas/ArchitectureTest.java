package brenner.edu.reservas;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.implement;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import brenner.edu.reservas.shared.Result;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;

/**
 * Contrato arquitetural do projeto.
 *
 * <p>Estes testes são o "corretor automático" das regras combinadas em sala:
 * hexagonal (ports & adapters), UseCase + Command, Result somente nos ports e domínio rico.
 * Toda regra tem uma mensagem em português explicando o que foi violado e por quê.
 *
 * <p>Enquanto um pacote estiver vazio a regra correspondente não falha
 * ({@code archRule.failOnEmptyShould=false} em {@code src/test/resources/archunit.properties}).
 */
@AnalyzeClasses(packages = "brenner.edu.reservas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String INPUT = "..input..";
    private static final String CORE = "..core..";
    private static final String DOMAIN = "..core.domain..";
    private static final String USECASE = "..core.usecase..";
    private static final String PORT = "..core.port..";
    private static final String OUTPUT = "..output..";
    private static final String CONFIG = "..config..";
    private static final String CONTROLLER = "..input.rest.controller..";
    private static final String PERSISTENCE = "..output.persistence..";

    // =====================================================================
    // 1. FRONTEIRAS ENTRE CAMADAS (hexagonal)
    // =====================================================================

    @ArchTest
    static final ArchRule camadas_hexagonais = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Input").definedBy(INPUT)
            .layer("Core").definedBy(CORE)
            .layer("Output").definedBy(OUTPUT)
            .layer("Config").definedBy(CONFIG)
            .whereLayer("Input").mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Output").mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Core").mayOnlyBeAccessedByLayers("Input", "Output", "Config")
            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .because("o core é o centro do hexágono: input e output apontam para ele, nunca o contrário, "
                    + "e input e output não se conhecem");

    @ArchTest
    static final ArchRule core_nao_conhece_frameworks = noClasses()
            .that().resideInAPackage(CORE)
            .should().dependOnClassesThat(
                    resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "org.hibernate..",
                            "com.fasterxml..",
                            "io.swagger..")
                            .and(not(resideInAPackage("org.springframework.transaction.annotation.."))))
            .because("o core deve ser Java puro. Única exceção tolerada: @Transactional no UseCase. "
                    + "Validação de Command é feita à mão no construtor do record, não com Bean Validation");

    @ArchTest
    static final ArchRule dominio_nao_depende_de_usecase_nem_port = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(USECASE, PORT)
            .because("o domínio é a camada mais interna: UseCases e ports dependem dele, nunca o inverso");

    @ArchTest
    static final ArchRule dominio_nao_conhece_result = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().belongToAnyOf(Result.class)
            .because("Result é o contrato dos ports; o domínio nem sabe que ports existem. "
                    + "Regra de negócio falha lançando exceção de domínio, não devolvendo Result");

    @ArchTest
    static final ArchRule controller_nao_acessa_dominio_nem_port = noClasses()
            .that().resideInAPackage(CONTROLLER)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, PORT)
            .because("o controller só conhece UseCase, Command, DTO e mapper. "
                    + "Quem converte domínio em DTO é o mapper da camada input; quem chama port é o UseCase");

    // =====================================================================
    // 2. PORTS E RESULT
    // =====================================================================

    @ArchTest
    static final ArchRule ports_sao_interfaces_com_sufixo_port = classes()
            .that().resideInAPackage(PORT)
            .and().areNotAnnotations()
            .and().haveSimpleNameNotContaining("package-info")
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("Port")
            .because("um port é apenas um contrato (interface) que o core exige e o output cumpre. "
                    + "O package-info é excluído: para o ArchUnit ele é uma classe do pacote como "
                    + "outra qualquer, mas documenta o pacote em vez de declarar um port");

    @ArchTest
    static final ArchRule todo_metodo_de_port_retorna_result = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(PORT)
            .should().haveRawReturnType(Result.class)
            .because("REGRA CENTRAL DO PROJETO: todo port retorna Result<T>. "
                    + "Sem isso o UseCase não é obrigado a tratar indisponibilidade nem ausência de valor");

    @ArchTest
    static final ArchRule adapters_implementam_ports_apenas_no_output = classes()
            .that(implement(resideInAPackage(PORT)))
            .should().resideInAPackage(OUTPUT)
            .because("a implementação de um port é um adapter de saída; ela mora em output, nunca em core ou input");

    @ArchTest
    static final ArchRule adapters_nao_declaram_throws = methods()
            .that().areDeclaredInClassesThat(implement(resideInAPackage(PORT)))
            .and().arePublic()
            .should(naoDeclararThrows())
            .because("um adapter nunca deixa exceção escapar: captura tudo e devolve Result.failure(e)");

    @ArchTest
    static final ArchRule result_so_e_criado_no_output = noClasses()
            .that().resideInAnyPackage(CORE, INPUT, CONFIG)
            .should().callMethod(Result.class, "of", Object.class)
            .orShould().callMethod(Result.class, "empty")
            .orShould().callMethod(Result.class, "failure", Throwable.class)
            .because("Result.of/empty/failure são responsabilidade do adapter. "
                    + "O core apenas consome via getAvailableValueOrElseThrow(...)");

    // =====================================================================
    // 3. USECASES E COMMANDS
    // =====================================================================

    @ArchTest
    static final ArchRule usecases_moram_no_pacote_usecase = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().resideInAPackage(USECASE)
            .andShould().notBeInterfaces()
            .andShould().notHaveModifier(JavaModifier.FINAL)
            .because("UseCases são classes concretas em core.usecase (não interfaces). Não podem ser "
                    + "final porque @Transactional exige proxy CGLIB, que estende a classe");

    @ArchTest
    static final ArchRule usecase_tem_um_unico_execute_com_um_command = classes()
            .that().resideInAPackage(USECASE)
            .and().haveSimpleNameEndingWith("UseCase")
            .should(terExatamenteUmMetodoPublicoExecuteRecebendoUmCommand())
            .because("PADRÃO COMMAND: um UseCase expõe somente execute(XxxCommand). "
                    + "Um endpoint = um UseCase = um Command");

    @ArchTest
    static final ArchRule commands_sao_records_no_pacote_usecase = classes()
            .that().haveSimpleNameEndingWith("Command")
            .should().beRecords()
            .andShould().resideInAPackage(USECASE)
            .because("Command é um record imutável (validação de forma no construtor compacto) "
                    + "e pertence à camada de aplicação, ao lado do UseCase que o consome");

    @ArchTest
    static final ArchRule commands_nao_carregam_objetos_de_dominio = noClasses()
            .that().haveSimpleNameEndingWith("Command")
            .should().dependOnClassesThat().resideInAPackage(DOMAIN)
            .because("Commands carregam tipos primitivos/String/UUID/LocalDateTime. "
                    + "Quem constrói Value Objects e agregados é o UseCase; se o Command exigisse VOs, "
                    + "o controller precisaria conhecer o domínio");

    @ArchTest
    static final ArchRule usecase_nao_chama_outro_usecase = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should(naoDependerDeOutroUseCase())
            .because("UseCases são atômicos por endpoint. Lógica compartilhada vai para o domínio, "
                    + "nunca para uma cadeia de UseCases");

    @ArchTest
    static final ArchRule usecase_nao_e_bean_por_anotacao = noClasses()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
            .because("UseCases são registrados como @Bean em config.UseCaseConfig para manter o core "
                    + "livre de Spring");

    @ArchTest
    static final ArchRule excecoes_de_aplicacao_ficam_no_usecase = classes()
            .that().haveSimpleNameEndingWith("NotFoundException")
            .or().haveSimpleNameEndingWith("UnavailableException")
            .should().resideInAPackage(USECASE)
            .because("*NotFoundException (404) e *UnavailableException (503) nascem da orquestração "
                    + "de ports, portanto pertencem à camada de aplicação, não ao domínio");

    // =====================================================================
    // 4. DOMÍNIO RICO
    // =====================================================================

    @ArchTest
    static final ArchRule dominio_sem_setters = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN)
            .should().haveNameMatching("set[A-Z].*")
            .because("DOMÍNIO RICO: estado muda por comportamento com nome de negócio "
                    + "(reserva.cancelar(...), reserva.realizarCheckIn(...)), nunca por setter");

    @ArchTest
    static final ArchRule dominio_com_campos_privados = fields()
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN)
            .and().areNotStatic()
            .should().bePrivate()
            .because("invariantes só são garantidas se o estado é encapsulado");

    @ArchTest
    static final ArchRule dominio_imutavel_ou_final = classes()
            .that().resideInAPackage(DOMAIN)
            .and().areNotInterfaces()
            .and().areNotEnums()
            .and().areNotRecords()
            .and().areNotAssignableTo(Throwable.class)
            .and().doNotHaveModifier(JavaModifier.ABSTRACT)
            .should().haveModifier(JavaModifier.FINAL)
            .because("Value Objects são records; agregados são classes finais. "
                    + "Nada no domínio é feito para herança");

    @ArchTest
    static final ArchRule excecoes_de_dominio_terminam_com_exception = classes()
            .that().resideInAPackage(DOMAIN)
            .and().areAssignableTo(Throwable.class)
            .should().haveSimpleNameEndingWith("Exception")
            .andShould().beAssignableTo(RuntimeException.class)
            .because("exceções de domínio são unchecked e devem ser reconhecíveis pelo nome");

    // =====================================================================
    // 5. PERSISTÊNCIA
    // =====================================================================

    @ArchTest
    static final ArchRule entidades_jpa_so_no_output = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(PERSISTENCE)
            .andShould().haveSimpleNameEndingWith("Entity")
            .because("entidade JPA ≠ entidade de domínio. A JPA mora em output.persistence com sufixo "
                    + "Entity e é convertida por um mapper; o domínio nunca é anotado");

    @ArchTest
    static final ArchRule repositorios_spring_data_so_no_output = classes()
            .that().areAssignableTo("org.springframework.data.repository.Repository")
            .should().resideInAPackage(PERSISTENCE)
            .because("Spring Data é detalhe de infraestrutura; o core enxerga apenas os ports");

    // =====================================================================
    // Condições customizadas
    // =====================================================================

    private static ArchCondition<JavaMethod> naoDeclararThrows() {
        return new ArchCondition<>("não declarar cláusula throws") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!method.getThrowsClause().isEmpty()) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " declara throws; adapters devem capturar a exceção "
                                    + "e devolver Result.failure(e)"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> terExatamenteUmMetodoPublicoExecuteRecebendoUmCommand() {
        return new ArchCondition<>("ter exatamente um método público chamado 'execute' "
                + "recebendo um único parâmetro cujo tipo termina em 'Command'") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                var publicos = clazz.getMethods().stream()
                        .filter(m -> m.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(m -> !m.getModifiers().contains(JavaModifier.SYNTHETIC))
                        .toList();

                if (publicos.size() != 1) {
                    events.add(SimpleConditionEvent.violated(clazz,
                            clazz.getSimpleName() + " tem " + publicos.size()
                                    + " métodos públicos; deve ter apenas execute(XxxCommand)"));
                    return;
                }

                var metodo = publicos.get(0);
                if (!"execute".equals(metodo.getName())) {
                    events.add(SimpleConditionEvent.violated(clazz,
                            clazz.getSimpleName() + " expõe '" + metodo.getName()
                                    + "'; o único método público deve se chamar 'execute'"));
                    return;
                }

                var parametros = metodo.getRawParameterTypes();
                if (parametros.size() != 1 || !parametros.get(0).getSimpleName().endsWith("Command")) {
                    events.add(SimpleConditionEvent.violated(clazz,
                            clazz.getSimpleName() + ".execute deve receber exatamente um parâmetro "
                                    + "do tipo *Command"));
                    return;
                }

                events.add(SimpleConditionEvent.satisfied(clazz,
                        clazz.getSimpleName() + " respeita o padrão Command"));
            }
        };
    }

    private static ArchCondition<JavaClass> naoDependerDeOutroUseCase() {
        return new ArchCondition<>("não depender de outro UseCase") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                clazz.getDirectDependenciesFromSelf().stream()
                        .map(dep -> dep.getTargetClass())
                        .filter(target -> !target.equals(clazz))
                        .filter(target -> target.getSimpleName().endsWith("UseCase"))
                        .distinct()
                        .forEach(target -> events.add(SimpleConditionEvent.violated(clazz,
                                clazz.getSimpleName() + " depende de " + target.getSimpleName()
                                        + "; UseCases não se invocam")));
            }
        };
    }
}
