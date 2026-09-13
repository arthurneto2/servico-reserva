/**
 * Entidades JPA (sufixo Entity obrigatório, anotadas com @Entity): espelho plano das tabelas do docker/init.sql, com TIMESTAMPTZ <-> Instant. São estruturas de dados, não agregados — campos mutáveis, construtor vazio e nenhuma invariante, porque a aplicação sobe com ddl-auto=validate e o que vale aqui é bater com o schema. Quem garante as regras é core.domain, reconstruído pelos mappers.
 */
package brenner.edu.reservas.output.persistence.entities;
