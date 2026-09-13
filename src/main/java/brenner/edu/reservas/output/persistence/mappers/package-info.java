/**
 * Conversão Entity <-> domínio. Reconstruir o domínio é reexecutar suas invariantes: um dado gravado por fora da aplicação que viole um Value Object explode aqui, e o adapter que chamou o mapper transforma a exceção em Result.failure(e). O fuso horário chega como parâmetro em ReservaMapper, porque a tabela guarda instantes e quem conhece o fuso é a sala.
 */
package brenner.edu.reservas.output.persistence.mappers;
