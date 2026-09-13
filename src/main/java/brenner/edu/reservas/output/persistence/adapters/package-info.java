/**
 * Adapters (driven) dos ports de persistência: uma classe por port, anotada com @Component e implementando a interface que o core declarou. Nenhum deixa exceção escapar e nenhum declara throws — capturam tudo, inclusive a invariante de domínio violada por um registro gravado por fora da aplicação, e devolvem Result.failure(e). São também o único lugar do projeto, junto dos demais adapters, autorizado a criar um Result.
 */
package brenner.edu.reservas.output.persistence.adapters;
