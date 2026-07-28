package br.com.fiap.avalieme.service;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import br.com.fiap.avalieme.dto.AvaliacaoRequest;
import br.com.fiap.avalieme.repository.AvaliacaoRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AvaliacaoService {

    private static final int TAMANHO_MINIMO_DESCRICAO = 15;

    private final AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    public Avaliacao registrar(AvaliacaoRequest request) {
        List<String> erros = new ArrayList<>();

        if (request.nota() == null) erros.add("nota e obrigatoria");
        else if (request.nota() < 0 || request.nota() > 10) erros.add("nota deve estar entre 0 e 10");

        if (request.descricao() == null || request.descricao().isBlank()) erros.add("descricao e obrigatoria");
        else if (request.descricao().trim().length() < TAMANHO_MINIMO_DESCRICAO) erros.add("descricao muito curta para ser uma avaliacao");

        if (!erros.isEmpty()) throw new ValidacaoException(erros);

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
