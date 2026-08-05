/**
 * Fachada "/api/me" do app do servidor: cada endpoint deriva o vinculo do dispositivo
 * autenticado (token), nunca de parametro do cliente — garantindo que o servidor
 * acesse apenas os proprios dados. Delegacao fina para os servicos de dominio.
 */
package br.gov.ponto.me;
