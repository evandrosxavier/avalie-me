package br.com.fiap.avalieme.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JanelaSemanalTest {

    private static final ZoneId FUSO_SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void relatorioDeSegundaDeveCobrirDaSegundaAnterioAoDomingo() {
        // 27/07/2026 e uma segunda-feira: o relatorio deve cobrir 20/07 a 26/07
        JanelaSemanal semana = JanelaSemanal.semanaAnteriorA(LocalDate.of(2026, 7, 27), FUSO_SAO_PAULO);

        assertEquals(LocalDate.of(2026, 7, 20), semana.primeiroDia());
        assertEquals(LocalDate.of(2026, 7, 26), semana.ultimoDia());
    }

    @Test
    void deveUsarMeiaNoiteDeSaoPauloComoLimites() {
        JanelaSemanal semana = JanelaSemanal.semanaAnteriorA(LocalDate.of(2026, 7, 27), FUSO_SAO_PAULO);

        // Sao Paulo esta em UTC-3: 00:00 local = 03:00 UTC
        assertEquals(Instant.parse("2026-07-20T03:00:00Z"), semana.inicio());
        assertEquals(Instant.parse("2026-07-27T03:00:00Z"), semana.fimExclusivo());
    }

    @Test
    void deveIgnorarDiaDaExecucaoQuandoRodadaForaDaSegunda() {
        // Execucao manual na quarta 29/07/2026 continua cobrindo 20/07 a 26/07
        JanelaSemanal semana = JanelaSemanal.semanaAnteriorA(LocalDate.of(2026, 7, 29), FUSO_SAO_PAULO);

        assertEquals(LocalDate.of(2026, 7, 20), semana.primeiroDia());
        assertEquals(LocalDate.of(2026, 7, 26), semana.ultimoDia());
    }

    @Test
    void deveCobrirSempreSeteDias() {
        JanelaSemanal semana = JanelaSemanal.semanaAnteriorA(LocalDate.of(2026, 7, 27), FUSO_SAO_PAULO);

        assertEquals(7, java.time.temporal.ChronoUnit.DAYS.between(semana.inicio(), semana.fimExclusivo()));
    }

    @Test
    void domingoDeveContarComoUltimoDiaDaSemanaAnterior() {
        // Domingo 26/07/2026 pertence a semana que comeca em 20/07,
        // entao a semana anterior a ele e 13/07 a 19/07
        JanelaSemanal semana = JanelaSemanal.semanaAnteriorA(LocalDate.of(2026, 7, 26), FUSO_SAO_PAULO);

        assertEquals(LocalDate.of(2026, 7, 13), semana.primeiroDia());
        assertEquals(LocalDate.of(2026, 7, 19), semana.ultimoDia());
    }
}
