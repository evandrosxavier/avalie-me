package br.com.fiap.avalieme.service;

import java.util.List;

/**
 * Agrupa todos os erros de validacao encontrados em uma unica requisicao,
 * para que o cliente nao precise corrigir um campo por vez.
 */
public class ValidacaoException extends IllegalArgumentException {

    private final List<String> erros;

    public ValidacaoException(List<String> erros) {
        super(String.join("; ", erros));
        this.erros = List.copyOf(erros);
    }

    public List<String> erros() {
        return erros;
    }
}
