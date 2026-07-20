package br.com.fiap.avalieme.domain;

import java.time.Instant;

public record Notificacao(
        String avaliacaoId,
        String descricao,
        int nota,
        String urgencia,
        Instant dataRegistroAvaliacao,
        Instant dataEnvio,
        StatusNotificacao status
) {}
