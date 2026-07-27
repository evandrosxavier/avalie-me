package br.com.fiap.avalieme.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ConversorData {

    private static final ZoneId FUSO_SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.of("pt", "BR"));

    private ConversorData() {
    }

    public static String paraIso(Instant instant) {
        return instant.toString();
    }

    public static Instant paraInstant(String iso) {
        return Instant.parse(iso);
    }

    public static String paraDataHoraSaoPaulo(String iso) {
        return paraInstant(iso).atZone(FUSO_SAO_PAULO).format(FORMATO_DATA_HORA);
    }
}
