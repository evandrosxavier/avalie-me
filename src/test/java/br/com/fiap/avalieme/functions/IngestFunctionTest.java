package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.HttpResponseMessageMock;
import br.com.fiap.avalieme.repository.InMemoryAvaliacaoRepository;
import br.com.fiap.avalieme.service.AvaliacaoService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class IngestFunctionTest {

    private static final Gson GSON = new Gson();

    private final IngestFunction function = new IngestFunction();
    private InMemoryAvaliacaoRepository repository;
    private AvaliacaoService service;
    private ExecutionContext context;
    private OutputBinding<String> avaliacaoUrgente;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = new InMemoryAvaliacaoRepository();
        repository.limpar();
        service = new AvaliacaoService(repository);

        context = Mockito.mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger("teste"));

        avaliacaoUrgente = Mockito.mock(OutputBinding.class);
    }

    @SuppressWarnings("unchecked")
    private HttpRequestMessage<Optional<String>> mockRequest(String corpo) {
        HttpRequestMessage<Optional<String>> request = Mockito.mock(HttpRequestMessage.class);
        when(request.getBody()).thenReturn(corpo == null ? Optional.empty() : Optional.of(corpo));
        when(request.createResponseBuilder(any(HttpStatus.class)))
                .thenAnswer(invocacao -> new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                        .status(invocacao.getArgument(0)));
        return request;
    }

    private JsonObject corpoComoJson(com.microsoft.azure.functions.HttpResponseMessage resposta) {
        return GSON.fromJson((String) resposta.getBody(), JsonObject.class);
    }

    @Test
    void deveRetornar400QuandoCorpoAusente() {
        HttpRequestMessage<Optional<String>> request = mockRequest(null);

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(400, resposta.getStatusCode());
        assertEquals("validacao-entrada", corpoComoJson(resposta).get("type").getAsString()
                .replaceFirst(".*/erros/", ""));
    }

    @Test
    void deveRetornar400QuandoJsonInvalido() {
        HttpRequestMessage<Optional<String>> request = mockRequest("{nota: abc}");

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(400, resposta.getStatusCode());
    }

    @Test
    void deveRetornar400QuandoNotaAusente() {
        HttpRequestMessage<Optional<String>> request =
                mockRequest("{\"descricao\":\"Curso muito bom, aprendi bastante coisa nova\"}");

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(400, resposta.getStatusCode());
        assertTrue(corpoComoJson(resposta).get("detail").getAsString().contains("nota"));
    }

    @Test
    void deveRetornar400QuandoDescricaoAusente() {
        HttpRequestMessage<Optional<String>> request = mockRequest("{\"nota\":8}");

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(400, resposta.getStatusCode());
        assertTrue(corpoComoJson(resposta).get("detail").getAsString().contains("descricao"));
    }

    @Test
    void deveRetornar400ComRegraDeNegocioQuandoNotaForaDoRange() {
        HttpRequestMessage<Optional<String>> request =
                mockRequest("{\"descricao\":\"Curso muito bom, aprendi bastante coisa nova\",\"nota\":15}");

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(400, resposta.getStatusCode());
        assertEquals("regra-negocio", corpoComoJson(resposta).get("type").getAsString()
                .replaceFirst(".*/erros/", ""));
    }

    @Test
    void deveRetornar400ComRegraDeNegocioQuandoDescricaoCurta() {
        HttpRequestMessage<Optional<String>> request = mockRequest("{\"descricao\":\"curto\",\"nota\":5}");

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(400, resposta.getStatusCode());
        assertEquals("regra-negocio", corpoComoJson(resposta).get("type").getAsString()
                .replaceFirst(".*/erros/", ""));
    }

    @Test
    void deveRetornar201EPersistirQuandoAvaliacaoValida() {
        HttpRequestMessage<Optional<String>> request =
                mockRequest("{\"descricao\":\"Curso muito bom, aprendi bastante coisa nova\",\"nota\":7}");

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(201, resposta.getStatusCode());
        assertEquals("BAIXA", corpoComoJson(resposta).get("urgencia").getAsString());
        assertEquals(1, repository.listarTodas().size());
        Mockito.verifyZeroInteractions(avaliacaoUrgente);
    }

    @Test
    void deveEnfileirarQuandoUrgenciaForAlta() {
        HttpRequestMessage<Optional<String>> request =
                mockRequest("{\"descricao\":\"Curso muito ruim, nao aprendi quase nada de util\",\"nota\":2}");

        var resposta = function.processar(request, avaliacaoUrgente, context, service);

        assertEquals(201, resposta.getStatusCode());
        assertEquals("ALTA", corpoComoJson(resposta).get("urgencia").getAsString());
        Mockito.verify(avaliacaoUrgente).setValue(any());
    }
}
