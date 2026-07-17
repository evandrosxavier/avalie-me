package br.com.fiap.avalieme.repository;

import br.com.fiap.avalieme.domain.Avaliacao;
import java.util.List;

public interface AvaliacaoRepository {
    void salvar(Avaliacao avaliacao);
    List<Avaliacao> listarTodas();
}
