# avalie-me

Plataforma serverless de feedback de cursos desenvolvida como Tech Challenge da Fase 4 da pós-graduação FIAP (ADJT). Alunos avaliam aulas; administradores recebem notificações imediatas para avaliações críticas e um relatório diário com resumo e link público.

---

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENTE (HTTP)                               │
│                   POST /api/avaliacao                               │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     ingest  (HttpTrigger)                           │
│  • Valida nota (0–10) e descrição                                   │
│  • Deriva urgência: ALTA (0–4) / MEDIA (5–7) / BAIXA (8–10)        │
│  • Persiste avaliação no Cosmos DB                                  │
│  • Se ALTA → publica mensagem na Storage Queue                      │
└─────────┬───────────────────────────┬───────────────────────────────┘
          │                           │
          ▼                           ▼
┌──────────────────┐     ┌────────────────────────────────────────────┐
│   Cosmos DB      │     │        Storage Queue                       │
│  (avaliacoes)    │     │      (avaliacoes-urgentes)                 │
└──────────────────┘     └──────────────────┬───────────────────────┘
                                            │
                                            ▼
                         ┌────────────────────────────────────────────┐
                         │        notify  (QueueTrigger)              │
                         │  • Lê mensagem da fila                     │
                         │  • Envia e-mail via ACS                    │
                         │  • Grava Notificacao no Cosmos DB          │
                         │    (snapshot de auditoria: ENVIADO/FALHA)  │
                         └────────────────┬───────────────────────────┘
                                          │
                              ┌───────────┴──────────┐
                              ▼                      ▼
                    ┌──────────────────┐   ┌──────────────────┐
                    │   ACS (e-mail)   │   │   Cosmos DB      │
                    │  (notificação    │   │  (notificacoes)  │
                    │   ao admin)      │   └──────────────────┘
                    └──────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│              report  (TimerTrigger — diário às 8h BRT)              │
│  • Busca todas as avaliações no Cosmos DB                           │
│  • Gera relatório HTML (lista + média + contagens)                  │
│  • Publica HTML no Blob Storage (link público)                      │
│  • Envia link por e-mail via ACS                                    │
└─────────┬───────────────────────────┬───────────────────────────────┘
          │                           │
          ▼                           ▼
┌──────────────────┐     ┌────────────────────────────────────────────┐
│   Cosmos DB      │     │   Blob Storage                             │
│  (avaliacoes)    │     │  (relatorios/relatorio-YYYY-MM-DD.html)    │
└──────────────────┘     └────────────────────────────────────────────┘

                    ┌──────────────────────────────┐
                    │   Application Insights        │
                    │  (telemetria das 3 funções)   │
                    └──────────────────────────────┘

                    ┌──────────────────────────────┐
                    │   GitHub Actions              │
                    │  (deploy automatizado via     │
                    │   service principal)          │
                    └──────────────────────────────┘
```

---

## Modelo de nuvem

**FaaS — Functions as a Service** na **Microsoft Azure**, plano **Consumption** (serverless puro: sem servidor dedicado, cobra-se apenas pelo tempo de execução). As três funções compartilham o mesmo **Function App** (`func-avalieme-dev`) na região **West Central US** — escolhida por disponibilidade de cota do SKU `Y1` na assinatura.

---

## Recursos Azure

| Recurso | Nome | Finalidade |
|---|---|---|
| Function App | `func-avalieme-dev` | hospeda as três funções |
| Cosmos DB | `cosmos-avalieme-dev` | persistência de avaliações e notificações |
| Communication Service | `acs-avalieme-dev` | envio de e-mails |
| Email Communication Service | `acs-email-avalieme-dev` | domínio remetente |
| Storage Account | `funcavaliemedev65741` | fila de mensagens + blobs de relatório |
| Application Insights | `appi-avalieme-dev` | monitoramento e telemetria |
| App Service Plan | `asp-avalieme-dev` | plano Consumption |

---

## Funções

### `ingest` — HttpTrigger
**Trigger:** `POST /api/avaliacao`

Recebe o feedback do aluno, valida, deriva a urgência e persiste no Cosmos DB. Se a urgência for ALTA, publica uma mensagem na fila para notificação assíncrona.

**Request:**
```json
{
  "descricao": "string",
  "nota": 0
}
```

**Regras de urgência:**
| Nota | Urgência |
|---|---|
| 0 – 4 | ALTA |
| 5 – 7 | MEDIA |
| 8 – 10 | BAIXA |

**Responses:** `201 Created` (sucesso) · `400 Bad Request` (payload inválido)

---

### `notify` — QueueTrigger
**Trigger:** mensagem na fila `avaliacoes-urgentes`

Acordada automaticamente quando o `ingest` publica uma avaliação urgente. Envia e-mail ao administrador via ACS e grava um snapshot de auditoria (`Notificacao`) no Cosmos DB com status `ENVIADO` ou `FALHA`.

**Campos do snapshot de auditoria:**
`avaliacaoId` · `descricao` · `nota` · `urgencia` · `dataRegistroAvaliacao` · `dataEnvio` · `status`

---

### `report` — TimerTrigger
**Trigger:** diariamente às 8h (horário de Brasília)  
**Cron:** `0 0 11 * * *` (UTC)

Busca todas as avaliações no Cosmos DB, gera um relatório em HTML com duas seções e publica no Blob Storage com acesso público. Envia o link por e-mail ao administrador.

**Seções do relatório:**
- **Avaliações recebidas:** descrição, urgência e data de cada avaliação
- **Resumo quantitativo:** média das notas, total por urgência, total por dia

**URL do relatório:**
```
https://funcavaliemedev65741.blob.core.windows.net/relatorios/relatorio-YYYY-MM-DD.html
```

---

## Instruções de deploy

### Pré-requisitos
- Java 21
- Maven 3.8+
- Azure CLI autenticado (`az login`)
- Conta Azure com cota disponível para plano Consumption em West Central US

### Variáveis de ambiente obrigatórias no Function App

| Variável | Descrição |
|---|---|
| `COSMOS_CONNECTION_STRING` | Connection string do Cosmos DB |
| `ACS_CONNECTION_STRING` | Connection string do Azure Communication Services |
| `EMAIL_ADMIN` | E-mail do destinatário dos alertas e relatórios |
| `AzureWebJobsStorage` | Connection string da Storage Account (fila + blob) |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | Connection string do Application Insights |

### Deploy manual
```bash
mvn clean package azure-functions:deploy
```

### Deploy automatizado
O repositório possui um workflow GitHub Actions (`.github/workflows/deploy.yml`) com disparo manual (`workflow_dispatch`). Autenticação via service principal armazenado como secret `AZURE_CREDENTIALS` no repositório.

```bash
# No portal GitHub: Actions → Deploy para Azure → Run workflow
```

---

## Monitoramento

O **Application Insights** (`appi-avalieme-dev`, região Central US) coleta automaticamente a telemetria das três funções via variável `APPLICATIONINSIGHTS_CONNECTION_STRING` — nenhuma mudança de código necessária.

Todas as funções emitem logs estruturados via `context.getLogger()`, visíveis em:
- **Portal Azure** → Application Insights → Logs → `traces`
- **Portal Azure** → Function App → Functions → `[nome]` → Monitor

**Consulta útil no Log Analytics:**
```kusto
traces
| where timestamp > ago(1h)
| order by timestamp desc
```

---

## Segurança

- Connection strings armazenadas como **variáveis de ambiente** do Function App (não no código-fonte).
- CI/CD usa **service principal** escopado ao resource group com menor privilégio.
- Blob Storage com acesso público restrito ao container `relatorios` — os demais containers permanecem privados.
- **Key Vault + Managed Identity:** migração das connection strings para cofre planejada (próxima iteração).

---

## Estrutura do projeto

```
src/main/java/br/com/fiap/avalieme/
├── domain/
│   ├── Avaliacao.java          # record imutável da avaliação
│   ├── Notificacao.java        # record imutável do snapshot de auditoria
│   ├── StatusNotificacao.java  # enum ENVIADO | FALHA
│   └── Urgencia.java           # enum ALTA | MEDIA | BAIXA
├── dto/
│   ├── AvaliacaoRequest.java         # entrada do ingest
│   └── AvaliacaoUrgenteMensagem.java # mensagem da fila (bilhete gordo)
├── functions/
│   ├── IngestFunction.java    # HttpTrigger
│   ├── NotifyFunction.java    # QueueTrigger
│   └── ReportFunction.java    # TimerTrigger
├── repository/
│   ├── AvaliacaoRepository.java          # interface
│   ├── CosmosAvaliacaoRepository.java    # impl Cosmos DB
│   ├── InMemoryAvaliacaoRepository.java  # impl para testes
│   ├── NotificacaoRepository.java        # interface
│   ├── CosmosNotificacaoRepository.java  # impl Cosmos DB
│   └── BlobRelatorioRepository.java      # upload HTML + URL pública
├── service/
│   ├── AvaliacaoService.java   # validação e derivação de urgência
│   └── RelatorioService.java   # geração do HTML do relatório
└── util/
    └── ConversorData.java      # conversão Instant ↔ String ISO-8601
```
