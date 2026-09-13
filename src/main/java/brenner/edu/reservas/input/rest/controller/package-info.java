/**
 * Controllers REST. Regra: um endpoint = um UseCase. O controller monta o Command a partir do DTO, chama execute(), entrega o retorno ao mapper. Não acessa core.domain nem core.port diretamente.
 */
package brenner.edu.reservas.input.rest.controller;
