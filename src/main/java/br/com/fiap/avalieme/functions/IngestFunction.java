package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import br.com.fiap.avalieme.dto.AvaliacaoRequest;
import br.com.fiap.avalieme.dto.AvaliacaoUrgenteMensagem;
import br.com.fiap.avalieme.repository.CosmosAvaliacaoRepository;
import br.com.fiap.avalieme.service.AvaliacaoService;
import br.com.fiap.avalieme.util.ConversorData;
import com.google.gson.Gson;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.Optional;

public class IngestFunction {

    private static final AvaliacaoService SERVICE =
            new AvaliacaoService(new CosmosAvaliacaoRepository());
    private static final Gson GSON = new Gson();

    @FunctionName("ingest")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "avaliacao",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<AvaliacaoRequest>> request,
            @QueueOutput(
                name = "avaliacaoUrgente",
                queueName = "avaliacoes-urgentes",
                connection = "AzureWebJobsStorage")
                OutputBinding<String> avaliacaoUrgente,
            final ExecutionContext context) {

        context.getLogger().info("Avaliacao recebida");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Corpo da requisicao ausente").build();
        }

        try {
            Avaliacao avaliacao = SERVICE.registrar(request.getBody().get());
            context.getLogger().info("Avaliacao " + avaliacao.id()
                    + " registrada com urgencia " + avaliacao.urgencia());

            if (avaliacao.urgencia() == Urgencia.ALTA) {
                avaliacaoUrgente.setValue(montarMensagemJson(avaliacao));
            }

            return request.createResponseBuilder(HttpStatus.CREATED)
                    .body("Avaliacao " + avaliacao.id() + " registrada com urgencia " + avaliacao.urgencia())
                    .build();
        } catch (IllegalArgumentException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage()).build();
        }
    }

    private static String montarMensagemJson(Avaliacao avaliacao) {
        AvaliacaoUrgenteMensagem mensagem = new AvaliacaoUrgenteMensagem(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia().name(),
                ConversorData.paraIso(avaliacao.dataRegistro())
        );
        return GSON.toJson(mensagem);
    }
}
