# TODO

Lista viva. Se marca al terminar y se commitea junto con el cambio.

## Fase 1 · Base
- [x] Proyecto Maven + Maven Wrapper + `application.yml` con variables de entorno
- [x] Enums: `Rol`, `Categoria`, `Prioridad`, `Estado`, `OrigenAnalisis`
- [x] Entidades `Usuario` y `Ticket` (dos relaciones N:1 hacia `Usuario`)
- [x] Migración `V1__esquema_inicial.sql` (PK, FK, CHECK, índices)
- [x] Repositorios (`TicketRepository` con `JpaSpecificationExecutor`)
- [x] Verificado: la app arranca y Hibernate valida el esquema

## Fase 2 · Usuarios y tickets (sin IA)
- [x] `POST /api/usuarios`, `GET /api/usuarios/{id}`, `GET /api/usuarios?rol=` (password con BCrypt, nunca en la respuesta)
- [x] `POST /api/tickets` → 201 + `Location` (clasificación provisional por reglas)
- [x] `GET /api/tickets` con filtros opcionales `estado`, `categoria`, `prioridad`, `usuarioId` (1 sola consulta SQL, sin N+1)
- [x] `GET /api/tickets/{id}`
- [x] `PATCH /api/tickets/{id}/estado` (con transiciones de D-12, 409 si no se permite)
- [x] `PATCH /api/tickets/{id}/asignar` (solo rol SOPORTE; un ticket CERRADO no se reasigna)
- [x] `DELETE /api/tickets/{id}` → 204 (lógico, D-03)
- [x] `TicketMapper`: entidad → DTO de respuesta

## Fase 3 · IA
- [x] Interfaz `AnalizadorTicketService` + record `AnalisisTicketResponse` (tal cual el enunciado)
- [x] `ReglasAnalizadorTicketService` (palabras clave, sin red)
- [x] `GroqAnalizadorTicketService` (prompt validado contra la API, timeout, `response_format` JSON, 1 reintento ante 429/5xx)
- [x] `NormalizadorAnalisis`: valida y limpia la salida de cualquier analizador (D-11)
- [x] `ClasificadorTicket` como orquestador (D-17), por ahora solo con reglas
- [x] `ClasificadorTicket`: intentar IA primero y caer a reglas ante timeout, error o respuesta inválida
- [x] Defensa básica contra prompt injection: el texto del usuario va delimitado y el prompt lo declara como dato (probado con Groq real)

## Fase 4 · Errores y validaciones
- [x] `ErrorResponse` con timestamp, status, error, message y path (+ `detalles` en validaciones)
- [x] `GlobalExceptionHandler` base: 400 (validación, enum inválido, JSON mal formado, tipo de parámetro), 404, 405, 409 y 500 genérico
- [x] Handler completo: técnico inválido (400), transición y ticket cerrado (409); los errores de la IA se resuelven con el respaldo (D-21)
- [x] `RecursoNoEncontradoException`, `CorreoDuplicadoException`
- [x] `TecnicoInvalidoException`, `TicketCerradoException` y `TransicionEstadoInvalidaException` mapeadas

## Fase 5 · Pruebas (mínimos del enunciado)
- [x] Creación de ticket (service, clasificador mockeado)
- [x] Clasificación con IA (`ClasificadorTicketTest`): resultado de la IA normalizado, caída al respaldo e IA que inventa una categoría
- [x] Ticket inexistente → 404 con formato correcto (MockMvc)
- [x] Técnico sin rol SOPORTE → 400 (service + MockMvc)
- [x] Actualización de estado válida e inválida, más las 16 transiciones en `EstadoTest`

## Fase 6 · Entrega
- [ ] README completo (los 10 puntos de la sección 13)
- [ ] Diagrama (Mermaid: entidades + flujo de creación con IA)
- [ ] Swagger con springdoc **o** colección Postman
- [x] Datos semilla: 1 ADMIN, 2 SOPORTE, 2 USUARIO (`db/demo/V2`, se apaga con `FLYWAY_LOCATIONS`)
- [ ] Revisión final: `git log`, que no haya secretos (`git grep -i "api_key\|password"`)

## Deuda técnica detectada
- [ ] Prueba que levante el contexto completo de Spring (habría detectado D-22). Necesita una base real: Testcontainers (requiere Docker) o un perfil de pruebas con PostgreSQL.

## Opcionales (solo con el núcleo terminado)
- [ ] Dockerfile + docker-compose (app + Postgres)
- [ ] Paginación y ordenamiento en `GET /api/tickets`
- [ ] Historial de cambios de estado
- [ ] Métricas por categoría y prioridad
