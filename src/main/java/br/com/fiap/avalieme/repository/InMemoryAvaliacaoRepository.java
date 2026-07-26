package br.com.fiap.avalieme.repository;

import br.com.fiap.avalieme.domain.Avaliacao;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryAvaliacaoRepository implements AvaliacaoRepository {

    private static final List<Avaliacao> DADOS = new CopyOnWriteArrayList<>();

    @Override
    public void salvar(Avaliacao avaliacao) {
        DADOS.add(avaliacao);
    }

    @Override
    public List<Avaliacao> listarTodas() {
        return List.copyOf(DADOS);
    }

    @Override
    public List<Avaliacao> listarPorPeriodo(Instant inicio, Instant fim) {
        return DADOS.stream()
                .filter(a -> !a.dataRegistro().isBefore(inicio) && a.dataRegistro().isBefore(fim))
                .toList();
    }

    public void limpar() {
        DADOS.clear();
    }
}
