# Project Gaps — E-Commerce Microservices Saga Orchestrator

> Documento de rastreamento do que falta para concluir o projeto.
> Última atualização: 2026-03-25

---

## Status Geral

| Dimensão               | Score  |
|------------------------|--------|
| Funcionalidade core    | 90%    |
| Testes                 | 5%     |
| Segurança              | 20%    |
| Infraestrutura         | 10%    |
| Observabilidade        | 5%     |
| Documentação           | 30%    |
| Resiliência            | 40%    |
| **Total estimado**     | ~30%   |

---

## P0 — Bloqueante (deve ser feito antes de qualquer deploy)

### 1. `SagaRecoveryJob` incompleto
**Arquivo:** `gateway/gateway/src/main/java/.../saga/SagaRecoveryJob.java`

**Problema atual:** O job apenas marca a saga como `FAILED`. Não dispara compensação.

**O que falta:**
- [ ] Usar o campo `payload` de `SagaState` para persistir os `cartItems` no momento da reserva de inventário
- [ ] Lógica de roteamento por `sagaType` (`CHECKOUT` vs `ORDER_CANCELLATION`)
- [ ] Compensação por step para CHECKOUT:
  - `RESERVE_INVENTORY` → só marcar FAILED (nada reservado ainda com sucesso)
  - `CREATE_ORDER` → liberar inventário (`/api/v1/inventory/release`)
  - `PROCESS_PAYMENT` → cancelar pedido + liberar inventário
  - `SCHEDULE_DELIVERY` → cancelar pedido + liberar inventário + reembolsar pagamento
  - `CHECKOUT_CART` → cancelar entrega + cancelar pedido + liberar inventário + reembolsar
- [ ] Compensação para ORDER_CANCELLATION (saga stuck = ação parcialmente executada):
  - `CANCEL_ORDER` → reverter status para PENDING
  - `RELEASE_INVENTORY` / `REFUND_PAYMENT` / `CANCEL_DELIVERY` → sem compensação reversa possível; apenas registrar e marcar como FAILED
- [ ] Controle de retry: evitar reprocessar a mesma saga mais de N vezes
- [ ] Status intermediário `RECOVERING` para sagas em processo de compensação pelo job

**Ver plano detalhado:** [seção de planejamento abaixo](#plano-sagarecoveryjob)

---

### 2. Validação de entrada ausente em todos os DTOs
**Arquivos afetados:** todos os DTOs de todos os microserviços

- [ ] Adicionar `@NotNull`, `@NotBlank`, `@Size`, `@Positive` nos campos
- [ ] Adicionar `@Valid @RequestBody` nos controllers
- [ ] Tratar `MethodArgumentNotValidException` no `GlobalExceptionHandler`

---

### 3. Segredos hardcoded
**Arquivos:**
- `gateway/gateway/src/main/resources/application.yml` — JWT secret em texto plano
- `docker-compose.yml` — credenciais do banco em texto plano

- [ ] Mover para variáveis de ambiente (`${JWT_SECRET}`, `${DB_PASSWORD}`)
- [ ] Documentar variáveis necessárias em `.env.example`

---

### 4. CI/CD inexistente
- [ ] Criar `.github/workflows/ci.yml` com: build Maven, testes, lint
- [ ] (Opcional) pipeline de build e push de imagem Docker

---

### 5. Migrations de banco versionadas
- [ ] Adicionar Flyway ou Liquibase em todos os microserviços
- [ ] Hoje apenas o Gateway tem `schema.sql`; os outros dependem de `ddl-auto=update` do Hibernate

---

### 6. README vazio
**Arquivo:** `README.md` (2 linhas atualmente)
- [ ] Setup local (pré-requisitos, como rodar com `docker-compose`)
- [ ] Descrição das rotas expostas pelo Gateway
- [ ] Variáveis de ambiente necessárias

---

## P1 — Alta Prioridade

### 7. Testes: 0% de cobertura real
**Situação:** todos os arquivos `*Tests.java` contêm apenas `contextLoads()`.

- [ ] Testes unitários para `CheckoutService` (happy path + cada cenário de falha)
- [ ] Testes unitários para `OrderCancellationService`
- [ ] Testes unitários para `SagaRecoveryJob` (cada step → compensação correta)
- [ ] Testes unitários para `JwtAuthenticationFilter`
- [ ] Testes unitários para services dos microserviços (Order, Payment, Inventory, Cart, Delivery)
- [ ] Testes de integração com WebClient mockado (MockWebServer / WireMock)
- [ ] Testes de repositório com Testcontainers (PostgreSQL)

---

### 8. Circuit Breaker ausente
**Mencionado na arquitetura, não implementado.**
- [ ] Adicionar `resilience4j-reactor` no `pom.xml` do Gateway
- [ ] Configurar `@CircuitBreaker` nos WebClient calls de `CheckoutService` e `OrderCancellationService`
- [ ] Definir thresholds no `application.yml`

---

### 9. Observabilidade
- [ ] Distributed tracing: Spring Cloud Sleuth + Zipkin (ou Micrometer Tracing)
- [ ] Métricas: Micrometer + Prometheus
- [ ] Health checks: `spring-boot-actuator` configurado em todos os serviços
- [ ] Logs estruturados: adicionar `traceId`/`sagaId` nos logs existentes

---

### 10. Documentação de API
- [ ] Adicionar `springdoc-openapi` em cada microserviço
- [ ] Expor Swagger UI via Gateway ou individualmente

---

### 11. Kubernetes manifests
- [ ] `Deployment`, `Service`, `ConfigMap`, `Secret` para cada serviço
- [ ] `Ingress` para o Gateway
- [ ] `PersistentVolumeClaim` para o PostgreSQL

---

## P2 — Melhorias

### 12. Retry policy nos WebClient calls
- [ ] Adicionar `.retryWhen(Retry.backoff(...))` nos calls críticos do Gateway
- [ ] Diferenciar erros retryable (5xx, timeout) de não retryable (4xx)

### 13. Outbox Pattern
- [ ] Garantir que o estado da saga e as chamadas downstream sejam atômicos
- [ ] Risco atual: saga salva como `IN_PROGRESS` mas chamada downstream falha silenciosamente

### 14. Idempotência na criação de pedidos
- [ ] Aceitar `Idempotency-Key` no header do checkout
- [ ] Evitar criar pedido duplicado em retries

### 15. CORS restrito
- [ ] Substituir `allowedOrigins("*")` por lista explícita de origens confiáveis

### 16. Mutual TLS / autenticação serviço-a-serviço
- [ ] Hoje os microserviços aceitam qualquer request; adicionar token de serviço ou mTLS

---

## Plano: SagaRecoveryJob {#plano-sagarecoveryjob}

### Contexto

O `SagaRecoveryJob` roda em cron e encontra sagas presas (não finalizadas e sem atualização há N minutos via `findStuckSagas`).

**Hoje (comportamento incompleto):**
```java
saga.setStatus(SagaState.STATUS_FAILED);
saga.setErrorMessage("Marked as FAILED by recovery job - stuck in step: " + saga.getCurrentStep());
repository.save(saga);
```

**Problema:** saga fica como `FAILED` mas os recursos já alocados (inventário reservado, pedido criado, pagamento processado) nunca são liberados/compensados.

---

### Pré-requisito: popular `SagaState.payload`

O campo `payload` (String, JSON) existe em `SagaState` mas nunca é preenchido.

Para o recovery funcionar, o `CheckoutService` precisa serializar os `cartItems` no payload **antes** de reservar o inventário, pois o Recovery Job não tem como buscar o carrinho do usuário depois (o cart pode ter sido modificado).

**Onde adicionar (em `CheckoutService.executeCheckoutPipeline`):**
```
RESERVE_INVENTORY step → serializar cartItems como JSON → salvar em saga.payload
```

---

### Matriz de compensação por step (CHECKOUT)

| `currentStep`        | Recursos alocados                          | Ação de compensação                                               |
|----------------------|--------------------------------------------|-------------------------------------------------------------------|
| `INITIATED`          | Nenhum                                     | Apenas marcar FAILED                                             |
| `GET_CART`           | Nenhum                                     | Apenas marcar FAILED                                             |
| `RESERVE_INVENTORY`  | Inventário pode estar parcialmente reservado | Liberar inventário (items do payload) + marcar FAILED           |
| `CREATE_ORDER`       | Inventário reservado                       | Cancelar pedido (se `orderId` presente) + liberar inventário     |
| `PROCESS_PAYMENT`    | Inventário + Pedido criado                 | Reembolsar pagamento + cancelar pedido + liberar inventário      |
| `SCHEDULE_DELIVERY`  | Inventário + Pedido + Pagamento            | Cancelar entrega + reembolsar + cancelar pedido + liberar inventário |
| `CHECKOUT_CART`      | Tudo alocado                               | Cancelar entrega + reembolsar + cancelar pedido + liberar inventário |

---

### Matriz de compensação por step (ORDER_CANCELLATION)

| `currentStep`        | Situação                                   | Ação de compensação                          |
|----------------------|--------------------------------------------|----------------------------------------------|
| `GET_ORDER`          | Nenhuma alteração feita                    | Apenas marcar FAILED                        |
| `CANCEL_ORDER`       | Status do pedido pode estar CANCELLED      | Tentar reverter para PENDING; se falhar, logar e marcar FAILED |
| `RELEASE_INVENTORY`  | Pedido cancelado, inventário pode já liberado | Não há reversão segura; marcar FAILED + alertar |
| `REFUND_PAYMENT`     | Inventário liberado                        | Não há reversão segura; marcar FAILED + alertar |
| `CANCEL_DELIVERY`    | Pagamento reembolsado                      | Não há reversão segura; marcar FAILED + alertar |

---

### Estrutura da implementação

**Novos componentes necessários:**

1. **`SagaCompensationRouter`** (novo `@Component`)
   - Recebe uma `SagaState` stuck
   - Roteia para `CheckoutCompensationHandler` ou `CancellationCompensationHandler`
   - Retorna `Mono<SagaState>` com estado final

2. **`CheckoutCompensationHandler`** (novo `@Component`)
   - Métodos por step: `compensateFromReserveInventory`, `compensateFromCreateOrder`, etc.
   - Reutiliza WebClients já configurados

3. Atualizar **`SagaRecoveryJob`**
   - Substituir o `flatMap` de `FAILED` simples por chamada ao `SagaCompensationRouter`
   - Adicionar controle de retry (campo `retryCount` em `SagaState` ou via lógica de tempo)

4. Atualizar **`SagaState`**
   - Adicionar campo `retryCount` (int) para evitar loop infinito de recovery
   - Campo `payload` já existe, só precisa ser preenchido

5. Atualizar **`CheckoutService`**
   - Salvar `cartItems` serializados como JSON no `payload` antes de `RESERVE_INVENTORY`

6. Atualizar **`schema.sql`**
   - Adicionar coluna `retry_count INTEGER DEFAULT 0`

---

### Fluxo do Recovery Job (novo)

```
SagaRecoveryJob.recoverStuckSagas()
  └─ findStuckSagas(threshold)
       └─ para cada saga stuck:
            ├─ se retryCount >= MAX_RETRIES → marcar FAILED definitivo, não compensar
            └─ senão:
                 ├─ marcar status = RECOVERING
                 ├─ incrementar retryCount
                 ├─ SagaCompensationRouter.compensate(saga)
                 │    ├─ sagaType == CHECKOUT → CheckoutCompensationHandler
                 │    └─ sagaType == ORDER_CANCELLATION → CancellationCompensationHandler
                 └─ resultado:
                      ├─ sucesso → status = COMPENSATED
                      └─ falha → status = FAILED + logar erro
```

---

### Arquivos a criar/modificar

| Ação     | Arquivo                                                    |
|----------|------------------------------------------------------------|
| Criar    | `saga/SagaCompensationRouter.java`                        |
| Criar    | `saga/CheckoutCompensationHandler.java`                   |
| Criar    | `saga/CancellationCompensationHandler.java`               |
| Modificar | `saga/SagaRecoveryJob.java`                              |
| Modificar | `entity/SagaState.java` (adicionar `retryCount`)         |
| Modificar | `service/CheckoutService.java` (persistir payload)       |
| Modificar | `config/R2dbcConfig.java` / `schema.sql` (coluna retry_count) |
| Modificar | `application.yml` (adicionar `app.saga.recovery.max-retries`) |
