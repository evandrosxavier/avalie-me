package br.com.fiap.avalieme.email;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;

public class AcsEmailSender implements EmailSender {

    private static final String REMETENTE =
            "DoNotReply@b3991415-9ff8-4633-9d9f-ca0a4fb29dcd.azurecomm.net";

    @Override
    public String enviar(String destinatario, String assunto, String corpo) {
        String connectionString = System.getenv("ACS_CONNECTION_STRING");

        EmailClient emailClient = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        EmailMessage email = new EmailMessage()
                .setSenderAddress(REMETENTE)
                .setToRecipients(destinatario)
                .setSubject(assunto)
                .setBodyPlainText(corpo);

        EmailSendResult resultado = emailClient.beginSend(email).waitForCompletion().getValue();
        return resultado.getStatus().toString();
    }
}
