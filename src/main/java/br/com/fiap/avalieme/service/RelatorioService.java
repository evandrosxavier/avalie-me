package br.com.fiap.avalieme.service;

import br.com.fiap.avalieme.domain.Avaliacao;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RelatorioService {

    private static final Locale LOCALE_RELATORIO = Locale.of("pt", "BR");

    public String gerarHtml(List<Avaliacao> avaliacoes) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html lang='pt-BR'><head>")
          .append("<meta charset='UTF-8'>")
          .append("<title>Relatório Semanal de Avaliações</title>")
          .append("<style>")
          .append("body { font-family: Arial, sans-serif; margin: 40px; color: #333; }")
          .append("h1 { color: #1a73e8; }")
          .append("h2 { color: #555; border-bottom: 1px solid #ddd; padding-bottom: 6px; }")
          .append("table { border-collapse: collapse; width: 100%; margin-bottom: 24px; }")
          .append("th { background-color: #1a73e8; color: white; padding: 10px; text-align: left; }")
          .append("td { padding: 8px 10px; border-bottom: 1px solid #eee; }")
          .append("tr:hover { background-color: #f5f5f5; }")
          .append(".stat { font-size: 1.1em; margin: 6px 0; }")
          .append("</style>")
          .append("</head><body>");

        sb.append("<h1>Relatório Semanal de Avaliações</h1>");

        sb.append(secaoQualitativa(avaliacoes));
        sb.append(secaoQuantitativa(avaliacoes));

        sb.append("</body></html>");

        return sb.toString();
    }

    private String secaoQualitativa(List<Avaliacao> avaliacoes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Avaliações Recebidas</h2>");
        sb.append("<table>")
          .append("<tr><th>Descrição</th><th>Urgência</th><th>Data</th></tr>");

        for (Avaliacao avaliacao : avaliacoes) {
            sb.append("<tr>")
              .append("<td>").append(avaliacao.descricao()).append("</td>")
              .append("<td>").append(avaliacao.urgencia()).append("</td>")
              .append("<td>").append(avaliacao.dataRegistro().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()).append("</td>")
              .append("</tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    private String secaoQuantitativa(List<Avaliacao> avaliacoes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Resumo Quantitativo</h2>");

        double media = avaliacoes.stream()
                .mapToInt(Avaliacao::nota)
                .average()
                .orElse(0);
        sb.append("<p class='stat'><strong>Média das notas:</strong> ")
          .append(String.format(LOCALE_RELATORIO, "%.1f", media))
          .append("</p>");

        Map<String, Long> porUrgencia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.urgencia().name(),
                        Collectors.counting()
                ));
        sb.append("<h3>Total por urgência</h3><table>")
          .append("<tr><th>Urgência</th><th>Quantidade</th></tr>");
        porUrgencia.forEach((urgencia, total) ->
                sb.append("<tr><td>").append(urgencia).append("</td><td>").append(total).append("</td></tr>"));
        sb.append("</table>");

        Map<String, Long> porDia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.dataRegistro().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate().toString(),
                        Collectors.counting()
                ));
        sb.append("<h3>Total por dia</h3><table>")
          .append("<tr><th>Data</th><th>Quantidade</th></tr>");
        porDia.forEach((dia, total) ->
                sb.append("<tr><td>").append(dia).append("</td><td>").append(total).append("</td></tr>"));
        sb.append("</table>");

        return sb.toString();
    }
}
