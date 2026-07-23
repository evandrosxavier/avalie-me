package br.com.fiap.avalieme.service;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import br.com.fiap.avalieme.dto.AvaliacaoRequest;
import br.com.fiap.avalieme.repository.InMemoryAvaliacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoServiceTest {

    private InMemoryAvaliacaoRepository repository;
    private AvaliacaoService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAvaliacaoRepository();
        repository.limpar();
        service = new AvaliacaoService(repository);
    }

    @Test
    void deveRegistrarAvaliacaoValidaEPersistirNoRepositorio() {
        AvaliacaoRequest request = new AvaliacaoRequest("Curso muito bom, aprendi bastante coisa nova", 8);

        Avaliacao avaliacao = service.registrar(request);

        assertNotNull(avaliacao.id());
        assertEquals(1, repository.listarTodas().size());
        assertEquals(avaliacao, repository.listarTodas().get(0));
    }

    @Test
    void deveLancarExcecaoQuandoNotaForNula() {
        AvaliacaoRequest request = new AvaliacaoRequest("Curso muito bom, aprendi bastante coisa nova", null);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
    }

    @Test
    void deveLancarExcecaoQuandoNotaForMenorQueZero() {
        AvaliacaoRequest request = new AvaliacaoRequest("Curso muito bom, aprendi bastante coisa nova", -1);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
    }

    @Test
    void deveLancarExcecaoQuandoNotaForMaiorQueDez() {
        AvaliacaoRequest request = new AvaliacaoRequest("Curso muito bom, aprendi bastante coisa nova", 11);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
    }

    @Test
    void deveLancarExcecaoQuandoDescricaoForNula() {
        AvaliacaoRequest request = new AvaliacaoRequest(null, 8);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
    }

    @Test
    void deveLancarExcecaoQuandoDescricaoForMuitoCurta() {
        AvaliacaoRequest request = new AvaliacaoRequest("curto", 8);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
    }

    @Test
    void deveDerivarUrgenciaAltaParaNotaMenorOuIgualATres() {
        AvaliacaoRequest request = new AvaliacaoRequest("Curso muito ruim, nao aprendi quase nada de util", 2);

        Avaliacao avaliacao = service.registrar(request);

        assertEquals(Urgencia.ALTA, avaliacao.urgencia());
    }

    @Test
    void deveDerivarUrgenciaMediaParaNotaEntreQuatroESeis() {
        AvaliacaoRequest request = new AvaliacaoRequest("Curso razoavel, esperava mais dos exemplos praticos", 5);

        Avaliacao avaliacao = service.registrar(request);

        assertEquals(Urgencia.MEDIA, avaliacao.urgencia());
    }

    @Test
    void deveDerivarUrgenciaBaixaParaNotaMaiorQueSeis() {
        AvaliacaoRequest request = new AvaliacaoRequest("Curso muito bom, aprendi bastante coisa nova", 9);

        Avaliacao avaliacao = service.registrar(request);

        assertEquals(Urgencia.BAIXA, avaliacao.urgencia());
    }
}
