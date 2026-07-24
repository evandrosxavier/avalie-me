package br.com.fiap.avalieme.functions;

import br.com.fiap.avalieme.domain.Notificacao;
import br.com.fiap.avalieme.domain.StatusNotificacao;
import br.com.fiap.avalieme.email.EmailSender;
import br.com.fiap.avalieme.repository.NotificacaoRepository;
import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NotifyFunctionTest {

    private static final String MENSAGEM =
            "{\"id\":\"35bd4e32-7b4b-4cb9-bc1d-acfb036edf67\","
            + "\"descricao\":\"Curso muito ruim, nao aprendi quase nada de util\","
            + "\"nota\":1,"
            + "\"urgencia\":\"ALTA\","
            + "\"dataRegistro\":\"2026-07-24T01:15:37.530024486Z\"}";

    private NotificacaoRepository notificacaoRepository;
    private EmailSender emailSender;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        notificacaoRepository = Mockito.mock(NotificacaoRepository.class);
        emailSender = Mockito.mock(EmailSender.class);

        context = Mockito.mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger("teste"));
    }

    @Test
    void deveSalvarNotificacaoEnviadaQuandoEmailEnviadoComSucesso() {
        when(emailSender.enviar(any(), any(), any())).thenReturn("Succeeded");
        NotifyFunction function = new NotifyFunction(notificacaoRepository, emailSender);

        function.processar(MENSAGEM, context);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        Mockito.verify(notificacaoRepository).salvar(captor.capture());
        assertEquals(StatusNotificacao.ENVIADO, captor.getValue().status());
        assertEquals("35bd4e32-7b4b-4cb9-bc1d-acfb036edf67", captor.getValue().avaliacaoId());
    }

    @Test
    void deveSalvarNotificacaoComFalhaERelancarExcecaoQuandoEnvioDeEmailFalha() {
        when(emailSender.enviar(any(), any(), any()))
                .thenThrow(new RuntimeException("Falha simulada no ACS"));
        NotifyFunction function = new NotifyFunction(notificacaoRepository, emailSender);

        assertThrows(RuntimeException.class, () -> function.processar(MENSAGEM, context));

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        Mockito.verify(notificacaoRepository).salvar(captor.capture());
        assertEquals(StatusNotificacao.FALHA, captor.getValue().status());
        assertEquals("35bd4e32-7b4b-4cb9-bc1d-acfb036edf67", captor.getValue().avaliacaoId());
    }
}
