package br.com.fiap.avalieme.service;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioServiceTest {

    private final RelatorioService service = new RelatorioService();
    private final LocalDate inicioPeriodo = LocalDate.parse("2026-07-20");
    private final LocalDate fimPeriodo = LocalDate.parse("2026-07-26");

    @Test
    void deveCalcularMediaDasNotasCorretamente() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z")),
                new Avaliacao("2", "Curso razoavel, esperava mais exemplos", 5, Urgencia.MEDIA, Instant.parse("2026-07-20T18:00:00Z")),
                new Avaliacao("3", "Curso muito bom, aprendi bastante coisa", 9, Urgencia.BAIXA, Instant.parse("2026-07-21T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes, inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("<div class='cartao'><div class='valor'>5,3</div><div class='rotulo'>Média das notas</div></div>"),
                "HTML deveria conter a média 5,3 (locale pt-BR), calculada de (2+5+9)/3");
    }

    @Test
    void deveContarAvaliacoesPorUrgencia() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z")),
                new Avaliacao("2", "Curso razoavel, esperava mais exemplos", 5, Urgencia.MEDIA, Instant.parse("2026-07-20T18:00:00Z")),
                new Avaliacao("3", "Curso muito bom, aprendi bastante coisa", 9, Urgencia.BAIXA, Instant.parse("2026-07-21T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes, inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("<div class='cartao alta'><div class='valor'>1</div>"));
        assertTrue(html.contains("<div class='cartao media'><div class='valor'>1</div>"));
        assertTrue(html.contains("<div class='cartao baixa'><div class='valor'>1</div>"));
    }

    @Test
    void deveContarAvaliacoesPorDiaRespeitandoFusoHorarioDeSaoPaulo() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z")),
                new Avaliacao("2", "Curso razoavel, esperava mais exemplos", 5, Urgencia.MEDIA, Instant.parse("2026-07-20T18:00:00Z")),
                new Avaliacao("3", "Curso muito bom, aprendi bastante coisa", 9, Urgencia.BAIXA, Instant.parse("2026-07-21T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes, inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("<span class='rotulo-dia'>20/07</span>"));
        assertTrue(html.contains("<span class='rotulo-dia'>21/07</span>"));
        assertTrue(html.contains("<span class='contagem'>2</span>"));
        assertTrue(html.contains("<span class='contagem'>1</span>"));
    }

    @Test
    void deveListarDescricaoDeCadaAvaliacaoNaSecaoQualitativa() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes, inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("Curso ruim, faltou pratica no conteudo"));
        assertTrue(html.contains("<span class='pill alta'>ALTA</span>"));
    }

    @Test
    void deveExibirNotaDeCadaAvaliacaoAntesDaUrgencia() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes, inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("<th>Descrição</th><th>Nota</th><th>Urgência</th><th>Data</th>"));
        assertTrue(html.contains("<td>2</td><td><span class='pill alta'>ALTA</span></td>"));
    }

    @Test
    void deveExibirLegendaComIntervalosDeUrgencia() {
        String html = service.gerarHtml(List.of(), inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("ALTA — nota 0 a 3"));
        assertTrue(html.contains("MÉDIA — nota 4 a 6"));
        assertTrue(html.contains("BAIXA — nota 7 a 10"));
    }

    @Test
    void deveGerarMediaZeroQuandoListaDeAvaliacoesForVazia() {
        String html = service.gerarHtml(List.of(), inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("<div class='cartao'><div class='valor'>0,0</div><div class='rotulo'>Média das notas</div></div>"));
        assertTrue(html.contains("Nenhuma avaliação no período."));
        assertTrue(html.contains("Nenhuma avaliação recebida no período."));
    }

    @Test
    void deveExibirPeriodoNoCabecalho() {
        String html = service.gerarHtml(List.of(), inicioPeriodo, fimPeriodo);

        assertTrue(html.contains("Período de 20/07/2026 a 26/07/2026"));
    }
}
