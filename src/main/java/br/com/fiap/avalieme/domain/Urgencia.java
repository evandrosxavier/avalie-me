package br.com.fiap.avalieme.domain;

public enum Urgencia {
    ALTA, MEDIA, BAIXA;

    private static final int LIMIAR_ALTA = 3;
    private static final int LIMIAR_MEDIA = 6;

    public static Urgencia deNota(int nota) {
        if (nota <= LIMIAR_ALTA) return ALTA;
        if (nota <= LIMIAR_MEDIA) return MEDIA;
        return BAIXA;
    }
}
