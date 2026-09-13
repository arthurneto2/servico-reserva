/**
 * Exceções de aplicação, nascidas da orquestração de ports e não do domínio. NotFoundException (404) marca o port que devolveu Result.empty() quando o recurso era obrigatório; UnavailableException (503) marca o port que devolveu Result.failure(e) e guarda a causa original, que o handler é obrigado a logar. Regra de negócio violada não mora aqui: é DomainException (422), em core.domain.exceptions.
 */
package brenner.edu.reservas.core.usecase.exceptions;
