/**
 * UseCases e Commands. Um UseCase é uma classe (não interface) com exatamente um método público execute(XxxCommand). Orquestra ports e domínio; não contém regra de negócio; nunca chama outro UseCase. Commands são records imutáveis que validam apenas forma (não nulo, formato). Exceções de aplicação (*NotFoundException, *UnavailableException) também moram aqui.
 */
package brenner.edu.reservas.core.usecase;
