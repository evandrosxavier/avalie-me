package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.repository.AvaliacaoRepository;
import br.com.fiap.avalieme.repository.BlobRelatorioRepository;
import br.com.fiap.avalieme.repository.CosmosAvaliacaoRepository;
import br.com.fiap.avalieme.service.RelatorioService;
import br.com.fiap.avalieme.util.JanelaSemanal;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class ReportFunction {

    private static final String REMETENTE =
            "DoNotReply@b3991415-9ff8-4633-9d9f-ca0a4fb29dcd.azurecomm.net";

    private final AvaliacaoRepository avaliacaoRepository = new CosmosAvaliacaoRepository();
    private final RelatorioService relatorioService = new RelatorioService();
    private final BlobRelatorioRepository blobRelatorioRepository = new BlobRelatorioRepository();

    @FunctionName("report")
    public void run(
            @TimerTrigger(name = "timer", schedule = "0 0 11 * * MON") String timerInfo,
            ExecutionContext context) {

        context.getLogger().info("Iniciando geração do relatório semanal");
        gerarEEnviarRelatorio(context);
    }

    private void gerarEEnviarRelatorio(ExecutionContext context) {
        String connectionString = System.getenv("ACS_CONNECTION_STRING");
        String destinatario = System.getenv("EMAIL_ADMIN");

        try {
            ZoneId fusoSaoPaulo = ZoneId.of("America/Sao_Paulo");
            JanelaSemanal semana = JanelaSemanal.semanaAnteriorA(LocalDate.now(fusoSaoPaulo), fusoSaoPaulo);

            List<Avaliacao> avaliacoes =
                    avaliacaoRepository.listarPorPeriodo(semana.inicio(), semana.fimExclusivo());
            String html = relatorioService.gerarHtml(avaliacoes, semana.primeiroDia(), semana.ultimoDia());

            String nomeArquivo = "relatorio-" + semana.ultimoDia() + ".html";
            String urlRelatorio = blobRelatorioRepository.salvar(nomeArquivo, html);

            context.getLogger().info("Relatório publicado em: " + urlRelatorio);

            EmailClient emailClient = new EmailClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            EmailMessage email = new EmailMessage()
                    .setSenderAddress(REMETENTE)
                    .setToRecipients(destinatario)
                    .setSubject("Relatório Semanal de Avaliações")
                    .setBodyPlainText("O relatório semanal está disponível em:\n\n" + urlRelatorio);

            emailClient.beginSend(email).waitForCompletion();

            context.getLogger().info("E-mail com link do relatório enviado com sucesso");
        } catch (Exception e) {
            context.getLogger().severe("Falha ao gerar ou enviar relatório: " + e.getMessage());
        }
    }
}
