package br.com.fiap.avalieme.dto;

public record AvaliacaoUrgenteMensagem(
        String id,
        String descricao,
        int nota,
        String urgencia,
        String dataRegistro
) {
}
