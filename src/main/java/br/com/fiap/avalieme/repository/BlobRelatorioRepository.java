package br.com.fiap.avalieme.repository;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;

public class BlobRelatorioRepository {

    private static final String CONTAINER_NAME = "relatorios";

    private final BlobContainerClient containerClient;

    public BlobRelatorioRepository() {
        String connectionString = System.getenv("AzureWebJobsStorage");
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = serviceClient.getBlobContainerClient(CONTAINER_NAME);
        if (!containerClient.exists()) {
            containerClient.create();
        }
        containerClient.setAccessPolicy(PublicAccessType.BLOB, null);
    }

    public String salvar(String nomeArquivo, String conteudoHtml) {
        BlobClient blobClient = containerClient.getBlobClient(nomeArquivo);
        blobClient.upload(BinaryData.fromString(conteudoHtml), true);
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType("text/html; charset=UTF-8"));
        return blobClient.getBlobUrl();
    }
}
