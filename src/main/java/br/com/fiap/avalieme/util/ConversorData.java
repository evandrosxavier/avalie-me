package br.com.fiap.avalieme.util;

import java.time.Instant;

public final class ConversorData {

    private ConversorData() {
    }

    public static String paraIso(Instant instant) {
        return instant.toString();
    }

    public static Instant paraInstant(String iso) {
        return Instant.parse(iso);
    }
}
