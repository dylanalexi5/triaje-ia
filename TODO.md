# TODO

Lista viva. Se marca al terminar y se commitea junto con el cambio.

## Fase 1 · Base
- [ ] Proyecto Maven + Maven Wrapper + `application.yml` con variables de entorno
- [ ] Enums: `Rol`, `Categoria`, `Prioridad`, `Estado`, `OrigenAnalisis`
- [ ] Entidades `Usuario` y `Ticket` (dos relaciones N:1 hacia `Usuario`)
- [ ] Migración `V1__esquema_inicial.sql` (PK, FK, CHECK, índices)
- [ ] Repositorios (`TicketRepository` con `JpaSpecificationExecutor`)
- [ ] Verificado: la app arranca y Hibernate valida el esquema

## Fase 2 · Usuarios y tickets (sin IA)
- [ ] `POST /api/usuarios`, `GET /api/usuarios/{id}`, `GET /api/usuarios` (password con BCrypt, nunca en la respuesta)
- [ ] `POST /api/tickets` → 201 + `Location`
- [ ] `GET /api/tickets` con filtros opcionales `estado`, `categoria`, `prioridad`, `usuarioId`
- [ ] `GET /api/tickets/{id}`
- [ ] `PATCH /api/tickets/{id}/estado` (con transiciones de D-12)
- [ ] `PATCH /api/tickets/{id}/asignar` (solo rol SOPORTE)
- [ ] `DELETE /api/tickets/{id}` → 204 (lógico, D-03)
- [ ] `TicketMapper`: entidad → DTO de respuesta

## Fase 3 · IA
- [ ] Interfaz `AnalizadorTicketService` + record `AnalisisTicketResponse` (tal cual el enunciado)
- [ ] `ReglasAnalizadorTicketService` (palabras clave, sin red)
- [ ] `GroqAnalizadorTicketService` (prompt, timeout, `response_format` JSON)
- [ ] Normalizador y validador de la salida del modelo (D-11)
- [ ] `AnalizadorConRespaldo` (Decorator, D-10) como bean `@Primary`
- [ ] Defensa básica contra prompt injection: el texto del usuario va delimitado y el prompt lo declara como dato

## Fase 4 · Errores y validaciones
- [ ] `ErrorResponse` con timestamp, status, error, message y path
- [ ] `GlobalExceptionHandler`: 400 (validación, enum inválido, JSON mal formado), 404, 409, 422 y 500 genérico sin detalles internos
- [ ] Excepciones propias: `RecursoNoEncontradoException`, `TecnicoInvalidoException`, `TransicionEstadoInvalidaException`

## Fase 5 · Pruebas (mínimos del enunciado)
- [ ] Creación de ticket (service, IA mockeada)
- [ ] Clasificación con IA (normalización de una respuesta "sucia" del modelo + caída al respaldo)
- [ ] Ticket inexistente → 404 con formato correcto (MockMvc)
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
