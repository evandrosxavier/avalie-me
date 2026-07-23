package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.domain.Notificacao;
import br.com.fiap.avalieme.domain.StatusNotificacao;
import br.com.fiap.avalieme.dto.AvaliacaoUrgenteMensagem;
import br.com.fiap.avalieme.repository.CosmosNotificacaoRepository;
import br.com.fiap.avalieme.repository.NotificacaoRepository;
import br.com.fiap.avalieme.util.ConversorData;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.google.gson.Gson;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;

import java.time.Instant;

public class NotifyFunction {

    private static final Gson GSON = new Gson();
    private static final String REMETENTE =
            "DoNotReply@b3991415-9ff8-4633-9d9f-ca0a4fb29dcd.azurecomm.net";

    private final NotificacaoRepository notificacaoRepository = new CosmosNotificacaoRepository();

    @FunctionName("notify")
    public void run(
            @QueueTrigger(
                name = "mensagem",
                queueName = "avaliacoes-urgentes",
                connection = "AzureWebJobsStorage")
                String mensagem,
            final ExecutionContext context) {

        AvaliacaoUrgenteMensagem avaliacao = GSON.fromJson(mensagem, AvaliacaoUrgenteMensagem.class);

        context.getLogger().info("Avaliacao urgente recebida - id: " + avaliacao.id()
                + ", descricao: " + avaliacao.descricao()
                + ", urgencia: " + avaliacao.urgencia());

        enviarEmail(avaliacao, context);
    }

    private void enviarEmail(AvaliacaoUrgenteMensagem avaliacao, ExecutionContext context) {
        String connectionString = System.getenv("ACS_CONNECTION_STRING");
        String destinatario = System.getenv("EMAIL_ADMIN");

        try {
            EmailClient emailClient = new EmailClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            EmailMessage email = new EmailMessage()
                    .setSenderAddress(REMETENTE)
                    .setToRecipients(destinatario)
                    .setSubject("Avaliação urgente recebida")
                    .setBodyPlainText(
                            "Descrição: " + avaliacao.descricao() + "\n"
                            + "Urgência: " + avaliacao.urgencia() + "\n"
                            + "Data: " + avaliacao.dataRegistro());

            EmailSendResult resultado = emailClient.beginSend(email).waitForCompletion().getValue();

            context.getLogger().info("E-mail enviado para avaliacao " + avaliacao.id()
                    + " com status " + resultado.getStatus());

            notificacaoRepository.salvar(new Notificacao(
                    avaliacao.id(),
                    avaliacao.descricao(),
                    avaliacao.nota(),
                    avaliacao.urgencia(),
                    ConversorData.paraInstant(avaliacao.dataRegistro()),
                    Instant.now(),
                    StatusNotificacao.ENVIADO
            ));
        } catch (Exception e) {
            context.getLogger().severe("Falha ao enviar e-mail para avaliacao "
                    + avaliacao.id() + ": " + e.getMessage());

            notificacaoRepository.salvar(new Notificacao(
                    avaliacao.id(),
                    avaliacao.descricao(),
                    avaliacao.nota(),
                    avaliacao.urgencia(),
                    ConversorData.paraInstant(avaliacao.dataRegistro()),
                    Instant.now(),
                    StatusNotificacao.FALHA
            ));

            throw new RuntimeException("Falha ao enviar e-mail para avaliacao " + avaliacao.id(), e);
        }
    }
}
