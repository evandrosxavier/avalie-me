package br.com.fiap.avalieme.service;

import br.com.fiap.avalieme.domain.Avaliacao;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RelatorioService {

    private static final Locale LOCALE_RELATORIO = Locale.of("pt", "BR");
    private static final DateTimeFormatter FORMATO_DATA_CURTA =
            DateTimeFormatter.ofPattern("dd/MM", LOCALE_RELATORIO);
    private static final DateTimeFormatter FORMATO_DATA_LONGA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_RELATORIO);

    public String gerarHtml(List<Avaliacao> avaliacoes, LocalDate inicio, LocalDate fim) {
        double media = avaliacoes.stream()
                .mapToInt(Avaliacao::nota)
                .average()
                .orElse(0);

        Map<String, Long> porUrgencia = avaliacoes.stream()
                .collect(Collectors.groupingBy(a -> a.urgencia().name(), Collectors.counting()));

        Map<String, Long> porDia = new TreeMap<>(avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.dataRegistro().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate().toString(),
                        Collectors.counting()
                )));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='pt-BR'><head>")
          .append("<meta charset='UTF-8'>")
          .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
          .append("<title>Relatório Semanal de Avaliações</title>")
          .append(css())
          .append("</head><body>");

        sb.append("<div class='container'>");
        sb.append(cabecalho(inicio, fim));
        sb.append(cartoesResumo(avaliacoes.size(), media, porUrgencia));
        sb.append(secaoPorDia(porDia));
        sb.append(secaoAvaliacoes(avaliacoes));
        sb.append(rodape());
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String css() {
        return "<style>"
                + ":root{--azul:#2563eb;--azul-escuro:#1d4ed8;--fundo:#f4f6fb;--cartao:#ffffff;"
                + "--texto:#1f2937;--texto-suave:#6b7280;--borda:#e5e7eb;"
                + "--alta:#dc2626;--alta-fundo:#fee2e2;--media:#d97706;--media-fundo:#fef3c7;"
                + "--baixa:#16a34a;--baixa-fundo:#dcfce7;}"
                + "*{box-sizing:border-box;}"
                + "body{margin:0;padding:24px;background:var(--fundo);color:var(--texto);"
                + "font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;}"
                + ".container{max-width:760px;margin:0 auto;}"
                + ".cabecalho{background:linear-gradient(135deg,var(--azul),var(--azul-escuro));"
                + "color:#fff;border-radius:12px;padding:28px 32px;margin-bottom:20px;}"
                + ".cabecalho h1{margin:0 0 6px;font-size:22px;}"
                + ".cabecalho p{margin:0;opacity:.9;font-size:14px;}"
                + ".cartoes{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));"
                + "gap:12px;margin-bottom:20px;}"
                + ".cartao{background:var(--cartao);border:1px solid var(--borda);border-radius:10px;"
                + "padding:16px;text-align:center;}"
                + ".cartao .valor{font-size:26px;font-weight:700;}"
                + ".cartao .rotulo{font-size:12px;color:var(--texto-suave);margin-top:4px;"
                + "text-transform:uppercase;letter-spacing:.03em;}"
                + ".cartao.alta .valor{color:var(--alta);}"
                + ".cartao.media .valor{color:var(--media);}"
                + ".cartao.baixa .valor{color:var(--baixa);}"
                + ".secao{background:var(--cartao);border:1px solid var(--borda);border-radius:10px;"
                + "padding:20px 24px;margin-bottom:16px;}"
                + ".secao h2{margin:0 0 14px;font-size:15px;color:var(--texto-suave);"
                + "text-transform:uppercase;letter-spacing:.04em;}"
                + ".barra-linha{display:flex;align-items:center;gap:10px;margin-bottom:8px;font-size:13px;}"
                + ".barra-linha .rotulo-dia{width:48px;color:var(--texto-suave);flex-shrink:0;}"
                + ".barra-fundo{flex:1;background:#eef1f6;border-radius:6px;overflow:hidden;height:18px;}"
                + ".barra-preenchida{background:var(--azul);height:100%;border-radius:6px;}"
                + ".barra-linha .contagem{width:24px;text-align:right;font-weight:600;flex-shrink:0;}"
                + "table{width:100%;border-collapse:collapse;}"
                + "th{text-align:left;font-size:12px;color:var(--texto-suave);text-transform:uppercase;"
                + "letter-spacing:.03em;padding:8px 6px;border-bottom:2px solid var(--borda);}"
                + "td{padding:10px 6px;border-bottom:1px solid var(--borda);font-size:14px;vertical-align:top;}"
                + "tr:last-child td{border-bottom:none;}"
                + ".pill{display:inline-block;padding:3px 10px;border-radius:999px;font-size:11px;"
                + "font-weight:700;text-transform:uppercase;letter-spacing:.02em;}"
                + ".pill.alta{background:var(--alta-fundo);color:var(--alta);}"
                + ".pill.media{background:var(--media-fundo);color:var(--media);}"
                + ".pill.baixa{background:var(--baixa-fundo);color:var(--baixa);}"
                + ".vazio{color:var(--texto-suave);font-size:14px;text-align:center;padding:12px;}"
                + ".legenda{display:flex;flex-wrap:wrap;gap:14px;margin-top:14px;padding-top:14px;"
                + "border-top:1px solid var(--borda);}"
                + ".legenda-item{display:flex;align-items:center;gap:6px;font-size:12px;color:var(--texto-suave);}"
                + ".legenda-cor{width:10px;height:10px;border-radius:50%;flex-shrink:0;}"
                + ".legenda-cor.alta{background:var(--alta);}"
                + ".legenda-cor.media{background:var(--media);}"
                + ".legenda-cor.baixa{background:var(--baixa);}"
                + ".rodape{text-align:center;color:var(--texto-suave);font-size:12px;margin-top:20px;}"
                + "</style>";
    }

    private String cabecalho(LocalDate inicio, LocalDate fim) {
        return "<div class='cabecalho'>"
                + "<h1>Relatório Semanal de Avaliações</h1>"
                + "<p>Período de " + inicio.format(FORMATO_DATA_LONGA) + " a " + fim.format(FORMATO_DATA_LONGA) + "</p>"
                + "</div>";
    }

    private String cartoesResumo(int total, double media, Map<String, Long> porUrgencia) {
        StringBuilder sb = new StringBuilder("<div class='cartoes'>");
        sb.append(cartao("total", total, "Avaliações"));
        sb.append(cartao("", String.format(LOCALE_RELATORIO, "%.1f", media), "Média das notas"));
        sb.append(cartao("alta", porUrgencia.getOrDefault("ALTA", 0L), "Urgência alta"));
        sb.append(cartao("media", porUrgencia.getOrDefault("MEDIA", 0L), "Urgência média"));
        sb.append(cartao("baixa", porUrgencia.getOrDefault("BAIXA", 0L), "Urgência baixa"));
        sb.append("</div>");
        return sb.toString();
    }

    private String cartao(String classe, Object valor, String rotulo) {
        String classeCss = classe.isEmpty() ? "cartao" : "cartao " + classe;
        return "<div class='" + classeCss + "'>"
                + "<div class='valor'>" + valor + "</div>"
                + "<div class='rotulo'>" + rotulo + "</div>"
                + "</div>";
    }

    private String secaoPorDia(Map<String, Long> porDia) {
        StringBuilder sb = new StringBuilder("<div class='secao'><h2>Avaliações por dia</h2>");

        if (porDia.isEmpty()) {
            sb.append("<p class='vazio'>Nenhuma avaliação no período.</p>");
        } else {
            long maximo = porDia.values().stream().mapToLong(Long::longValue).max().orElse(1);
            porDia.forEach((dia, total) -> {
                int percentual = (int) Math.round((total * 100.0) / maximo);
                sb.append("<div class='barra-linha'>")
                  .append("<span class='rotulo-dia'>").append(LocalDate.parse(dia).format(FORMATO_DATA_CURTA)).append("</span>")
                  .append("<span class='barra-fundo'><span class='barra-preenchida' style='width:").append(percentual).append("%'></span></span>")
                  .append("<span class='contagem'>").append(total).append("</span>")
                  .append("</div>");
            });
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String secaoAvaliacoes(List<Avaliacao> avaliacoes) {
        StringBuilder sb = new StringBuilder("<div class='secao'><h2>Avaliações recebidas</h2>");

        if (avaliacoes.isEmpty()) {
            sb.append("<p class='vazio'>Nenhuma avaliação recebida no período.</p>");
        } else {
            sb.append("<table><tr><th>Descrição</th><th>Nota</th><th>Urgência</th><th>Data</th></tr>");
            for (Avaliacao avaliacao : avaliacoes) {
                String urgenciaClasse = avaliacao.urgencia().name().toLowerCase(LOCALE_RELATORIO);
                sb.append("<tr>")
                  .append("<td>").append(avaliacao.descricao()).append("</td>")
                  .append("<td>").append(avaliacao.nota()).append("</td>")
                  .append("<td><span class='pill ").append(urgenciaClasse).append("'>")
                  .append(avaliacao.urgencia()).append("</span></td>")
                  .append("<td>").append(avaliacao.dataRegistro().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate())
                  .append("</td>")
                  .append("</tr>");
            }
            sb.append("</table>");
        }

        sb.append(legendaUrgencia());
        sb.append("</div>");
        return sb.toString();
    }

    private String legendaUrgencia() {
        return "<div class='legenda'>"
                + legendaItem("alta", "ALTA — nota 0 a 3")
                + legendaItem("media", "MÉDIA — nota 4 a 6")
                + legendaItem("baixa", "BAIXA — nota 7 a 10")
                + "</div>";
    }

    private String legendaItem(String classe, String texto) {
        return "<span class='legenda-item'><span class='legenda-cor " + classe + "'></span>" + texto + "</span>";
    }

    private String rodape() {
        return "<p class='rodape'>Gerado automaticamente pela plataforma avalie-me</p>";
    }
}
