package br.com.fiap.avalieme.dto;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.util.ConversorData;

public record AvaliacaoResponse(
        String id,
        String descricao,
        int nota,
        String urgencia,
        String dataRegistro
) {
    public static AvaliacaoResponse de(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia().name(),
                ConversorData.paraIso(avaliacao.dataRegistro())
        );
    }
}
