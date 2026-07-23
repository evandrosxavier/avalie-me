package br.com.fiap.avalieme.service;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioServiceTest {

    private final RelatorioService service = new RelatorioService();

    @Test
    void deveCalcularMediaDasNotasCorretamente() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z")),
                new Avaliacao("2", "Curso razoavel, esperava mais exemplos", 5, Urgencia.MEDIA, Instant.parse("2026-07-20T18:00:00Z")),
                new Avaliacao("3", "Curso muito bom, aprendi bastante coisa", 9, Urgencia.BAIXA, Instant.parse("2026-07-21T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes);

        assertTrue(html.contains("<strong>Média das notas:</strong> 5,3"),
                "HTML deveria conter a média 5,3 (locale pt-BR), calculada de (2+5+9)/3");
    }

    @Test
    void deveContarAvaliacoesPorUrgencia() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z")),
                new Avaliacao("2", "Curso razoavel, esperava mais exemplos", 5, Urgencia.MEDIA, Instant.parse("2026-07-20T18:00:00Z")),
                new Avaliacao("3", "Curso muito bom, aprendi bastante coisa", 9, Urgencia.BAIXA, Instant.parse("2026-07-21T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes);

        assertTrue(html.contains("<td>ALTA</td><td>1</td>"));
        assertTrue(html.contains("<td>MEDIA</td><td>1</td>"));
        assertTrue(html.contains("<td>BAIXA</td><td>1</td>"));
    }

    @Test
    void deveContarAvaliacoesPorDiaRespeitandoFusoHorarioDeSaoPaulo() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z")),
                new Avaliacao("2", "Curso razoavel, esperava mais exemplos", 5, Urgencia.MEDIA, Instant.parse("2026-07-20T18:00:00Z")),
                new Avaliacao("3", "Curso muito bom, aprendi bastante coisa", 9, Urgencia.BAIXA, Instant.parse("2026-07-21T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes);

        assertTrue(html.contains("<td>2026-07-20</td><td>2</td>"));
        assertTrue(html.contains("<td>2026-07-21</td><td>1</td>"));
    }

    @Test
    void deveListarDescricaoDeCadaAvaliacaoNaSecaoQualitativa() {
        List<Avaliacao> avaliacoes = List.of(
                new Avaliacao("1", "Curso ruim, faltou pratica no conteudo", 2, Urgencia.ALTA, Instant.parse("2026-07-20T15:00:00Z"))
        );

        String html = service.gerarHtml(avaliacoes);

        assertTrue(html.contains("Curso ruim, faltou pratica no conteudo"));
    }

    @Test
    void deveGerarMediaZeroQuandoListaDeAvaliacoesForVazia() {
        String html = service.gerarHtml(List.of());

        assertTrue(html.contains("<strong>Média das notas:</strong> 0,0"));
    }
}
