/**
 * Commands: a entrada de cada UseCase. São records imutáveis que validam apenas forma (não nulo, formato parseável, número positivo), lançando IllegalArgumentException/NullPointerException — que o handler converte em 400. Carregam tipos primitivos, String, UUID, LocalDateTime e LocalDate: quem constrói Value Objects e agregados é o UseCase, para que o controller não precise conhecer o domínio.
 */
package brenner.edu.reservas.core.usecase.commands;
