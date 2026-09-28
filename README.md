# Sistema de Turnos - Grupo 06

Sistema web para la gestión de turnos entre profesionales y pacientes. Permite a los profesionales configurar su agenda y servicios, y a los pacientes reservar, confirmar, reprogramar y cancelar turnos de forma online.

## Estructura del Repositorio

```
/
├── backend/          # API REST en Java/Spring Boot
├── frontend/         # SPA en TypeScript/React
├── ingress/          # Reverse proxy nginx
├── docs/             # Documentación del proyecto, informes de sprints e imágenes
├── .gitlab-ci.yml    # Pipeline CI/CD
└── docker-compose.yml
```

## Tecnologías

| Componente | Tecnología |
|-----------|------------|
| **Backend** | Java 21, Spring Boot 4.0.5, Spring Security, Spring Data JPA, PostgreSQL 15 |
| **Frontend** | TypeScript 5.9, React 19, Vite 7, TanStack Query 5, TanStack Form 1, Wouter, Zod 4 |
| **Infraestructura** | Docker, docker-compose, nginx, GitLab CI |
| **Testing Backend** | JUnit 5, Mockito, MockMvc, JaCoCo (81.7% cobertura) |
| **Testing Frontend** | Vitest 4, happy-dom, Testing Library |

## Requisitos

- [Docker](https://docs.docker.com/get-docker/) y Docker Compose

## Instalación y Ejecución

### Con Docker (producción local)

```bash
# Revisar variables de entorno
cp .env .env.local  # o editar .env directamente

# Iniciar todos los servicios
docker compose up -d --build --remove-orphans
```

La aplicación queda disponible en:
- **Frontend**: http://localhost:20000
- **API**: http://localhost:20000/api
- **Swagger UI**: http://localhost:20000/api/swagger-ui/index.html
- **Adminer (DB)**: http://localhost:20000/adminer
- **Logs**: http://localhost:20000/logs

### Sin Docker (desarrollo)

Cada submódulo tiene su propio *README* con instrucciones detalladas:

- [Backend](./backend/README.md) — `mvn package && java -jar target/*.jar`
- [Frontend](./frontend/README.md) — `npm install && npm run dev`

Requiere una base de datos PostgreSQL accesible.

## Despliegue

El despliegue en la nube se maneja mediante GitLab CI. El pipeline (`.gitlab-ci.yml`) consta de 3 etapas:

1. **test** — ejecuta `mvn verify` (backend) y `npm test` (frontend)
2. **build** — construye y sube las imágenes Docker al registro de GitLab
3. **deploy** — despliega en el servidor cloud

Ambiente de producción: **https://grupo-06.tp1.ingsoft1.fiuba.ar**

![Vista física — despliegue](docs/img/Vista%20F%C3%ADsica%20%E2%80%94%20Despliegue.png)

## Documentación

| Documento | Descripción |
|-----------|-------------|
| [docs/primer entregable.pdf](./docs/primer%20entregable.pdf) | Primer entregable del proyecto |
| [docs/resultados encuesta.pdf](./docs/resultados%20encuesta.pdf) | Resultados de encuesta |
| [docs/user persona.pdf](./docs/user%20persona.pdf) | Dos users persona |
| [docs/mapa empatia.pdf](./docs/mapa%20empatia.pdf) | Dos mapas de empatía |

### Informes por Sprint

| Sprint | Scrum Master | QA |
|--------|-------------|-----|
| Sprint 1 | [PDF](./docs/sprints/sprint%201/Informe%20Scrum%20Master.pdf) | [Markdown](./docs/sprints/sprint%201/Informe%20QA.md) |
| Sprint 2 | [PDF](./docs/sprints/sprint%202/Informe%20Scrum%20Master.pdf) | [PDF](./docs/sprints/sprint%202/InformeTestSprint2.pdf) |
| Sprint 3 | [PDF](./docs/sprints/sprint%203/Informe%20Scrum%20Master%20-%20Sprint%203.pdf) | [PDF](./docs/sprints/sprint%203/Informe%20QA.pdf) |
| Sprint 4 | [PDF](./docs/sprints/sprint%204/Informe%20Scrum%20Master%20-%20Sprint%204.pdf) | [Markdown](./docs/sprints/sprint%204/Informe%20QA.md) |

## Testing

### Backend
- 239 tests (JUnit 5 + Mockito + MockMvc)
- Cobertura: 81.7% instrucciones, 84.9% líneas
- Base de datos en memoria H2 para tests
- Reporte JaCoCo: `backend/target/site/jacoco/index.html`

```bash
cd backend && ./mvnw verify
```

### Frontend
- 5 tests (Vitest + happy-dom)

```bash
cd frontend && npm test
```

