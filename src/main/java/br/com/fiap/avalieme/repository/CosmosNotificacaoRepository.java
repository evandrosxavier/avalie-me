package br.com.fiap.avalieme.repository;

import br.com.fiap.avalieme.domain.Notificacao;
import br.com.fiap.avalieme.domain.StatusNotificacao;
import br.com.fiap.avalieme.util.ConversorData;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CosmosNotificacaoRepository implements NotificacaoRepository {

    private static final String DATABASE_NAME = "avalieme";
    private static final String CONTAINER_NAME = "notificacoes";

    private final CosmosContainer container;

    public CosmosNotificacaoRepository() {
        Map<String, String> parametros = parseConnectionString(System.getenv("COSMOS_CONNECTION_STRING"));
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(parametros.get("AccountEndpoint"))
                .key(parametros.get("AccountKey"))
                .buildClient();
        this.container = client.getDatabase(DATABASE_NAME).getContainer(CONTAINER_NAME);
    }

    private static Map<String, String> parseConnectionString(String connectionString) {
        return java.util.Arrays.stream(connectionString.split(";"))
                .filter(par -> !par.isBlank())
                .map(par -> par.split("=", 2))
                .collect(Collectors.toMap(par -> par[0], par -> par[1]));
    }

    @Override
    public void salvar(Notificacao notificacao) {
        NotificacaoDocumento documento = new NotificacaoDocumento(
                UUID.randomUUID().toString(),
                notificacao.avaliacaoId(),
                notificacao.descricao(),
                notificacao.nota(),
                notificacao.urgencia(),
                ConversorData.paraIso(notificacao.dataRegistroAvaliacao()),
                ConversorData.paraIso(notificacao.dataEnvio()),
                notificacao.status().name()
        );
        container.createItem(
                documento,
                new PartitionKey(documento.avaliacaoId),
                new CosmosItemRequestOptions()
        );
    }

    private static final class NotificacaoDocumento {
        public String id;
        public String avaliacaoId;
        public String descricao;
        public int nota;
        public String urgencia;
        public String dataRegistroAvaliacao;
        public String dataEnvio;
        public String status;

        public NotificacaoDocumento() {
        }

        public NotificacaoDocumento(String id, String avaliacaoId, String descricao, int nota,
                                    String urgencia, String dataRegistroAvaliacao,
                                    String dataEnvio, String status) {
            this.id = id;
            this.avaliacaoId = avaliacaoId;
            this.descricao = descricao;
            this.nota = nota;
            this.urgencia = urgencia;
            this.dataRegistroAvaliacao = dataRegistroAvaliacao;
            this.dataEnvio = dataEnvio;
            this.status = status;
        }
    }
}
