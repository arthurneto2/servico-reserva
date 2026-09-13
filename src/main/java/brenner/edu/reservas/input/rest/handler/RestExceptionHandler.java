package brenner.edu.reservas.input.rest.handler;

import brenner.edu.reservas.core.domain.exceptions.DomainException;
import brenner.edu.reservas.core.usecase.exceptions.NotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.UnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.util.stream.Collectors;

/**
 * Traduz exceção em status HTTP. É o único lugar do projeto que faz essa tradução, e por isso o
 * resto do código pode lançar sem pensar em HTTP.
 *
 * <p>São quatro famílias, e cada uma responde a uma pergunta diferente:
 *
 * <table border="1">
 *   <caption>Famílias de erro</caption>
 *   <tr><th>Exceção</th><th>Status</th><th>O que aconteceu</th></tr>
 *   <tr><td>{@code IllegalArgumentException}, {@code NullPointerException},
 *           erros de binding do Spring</td>
 *       <td>400</td><td>o pedido está malformado</td></tr>
 *   <tr><td>{@link NotFoundException}</td><td>404</td><td>o recurso pedido não existe</td></tr>
 *   <tr><td>{@link DomainException}</td><td>422</td>
 *       <td>o pedido está bem formado, mas uma regra de negócio o recusa</td></tr>
 *   <tr><td>{@link UnavailableException}</td><td>503</td>
 *       <td>uma dependência externa não respondeu</td></tr>
 * </table>
 *
 * <p>A distinção entre 400 e 422 é o que separa "você escreveu errado" de "o que você pediu não
 * pode acontecer": um e-mail sem arroba e uma sala lotada não são o mesmo tipo de problema.
 *
 * <p>Não existe um {@code @ExceptionHandler(Exception.class)} devolvendo 503. Erro inesperado
 * continua 500: um bug não é indisponibilidade, e disfarçá-lo de "tente de novo" esconderia
 * defeito em vez de corrigi-lo.
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * O carimbo de hora das respostas de erro.
     *
     * <p>Não passa pelo {@code GetDataHoraAtualPort} de propósito: aquele port existe porque o
     * domínio decide coisas com base no tempo — antecedência, prazo, janela de check-in — e cada
     * sala vive em um fuso. Aqui o tempo é só metadado de observabilidade, sem fuso de negócio e
     * sem decisão pendurada nele. Ainda assim é um {@link Clock} injetável, e não uma chamada
     * direta ao relógio.
     */
    private final Clock clock;

    public RestExceptionHandler() {
        this(Clock.systemUTC());
    }

    RestExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /** 404: o port devolveu vazio e o UseCase decidiu que aquela ausência é erro do chamador. */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail naoEncontrado(NotFoundException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage());
    }

    /** 422: o domínio recusou. A mensagem da exceção explica qual regra, e vai inteira ao cliente. */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail regraDeNegocio(DomainException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", e.getMessage());
    }

    /**
     * 503: uma dependência externa não respondeu.
     *
     * <p>O log da causa é obrigatório, e não zelo extra: o adapter capturou a exceção técnica
     * dentro de {@code Result.failure(e)}, então <b>este log é o único lugar onde o operador
     * descobre o que realmente aconteceu</b>. O cliente recebe só o que falhou — "a busca da sala
     * não pôde ser concluída" —, sem detalhe de infraestrutura.
     */
    @ExceptionHandler(UnavailableException.class)
    public ProblemDetail indisponivel(UnavailableException e) {
        log.error("Dependência indisponível: {}", e.getMessage(), e.getCause());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível", e.getMessage());
    }

    /**
     * 503: a infraestrutura de persistência falhou <b>fora</b> do alcance dos adapters.
     *
     * <p>O contrato do projeto diz que adapter nunca deixa exceção escapar — e ele cumpre. Mas
     * existe uma falha que acontece antes de qualquer adapter rodar: o {@code @Transactional} do
     * UseCase pede uma conexão ao entrar no método, e se o banco está fora, o Spring lança
     * {@link TransactionException} ali, envolvendo o UseCase inteiro. Nenhum {@code try/catch} de
     * adapter cobre isso, porque nada de dentro do UseCase chegou a executar. O mesmo vale para
     * uma {@link DataAccessException} no commit.
     *
     * <p>É a mesma situação das {@code *UnavailableException} — uma dependência externa não
     * respondeu — e por isso recebe o mesmo 503 e o mesmo log. Sem este tratamento, "banco fora
     * do ar" apareceria para o cliente como 500, isto é, como bug da aplicação.
     */
    @ExceptionHandler({TransactionException.class, DataAccessException.class})
    public ProblemDetail infraestruturaIndisponivel(RuntimeException e) {
        log.error("Persistência indisponível: {}", e.getMessage(), e);
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível",
                "A persistência não está disponível no momento");
    }

    /**
     * 400: o Command recusou a forma do pedido.
     *
     * <p>Os Commands validam com {@code Objects.requireNonNull} e {@code IllegalArgumentException}
     * justamente para não precisarem conhecer HTTP; sem este tratamento, essa validação viraria
     * 500.
     */
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ProblemDetail formaInvalida(RuntimeException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
    }

    /**
     * 400 da Bean Validation, com os campos recusados no {@code detail}.
     *
     * <p>O padrão do Spring é um "Invalid request content." genérico, que obriga quem integra a
     * adivinhar o que estava errado. Como a validação do DTO é só de forma, dizer qual campo
     * falhou não revela nada de negócio — é só devolver a informação que o cliente precisa para
     * corrigir o pedido.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String detalhe = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Requisição inválida");

        return handleExceptionInternal(e, carimbar(problema), headers, status, request);
    }

    /**
     * Os demais erros que o próprio Spring MVC detecta — corpo malformado, Bean Validation, header ou
     * query param ausente, id que não é UUID — já chegam como {@link ProblemDetail}. Aqui eles
     * apenas recebem o mesmo carimbo dos demais, para a API não devolver dois formatos de erro.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e,
                                                             Object corpo,
                                                             HttpHeaders headers,
                                                             HttpStatusCode status,
                                                             WebRequest request) {
        ResponseEntity<Object> resposta = super.handleExceptionInternal(e, corpo, headers, status, request);

        if (resposta != null && resposta.getBody() instanceof ProblemDetail problema) {
            carimbar(problema);
        }
        return resposta;
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return carimbar(problema);
    }

    private ProblemDetail carimbar(ProblemDetail problema) {
        problema.setProperty("timestamp", clock.instant());
        return problema;
    }
}
