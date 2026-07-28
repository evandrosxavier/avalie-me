package br.com.fiap.avalieme.dto;

import java.util.List;

public record ErroResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<String> errors
) {
    private static final String BASE_URL = "https://github.com/evandrosxavier/avalie-me/erros/";

    public static ErroResponse de(int status, String title, String tipo, String detail, String instance) {
        return de(status, title, tipo, detail, instance, null);
    }

    public static ErroResponse de(int status, String title, String tipo, String detail, String instance,
                                  List<String> errors) {
        return new ErroResponse(BASE_URL + tipo, title, status, detail, instance, errors);
    }
}
