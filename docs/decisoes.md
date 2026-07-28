# avalie-me — Decisões de Arquitetura

> Tech Challenge FIAP — Fase 4. Registro das decisões técnicas e arquiteturais do projeto, com a justificativa de cada uma.

O **avalie-me** é uma plataforma de feedback: um aluno avalia um curso e, quando a avaliação é crítica (urgência ALTA), o administrador recebe uma notificação por e-mail imediata; diariamente, o administrador também recebe um relatório consolidado. A arquitetura é inteiramente **serverless em Azure**, implementada em **Java puro** (sem framework), com três Azure Functions.

---

## 1. Modelo de nuvem: FaaS (Functions as a Service)

A aplicação usa **Azure Functions** — modelo **FaaS**, a forma mais granular de serverless (frequentemente descrita como uma evolução do PaaS). Não há sistema operacional, runtime ou servidor lógico para gerenciar: o código "acorda" a cada evento e a cobrança é apenas pela execução. Atende diretamente ao requisito de implementar uma solução serverless.

A escolha se justifica por dois motivos complementares:

1. **Natureza da carga de trabalho.** A carga de eventos do avalie-me é **intermitente e imprevisível**, não constante: uma avaliação chega quando um aluno decide enviá-la, uma notificação urgente ocorre esporadicamente, e o relatório roda uma vez por semana. Não há necessidade de um serviço ativo 24 horas por dia esperando requisições — FaaS permite que a infraestrutura escale a zero nos períodos ociosos e suba automaticamente só quando o evento acontece.
2. **Delegação total da infraestrutura ao provedor.** Não há provisionamento de VM, aplicação de patch de sistema operacional, gerenciamento de runtime ou configuração de load balancer — tudo isso é responsabilidade da Azure. Isso reduz o custo operacional (paga-se apenas pelo uso, modelo *pay-per-execution*) e a complexidade de manter o ambiente, além de trazer escalonamento automático (absorve picos sem intervenção manual) e alta disponibilidade nativa (redundância e recuperação de falha ficam a cargo da plataforma).

**Componentes envolvidos na solução**, todos nativos do ecossistema Azure:

- **3 Azure Functions**, cada uma com um gatilho (*trigger*) diferente: `ingest` (**HttpTrigger**), `notify` (**QueueTrigger**) e `report` (**TimerTrigger**).
- **Cosmos DB** — persistência das avaliações e das notificações.
- **Storage Queue** — desacopla `ingest` de `notify` via mensageria assíncrona.
- **Blob Storage** — hospeda o relatório HTML semanal com acesso público.
- **Azure Communication Services (ACS)** — envio de e-mail ao administrador.
- **Application Insights** — monitoramento e telemetria das três funções.
- **Key Vault + Managed Identity** — armazenamento seguro dos segredos de Cosmos e ACS.

## 2. Plano Consumption (e a região resultante)

O Function App roda em **plano Consumption** (serverless de verdade: nenhum servidor dedicado ligado 24/7). A região escolhida foi **West Central US** — não por latência, mas por **disponibilidade de cota** do SKU `Y1` (Consumption): na assinatura utilizada, `Y1` só estava disponível em West Central US e India South Central, com a maioria das demais regiões (incluindo Brazil South) com cota zero. Fica a lição: em cloud, a escolha de região é condicionada pela cota/capacidade real da assinatura, não só pela geografia.

## 3. Java puro, sem framework

Investigou-se o uso de framework antes de decidir por Java puro. **Quarkus + Funqy** cobre apenas o gatilho HTTP no Azure (confirmado pela documentação da Microsoft) — não atende os gatilhos de fila (`notify`) nem timer (`report`). A extensão geral `quarkus-azure-functions` cobre os três gatilhos, mas está em preview/beta. **Java puro** é o caminho oficial e estável, cobre os três gatilhos necessários, e o enunciado do trabalho não exige framework.

## 4. Imutabilidade da avaliação

Uma avaliação, uma vez registrada, não se altera. Essa regra de domínio se reflete em três camadas:
- `Avaliacao` é um **record** (imutável por natureza);
- a persistência usa **`createItem`** (não `upsertItem`) — gravar um id repetido **falha** em vez de sobrescrever, o que é uma proteção e não um inconveniente;
- o repositório **não expõe operação de update**.

## 5. Datas cruzam fronteiras como texto ISO-8601

O worker/SDK do Azure **não serializa `java.time.Instant`** — o module system do Java 21 não abre `java.time` para reflexão. Regra do projeto: **datas cruzam qualquer fronteira (HTTP, banco, fila) como texto ISO-8601**, nunca como `Instant`. A conversão para `Instant` acontece apenas dentro do domínio. A lógica de conversão está centralizada em `util/ConversorData.java` (princípio DRY), reutilizada pelo repositório e pela função `ingest`.

## 6. Cosmos DB — NoSQL de documentos, serverless

- **API NoSQL nativa** (melhor suporte a Java, guarda JSON diretamente, documentação farta).
- **Modo serverless** (paga por uso, coerente com o restante da arquitetura).
- **Container `avaliacoes`, partition key `/urgencia`** — o relatório consulta e agrega por urgência, e os três valores possíveis (ALTA/MÉDIA/BAIXA) distribuem os dados de forma relativamente equilibrada (evitando uma "hot partition"). Regra geral aplicada na escolha da partition key: (1) o campo mais consultado, (2) que distribua os dados de forma equilibrada.

## 7. Padrão de persistência: classe `Documento` interna ao repositório

O SDK do Cosmos precisa de uma classe com construtor vazio e campos acessíveis para desserializar na leitura — e um `record` não possui construtor vazio. A solução foi uma classe `private static` (`AvaliacaoDocumento` / `NotificacaoDocumento`) escondida dentro do próprio repositório, invisível ao restante do código. Isso mantém o domínio limpo e resolve a conversão na fronteira do banco sem introduzir uma camada de mapeamento separada — o que seria uma proteção contra um risco inexistente neste projeto (o banco não vai trocar de tecnologia).

## 8. Inversão de dependência no repositório

A interface `AvaliacaoRepository` tem duas implementações: `CosmosAvaliacaoRepository` (produção) e `InMemoryAvaliacaoRepository` (testes e desenvolvimento local). O domínio conhece apenas o contrato, não o banco. A implementação em memória não é código descartável — é a ferramenta que permite testar o `AvaliacaoService` sem depender do Cosmos real.

## 9. Comunicação assíncrona por fila

A função `ingest` não envia o e-mail diretamente: publica uma mensagem na fila `avaliacoes-urgentes` (apenas quando a urgência é ALTA) e responde `201 Created` imediatamente. A função `notify` consome a fila e faz o envio. Benefícios: quem registra a avaliação não fica bloqueado esperando o e-mail; e se o serviço de e-mail estiver indisponível, a mensagem permanece na fila para reprocessamento. A implementação usa **Azure Storage Queue** — fila simples, nativa, já disponível junto do storage do Function App. O padrão reforça a Responsabilidade Única: `ingest` recebe e grava; `notify` avisa.

## 10. Mensagem "gorda" na fila (Event-Carried State Transfer)

A mensagem da fila carrega a avaliação inteira, não apenas o id (o que seria o padrão "Claim Check"). Justificativas: o volume é pequeno (bem abaixo do limite de 64 KB da Storage Queue); a mensagem é um retrato do momento — reflete a avaliação como estava quando o evento ocorreu; e dá autonomia ao `notify`, que não precisa consultar o banco para montar o e-mail. O Claim Check seria preferível para payloads grandes ou múltiplos consumidores com necessidades distintas. A serialização usa **Gson** — leve, e como a data já trafega como String, não esbarra no problema de serialização do `Instant`.

## 11. `Notificacao` como snapshot de auditoria

A `Notificacao` é gravada em uma coleção própria (`notificacoes`, partition key `/avaliacaoId`), separada da avaliação — assim a avaliação permanece imutável (registrar a data de envio nela exigiria alterá-la).

A `Notificacao` é um snapshot de auditoria autossuficiente: guarda `{ id, avaliacaoId, descricao, nota, urgencia, dataRegistroAvaliacao, dataEnvio, status }`. Descrição, nota e urgência são copiadas da avaliação — uma desnormalização proposital, para que o registro conte a história completa sem depender de dado externo mutável. Isso segue o princípio de que um registro de auditoria deve ser autossuficiente e imutável, garantindo resiliência mesmo diante de mudanças futuras (por exemplo, exclusão de avaliações por LGPD). A `dataRegistroAvaliacao` guardada junto permite medir o atraso de notificação (`dataEnvio − dataRegistro`) sem cruzar coleções.

## 12. Histórico de tentativas append-only

Cada tentativa de envio grava um **novo** registro de `Notificacao`, em vez de atualizar um existente. Cada tentativa é um fato imutável ("às 10h falhou", "às 10h05 enviou") — um log append-only, no espírito de event sourcing. A consulta "esta avaliação foi notificada?" equivale a buscar a última notificação daquele `avaliacaoId` — eficiente, já que todas ficam na mesma partição. Consequência natural: uma avaliação que só é enviada na terceira tentativa gera três registros (FALHA, FALHA, ENVIADO).

## 13. Reprocessamento: retry nativo e poison queue

A função `notify` deixa a exceção propagar em caso de falha de envio, em vez de capturá-la silenciosamente. Isso aciona o mecanismo nativo do Azure Storage Queue: retry automático (5 tentativas) e, esgotadas, a mensagem é movida para a poison queue `avaliacoes-urgentes-poison` — equivalente à dead letter queue de outras plataformas de mensageria. Antes de relançar a exceção, a `Notificacao` é gravada com status FALHA, preservando o histórico.

## 14. `status` como enum

`StatusNotificacao { ENVIADO, FALHA }` é um enum, não uma String — limita os valores possíveis e evita erros de digitação que quebrariam consultas (por exemplo, `"Enviado"` vs. `"ENVIADO"`). É gravado no documento como texto (`.name()`) e reconstruído com `valueOf`, seguindo o mesmo padrão do enum `Urgencia`.

## 15. E-mail via Azure Communication Services (ACS)

O envio de e-mail usa **ACS**, nativo do Azure, em vez de SendGrid ou SMTP — uma escolha que mantém a arquitetura inteiramente dentro do ecossistema Azure e permite integração com Managed Identity. Usa o domínio gerenciado do Azure (subdomínio pronto, sem verificação de DNS). A arquitetura do ACS separa o **Email Communication Service** (registra o domínio remetente) do **Communication Service** (envia o e-mail, detém a connection string), aplicando o princípio de Responsabilidade Única na própria plataforma.

## 16. CI/CD com GitHub Actions

O deploy é automatizado via GitHub Actions: um workflow de disparo manual (`workflow_dispatch`) executa `mvn clean package azure-functions:deploy` em uma VM efêmera do runner, autenticando-se com um service principal escopado ao resource group (princípio do menor privilégio), com as credenciais guardadas como secret do repositório. Atende ao requisito de deploy automatizado dos componentes atualizáveis, e a natureza efêmera do runner é coerente com o espírito serverless do restante do projeto.

## 17. Monitoramento com Application Insights

O Application Insights é conectado ao Function App pela variável `APPLICATIONINSIGHTS_CONNECTION_STRING` — o runtime reconhece esse nome automaticamente e envia toda a telemetria, sem exigir mudança de código. As chamadas a `context.getLogger()` já presentes no código passam a aparecer no painel. Como a região West Central US não suporta App Insights, o recurso foi criado separadamente em Central US — telemetria é um serviço de ingestão remota, e não exige colocalização com a aplicação.

## 18. Segurança de segredos em duas etapas

A estratégia de segurança de segredos foi deliberadamente faseada: primeiro as connection strings ficaram em variáveis de ambiente, o suficiente para validar o fluxo funcionando de ponta a ponta; em seguida, migraram para Key Vault + Managed Identity (decisão 20). A lógica é a mesma do walking skeleton: primeiro provar que o sistema funciona, depois elevar a segurança sobre algo que já está rodando.

## 19. Relatório publicado como HTML no Blob Storage

O relatório gerado pela função `report` é publicado como arquivo HTML no Azure Blob Storage (container `relatorios`, acesso público), e o e-mail enviado ao administrador contém apenas o link para esse arquivo — não o conteúdo inline. Motivações:

- **Apresentação**: HTML renderizado no navegador é muito mais legível do que texto plano no corpo do e-mail, especialmente para tabelas de dados.
- **Reaproveitamento de infraestrutura**: o Blob Storage já existia no projeto (mesma conta usada pela Storage Queue) — sem custo adicional de configuração.
- **Demonstrabilidade**: o link público é um artefato concreto, útil como evidência de funcionamento.
- **Separação de responsabilidades**: o `RelatorioService` gera o HTML; o `BlobRelatorioRepository` publica e devolve a URL; a `ReportFunction` orquestra — cada peça com uma responsabilidade clara.

O `Content-Type: text/html; charset=UTF-8` é definido via `BlobHttpHeaders` após o upload, para que o navegador renderize o arquivo em vez de baixá-lo.

## 20. Segurança de segredos — Key Vault e Managed Identity, com escopo deliberado

`COSMOS_CONNECTION_STRING` e `ACS_CONNECTION_STRING` foram migradas para **Azure Key Vault**, lidas pelo Function App via **Managed Identity** (system-assigned) — nenhuma das duas existe mais em texto puro na configuração. A chave primária do Cosmos, que havia sido exposta durante o desenvolvimento, foi regenerada antes da migração.

`AzureWebJobsStorage` (a Storage Account usada pelas filas e pelo Blob de relatórios) permaneceu como connection string em variável de ambiente — uma decisão consciente, não uma lacuna. Motivos:

- Não carrega dado sensível de negócio como as outras duas connection strings — não expõe avaliações de alunos nem credenciais de e-mail, apenas infraestrutura interna do próprio runtime.
- Migrá-la exigiria mais do que trocar a variável: o binding de fila já suportaria identidade gerenciada via `AzureWebJobsStorage__accountName`, mas o `BlobRelatorioRepository` lê a connection string diretamente no código — seria necessário reescrever para `DefaultAzureCredential`, adicionar a dependência `azure-identity`, conceder papéis RBAC extras e revalidar todo o caminho de geração de relatório.
- O retorno em nota é marginal, já que o padrão Key Vault + Managed Identity já foi demonstrado com Cosmos e ACS, frente ao risco de regressão perto do prazo de entrega.

Em produção real, seria o próximo passo natural de hardening. Neste projeto, o custo/benefício não compensou dado o prazo — saber até onde vale endurecer a segurança é parte da decisão arquitetural, não apenas aplicar o máximo possível de controles disponíveis.

## 21. Validação acumulada em uma única resposta

A validação original interrompia no primeiro erro encontrado: uma requisição com corpo `{}` respondia apenas `"nota é obrigatória"`, escondendo que a `descricao` também faltava. Funcionalmente correto, mas obriga o cliente a descobrir os problemas um por vez, uma requisição de cada vez.

As duas camadas passaram a **acumular** os erros. Na borda HTTP a lista é montada direto na `IngestFunction`; no domínio, o `AvaliacaoService` agrupa os erros e os transporta na `ValidacaoException`. A exceção estende `IllegalArgumentException` de propósito — o catch existente na função continua válido, e o tratamento específico é um `catch` adicional antes dele, sem quebrar quem já dependia do comportamento anterior.

O formato RFC 9457 acomoda isso sem invenção: o `detail` traz o resumo legível e o campo `errors` lista cada problema separadamente. O campo é omitido quando não há lista aplicável (corpo ausente, JSON malformado), em vez de aparecer como array vazio — ausência e vazio significam coisas diferentes.

## 22. Janela do relatório: semana civil fechada, não janela móvel

A primeira versão do `report` calculava o período como `Instant.now()` menos 7 dias. Rodando na segunda-feira às 8h, isso cobria da segunda anterior 8h até a segunda atual 8h — uma janela **móvel**, ancorada no instante da execução. Dois defeitos decorriam disso: avaliações registradas na segunda-feira antes das 8h entravam no relatório da própria semana e não apareciam no da semana seguinte; e reprocessar o relatório em outro dia produzia um recorte diferente.

A janela passou a ser derivada do **calendário** (`JanelaSemanal`): a partir da data de referência no fuso `America/Sao_Paulo`, recua até a segunda-feira da semana corrente e devolve a semana imediatamente anterior — segunda 00:00 (inclusivo) até a segunda seguinte 00:00 (exclusivo). O dia da execução nunca entra no recorte.

O fim exclusivo casa com o `>= @inicio AND < @fim` que a consulta ao Cosmos já usava, de modo que nenhum registro é contado duas vezes nem se perde na fronteira entre semanas. E como o resultado não depende mais do horário do disparo, o relatório virou uma função pura da semana: reprocessá-lo em qualquer dia produz exatamente o mesmo documento — propriedade que o teste `JanelaSemanalTest` fixa explicitamente.

---

## Lições técnicas de armadilhas encontradas

- **Cota de App Service:** o App Service Plan usa a cota do provider `Microsoft.Web`, separada da `Microsoft.Compute`. Em assinaturas recém-migradas de Free Trial, a cota do `Microsoft.Web` pode estar zerada. O SKU do plano Consumption é `Y1 VMs`.
- **Resource group já existente prevalece na região:** a tag `<region>` do `pom.xml` só é aplicada quando o plugin cria o resource group do zero; se ele já existe, a região dele prevalece.
- **Remetente ACS:** o endereço correto não contém `.us3.` (isso é rota interna). A fonte da verdade é a tela Email → Domínios no portal, não o cabeçalho do e-mail recebido.
- **App Insights em West Central US:** não é suportado — por isso `<disableAppInsights>true</disableAppInsights>` no `pom.xml`, com o recurso criado separadamente em outra região.
- **Deploy manual precisa recompilar:** o goal `azure-functions-maven-plugin:deploy` apenas empacota o que já está em `target/`, sem rodar `compile`/`package`. Executar somente `mvn azure-functions:deploy` após alterar código publica silenciosamente o jar antigo, sem qualquer erro ou aviso — o problema só aparece ao testar o comportamento em produção. O comando correto para deploy manual é `mvn clean package azure-functions:deploy`.
