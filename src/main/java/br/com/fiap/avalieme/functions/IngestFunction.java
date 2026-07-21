package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import br.com.fiap.avalieme.dto.AvaliacaoRequest;
import br.com.fiap.avalieme.dto.AvaliacaoResponse;
import br.com.fiap.avalieme.dto.AvaliacaoUrgenteMensagem;
import br.com.fiap.avalieme.repository.CosmosAvaliacaoRepository;
import br.com.fiap.avalieme.service.AvaliacaoService;
import br.com.fiap.avalieme.util.ConversorData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
                HttpRequestMessage<Optional<String>> request,
            @QueueOutput(
                name = "avaliacaoUrgente",
                queueName = "avaliacoes-urgentes",
                connection = "AzureWebJobsStorage")
                OutputBinding<String> avaliacaoUrgente,
            final ExecutionContext context) {

        context.getLogger().info("Avaliacao recebida");

        if (request.getBody().isEmpty() || request.getBody().get().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Corpo da requisicao ausente").build();
        }

        AvaliacaoRequest avaliacaoRequest;
        try {
            avaliacaoRequest = GSON.fromJson(request.getBody().get(), AvaliacaoRequest.class);
        } catch (JsonSyntaxException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Corpo da requisicao invalido: verifique se e um JSON valido e se nota e um numero inteiro").build();
        }

        if (avaliacaoRequest == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Corpo da requisicao ausente").build();
        }

        if (avaliacaoRequest.nota() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("nota e obrigatoria").build();
        }

        if (avaliacaoRequest.descricao() == null || avaliacaoRequest.descricao().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("descricao e obrigatoria").build();
        }

        try {
            Avaliacao avaliacao = SERVICE.registrar(avaliacaoRequest);
            context.getLogger().info("Avaliacao " + avaliacao.id()
                    + " registrada com urgencia " + avaliacao.urgencia());

            if (avaliacao.urgencia() == Urgencia.ALTA) {
                avaliacaoUrgente.setValue(montarMensagemJson(avaliacao));
            }

            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(AvaliacaoResponse.de(avaliacao)))
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
