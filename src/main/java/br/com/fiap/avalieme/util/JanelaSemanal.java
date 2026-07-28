package br.com.fiap.avalieme.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Semana civil fechada (segunda a domingo) usada pelo relatorio semanal.
 * A janela nunca inclui o dia da execucao: o relatorio disparado na segunda-feira
 * cobre da segunda-feira anterior ao ultimo domingo.
 */
public record JanelaSemanal(LocalDate primeiroDia, LocalDate ultimoDia, ZoneId fuso) {

    public static JanelaSemanal semanaAnteriorA(LocalDate referencia, ZoneId fuso) {
        LocalDate segundaDaSemanaAtual = referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new JanelaSemanal(
                segundaDaSemanaAtual.minusWeeks(1),
                segundaDaSemanaAtual.minusDays(1),
                fuso
        );
    }

    /** Inicio inclusivo: 00:00 da segunda-feira, no fuso do relatorio. */
    public Instant inicio() {
        return primeiroDia.atStartOfDay(fuso).toInstant();
    }

    /** Fim exclusivo: 00:00 da segunda-feira seguinte, cobrindo o domingo por inteiro. */
    public Instant fimExclusivo() {
        return ultimoDia.plusDays(1).atStartOfDay(fuso).toInstant();
    }
}
