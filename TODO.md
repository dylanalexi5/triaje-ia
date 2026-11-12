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
- [ ] `PATCH /api/tickets/{id}/estado` (con transiciones de D-12)
- [ ] `PATCH /api/tickets/{id}/asignar` (solo rol SOPORTE)
- [ ] `DELETE /api/tickets/{id}` → 204 (lógico, D-03)
- [x] `TicketMapper`: entidad → DTO de respuesta

## Fase 3 · IA
- [x] Interfaz `AnalizadorTicketService` + record `AnalisisTicketResponse` (tal cual el enunciado)
- [x] `ReglasAnalizadorTicketService` (palabras clave, sin red)
- [ ] `GroqAnalizadorTicketService` (prompt, timeout, `response_format` JSON)
- [x] `NormalizadorAnalisis`: valida y limpia la salida de cualquier analizador (D-11)
- [x] `ClasificadorTicket` como orquestador (D-17), por ahora solo con reglas
- [ ] `ClasificadorTicket`: intentar IA primero y caer a reglas ante timeout, error o respuesta inválida
- [ ] Defensa básica contra prompt injection: el texto del usuario va delimitado y el prompt lo declara como dato

## Fase 4 · Errores y validaciones
- [x] `ErrorResponse` con timestamp, status, error, message y path (+ `detalles` en validaciones)
- [x] `GlobalExceptionHandler` base: 400 (validación, enum inválido, JSON mal formado, tipo de parámetro), 404, 405, 409 y 500 genérico
- [ ] Completar el handler con los casos de tickets: técnico inválido, transición de estado, error del proveedor de IA
- [x] `RecursoNoEncontradoException`, `CorreoDuplicadoException`
- [ ] `TecnicoInvalidoException` y mapear `TransicionEstadoInvalidaException`

## Fase 5 · Pruebas (mínimos del enunciado)
- [x] Creación de ticket (service, clasificador mockeado)
- [~] Clasificación con IA: normalización de respuestas "sucias" ✔ — falta la caída al respaldo (fase 3)
- [x] Ticket inexistente → 404 con formato correcto (MockMvc)
- [ ] Técnico sin rol SOPORTE → error
- [ ] Actualización de estado (válida e inválida)

## Fase 6 · Entrega
- [ ] README completo (los 10 puntos de la sección 13)
- [ ] Diagrama (Mermaid: entidades + flujo de creación con IA)
- [ ] Swagger con springdoc **o** colección Postman
- [ ] Datos semilla: 1 ADMIN, 2 SOPORTE, 2 USUARIO (migración `V2`)
- [ ] Revisión final: `git log`, que no haya secretos (`git grep -i "api_key\|password"`)

## Opcionales (solo con el núcleo terminado)
- [ ] Dockerfile + docker-compose (app + Postgres)
- [ ] Paginación y ordenamiento en `GET /api/tickets`
- [ ] Historial de cambios de estado
- [ ] Métricas por categoría y prioridad
