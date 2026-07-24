package br.com.fiap.avalieme.email;

public interface EmailSender {
    String enviar(String destinatario, String assunto, String corpo);
}
