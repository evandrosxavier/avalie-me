package br.com.fiap.avalieme.domain;

import java.time.Instant;

public record Avaliacao(
        String id,
        String descricao,
        int nota,
        Urgencia urgencia,
        Instant dataRegistro
) {}
