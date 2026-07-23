package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import br.com.fiap.avalieme.dto.AvaliacaoRequest;
import br.com.fiap.avalieme.dto.AvaliacaoResponse;
import br.com.fiap.avalieme.dto.AvaliacaoUrgenteMensagem;
import br.com.fiap.avalieme.dto.ErroResponse;
import br.com.fiap.avalieme.repository.CosmosAvaliacaoRepository;
import br.com.fiap.avalieme.service.AvaliacaoService;
import br.com.fiap.avalieme.util.ConversorData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.Optional;

public class IngestFunction {

    private static volatile AvaliacaoService service;
    private static final Gson GSON = new Gson();

    private static AvaliacaoService getService() {
        AvaliacaoService instancia = service;
        if (instancia == null) {
            synchronized (IngestFunction.class) {
                instancia = service;
                if (instancia == null) {
                    instancia = new AvaliacaoService(new CosmosAvaliacaoRepository());
                    service = instancia;
                }
            }
        }
        return instancia;
    }

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

        return processar(request, avaliacaoUrgente, context, getService());
    }

    HttpResponseMessage processar(
            HttpRequestMessage<Optional<String>> request,
            OutputBinding<String> avaliacaoUrgente,
            ExecutionContext context,
            AvaliacaoService service) {

        context.getLogger().info("Avaliacao recebida");

        if (request.getBody().isEmpty() || request.getBody().get().isBlank()) {
            return erro(request, "Corpo da requisição ausente");
        }

        AvaliacaoRequest avaliacaoRequest;
        try {
            avaliacaoRequest = GSON.fromJson(request.getBody().get(), AvaliacaoRequest.class);
        } catch (JsonSyntaxException e) {
            return erro(request, "Corpo da requisição inválido: verifique se é um JSON válido e se nota é um número inteiro");
        }

        if (avaliacaoRequest == null) {
            return erro(request, "Corpo da requisição ausente");
        }

        if (avaliacaoRequest.nota() == null) {
            return erro(request, "nota é obrigatória");
        }

        if (avaliacaoRequest.descricao() == null || avaliacaoRequest.descricao().isBlank()) {
            return erro(request, "descricao é obrigatória");
        }

        try {
            Avaliacao avaliacao = service.registrar(avaliacaoRequest);
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
            return erroRegraNegocio(request, e.getMessage());
        }
    }

    private static HttpResponseMessage erro(HttpRequestMessage<Optional<String>> request, String detail) {
        ErroResponse erroResponse = ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Validação",
                "validacao-entrada",
                detail,
                "/avaliacao");
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body(GSON.toJson(erroResponse))
                .build();
    }

    private static HttpResponseMessage erroRegraNegocio(HttpRequestMessage<Optional<String>> request, String detail) {
        ErroResponse erroResponse = ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Regra de Negócio",
                "regra-negocio",
                detail,
                "/avaliacao");
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body(GSON.toJson(erroResponse))
                .build();
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
