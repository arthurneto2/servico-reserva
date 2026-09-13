/**
 * Resultados compostos de UseCase: o que um único agregado não responde sozinho. São records imutáveis de saída, montados pelo UseCase com objetos de domínio — é o mapper da camada input que os converte em DTO. Consultas simples não passam por aqui: quando o UseCase tem um agregado a devolver, ele devolve o agregado.
 */
package brenner.edu.reservas.core.usecase.results;
