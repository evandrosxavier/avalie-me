package br.com.fiap.avalieme.repository;

import br.com.fiap.avalieme.domain.Avaliacao;
import br.com.fiap.avalieme.domain.Urgencia;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CosmosAvaliacaoRepository implements AvaliacaoRepository {

    private static final String DATABASE_NAME = "avalieme";
    private static final String CONTAINER_NAME = "avaliacoes";

    private final CosmosContainer container;

    public CosmosAvaliacaoRepository() {
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
    public void salvar(Avaliacao avaliacao) {
        container.createItem(
                toDocumento(avaliacao),
                new PartitionKey(avaliacao.urgencia().name()),
                new CosmosItemRequestOptions()
        );
    }

    @Override
    public List<Avaliacao> listarTodas() {
        CosmosPagedIterable<AvaliacaoDocumento> resultados = container.queryItems(
                "SELECT * FROM c",
                new CosmosQueryRequestOptions(),
                AvaliacaoDocumento.class
        );

        List<Avaliacao> avaliacoes = new ArrayList<>();
        for (AvaliacaoDocumento documento : resultados) {
            avaliacoes.add(toAvaliacao(documento));
        }
        return avaliacoes;
    }

    private String instantParaIso(Instant instant) {
        return instant.toString();
    }

    private Instant isoParaInstant(String iso) {
        return Instant.parse(iso);
    }

    private AvaliacaoDocumento toDocumento(Avaliacao avaliacao) {
        return new AvaliacaoDocumento(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia().name(),
                instantParaIso(avaliacao.dataRegistro())
        );
    }

    private Avaliacao toAvaliacao(AvaliacaoDocumento documento) {
        return new Avaliacao(
                documento.id,
                documento.descricao,
                documento.nota,
                Urgencia.valueOf(documento.urgencia),
                isoParaInstant(documento.dataRegistro)
        );
    }

    private static final class AvaliacaoDocumento {
        public String id;
        public String descricao;
        public int nota;
        public String urgencia;
        public String dataRegistro;

        public AvaliacaoDocumento() {
        }

        public AvaliacaoDocumento(String id, String descricao, int nota, String urgencia, String dataRegistro) {
            this.id = id;
            this.descricao = descricao;
            this.nota = nota;
            this.urgencia = urgencia;
            this.dataRegistro = dataRegistro;
        }
    }
}
