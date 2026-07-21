package br.com.fiap.avalieme.service;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import br.com.fiap.avalieme.dto.AvaliacaoRequest;
import br.com.fiap.avalieme.repository.AvaliacaoRepository;

import java.time.Instant;
import java.util.UUID;

public class AvaliacaoService {

    private static final int TAMANHO_MINIMO_DESCRICAO = 15;

    private final AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    public Avaliacao registrar(AvaliacaoRequest request) {
        if (request.nota() == null) throw new IllegalArgumentException("nota e obrigatoria");
        if (request.nota() < 0 || request.nota() > 10) throw new IllegalArgumentException("nota deve estar entre 0 e 10");
        if (request.descricao() == null || request.descricao().isBlank()) throw new IllegalArgumentException("descricao e obrigatoria");
        if (request.descricao().trim().length() < TAMANHO_MINIMO_DESCRICAO) throw new IllegalArgumentException("descricao muito curta para ser uma avaliacao");

        Avaliacao avaliacao = new Avaliacao(
            UUID.randomUUID().toString(),
            request.descricao(),
            request.nota(),
            Urgencia.deNota(request.nota()),
            Instant.now()
        );
        repository.salvar(avaliacao);
        return avaliacao;
    }
}
