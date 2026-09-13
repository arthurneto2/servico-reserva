/**
 * UseCases. Um UseCase é uma classe (não interface) com exatamente um método público execute(XxxCommand). Orquestra ports e domínio; não contém regra de negócio; nunca chama outro UseCase. Sua entrada mora em {@code core.usecase.commands} e as exceções de aplicação que ele lança, em {@code core.usecase.exceptions}.
 */
package brenner.edu.reservas.core.usecase;
