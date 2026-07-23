package br.com.fiap.avalieme.dto;

public record ErroResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance
) {
    private static final String BASE_URL = "https://github.com/evandrosxavier/avalie-me/erros/";

    public static ErroResponse de(int status, String title, String tipo, String detail, String instance) {
        return new ErroResponse(BASE_URL + tipo, title, status, detail, instance);
    }
}
