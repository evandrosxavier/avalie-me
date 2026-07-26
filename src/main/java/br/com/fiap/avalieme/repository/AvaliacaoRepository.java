package br.com.fiap.avalieme.repository;

import br.com.fiap.avalieme.domain.Avaliacao;
import java.time.Instant;
import java.util.List;

public interface
AvaliacaoRepository {
    void salvar(Avaliacao avaliacao);
    List<Avaliacao> listarTodas();
    List<Avaliacao> listarPorPeriodo(Instant inicio, Instant fim);
}
