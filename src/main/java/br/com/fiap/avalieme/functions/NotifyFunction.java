package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.domain.Notificacao;
import br.com.fiap.avalieme.domain.StatusNotificacao;
import br.com.fiap.avalieme.dto.AvaliacaoUrgenteMensagem;
import br.com.fiap.avalieme.email.AcsEmailSender;
import br.com.fiap.avalieme.email.EmailSender;
import br.com.fiap.avalieme.repository.CosmosNotificacaoRepository;
import br.com.fiap.avalieme.repository.NotificacaoRepository;
import br.com.fiap.avalieme.util.ConversorData;
import com.google.gson.Gson;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;

import java.time.Instant;

public class NotifyFunction {

    private static final Gson GSON = new Gson();

    private final NotificacaoRepository notificacaoRepository;
    private final EmailSender emailSender;

    public NotifyFunction() {
        this(new CosmosNotificacaoRepository(), new AcsEmailSender());
    }

    NotifyFunction(NotificacaoRepository notificacaoRepository, EmailSender emailSender) {
        this.notificacaoRepository = notificacaoRepository;
        this.emailSender = emailSender;
    }

    @FunctionName("notify")
    public void run(
            @QueueTrigger(
                name = "mensagem",
                queueName = "avaliacoes-urgentes",
                connection = "AzureWebJobsStorage")
                String mensagem,
            final ExecutionContext context) {

        processar(mensagem, context);
    }

    void processar(String mensagem, ExecutionContext context) {
        AvaliacaoUrgenteMensagem avaliacao = GSON.fromJson(mensagem, AvaliacaoUrgenteMensagem.class);

        context.getLogger().info("Avaliacao urgente recebida - id: " + avaliacao.id()
                + ", descricao: " + avaliacao.descricao()
                + ", urgencia: " + avaliacao.urgencia());

        enviarEmail(avaliacao, context);
    }

    private void enviarEmail(AvaliacaoUrgenteMensagem avaliacao, ExecutionContext context) {
        String destinatario = System.getenv("EMAIL_ADMIN");

        try {
            String status = emailSender.enviar(
                    destinatario,
                    "Avaliação urgente recebida",
                    "Descrição: " + avaliacao.descricao() + "\n"
                            + "Urgência: " + avaliacao.urgencia() + "\n"
                            + "Data: " + avaliacao.dataRegistro());

            context.getLogger().info("E-mail enviado para avaliacao " + avaliacao.id()
                    + " com status " + status);

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
