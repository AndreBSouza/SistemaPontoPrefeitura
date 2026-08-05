# Observabilidade (local / desenvolvimento)

Stack de observabilidade para o **Ponto Municipal**, voltada ao ambiente local.
**NAO usar em producao.**

Componentes:

| Servico    | Funcao                                  | URL local                  |
| ---------- | --------------------------------------- | -------------------------- |
| Prometheus | Coleta e armazena metricas              | http://localhost:9090      |
| Grafana    | Dashboards e visualizacao               | http://localhost:3001      |
| Loki       | Armazenamento de logs                   | http://localhost:3100 (API)|
| Promtail   | Coletor de logs dos containers Docker   | (sem UI)                   |

## Como subir

A partir desta pasta (`infra/observability`):

```bash
docker compose -f docker-compose.observability.yml up -d
```

Para acompanhar os logs:

```bash
docker compose -f docker-compose.observability.yml logs -f
```

Para derrubar (preservando os volumes):

```bash
docker compose -f docker-compose.observability.yml down
```

Para derrubar **e apagar** os dados (Prometheus, Grafana e Loki):

```bash
docker compose -f docker-compose.observability.yml down -v
```

> Esta stack e independente do `infra/docker-compose.yml` (postgres, redis,
> rabbitmq, keycloak, minio). Voce pode subir as duas em paralelo; elas usam
> portas e projetos Compose diferentes.

## Acesso

- **Grafana**: http://localhost:3001 — usuario `admin`, senha `admin`.
  Os datasources **Prometheus** e **Loki** ja vem provisionados
  automaticamente (veja `grafana/datasources.yml`), entao basta criar/importar
  os dashboards desejados.
- **Prometheus**: http://localhost:9090 — em *Status > Targets* voce confere se
  o backend esta sendo raspado com sucesso.

## Scrape do backend

O Prometheus esta configurado (em `prometheus.yml`) para raspar:

1. **Ele mesmo** — `localhost:9090`.
2. **O backend Spring Boot** — `host.docker.internal:8080`, no caminho
   `/actuator/prometheus`.

O backend roda **no host** (nao em container), por isso o alvo usa
`host.docker.internal`. No Linux isso so funciona porque o servico
`prometheus` declara `extra_hosts: host.docker.internal:host-gateway` no
compose; no Docker Desktop (Windows/macOS) esse hostname ja existe nativamente.

> Se o seu backend rodar em outra porta, ajuste o alvo `ponto-backend` em
> `prometheus.yml`.

## Nota: dependencia no backend (pom.xml)

Para que o endpoint `/actuator/prometheus` exista, o backend precisa do
Micrometer com o registry do Prometheus. Esta tarefa **nao altera** o `pom.xml`
— a dependencia abaixo deve ser adicionada manualmente pela equipe de backend:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

E expor o endpoint no `application.yml` (exemplo):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ponto-backend
```

Sem essa dependencia o alvo `ponto-backend` aparecera como **DOWN** no
Prometheus — o restante da stack continua funcionando normalmente.

## Coleta de logs (Loki + Promtail)

O Promtail le os logs em arquivo dos containers Docker do host
(`/var/lib/docker/containers`) e envia para o Loki. No Grafana, use o
datasource **Loki** em *Explore* para consultar os logs (por exemplo, filtrando
pelo container do backend quando ele rodar em container).
