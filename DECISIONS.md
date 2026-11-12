# Decisiones técnicas

Registro corto de decisiones (estilo ADR liviano). Cada una dice qué se
decidió, por qué, y qué se descartó. Si una decisión cambia, no se borra:
se marca como reemplazada y se agrega la nueva.

---

### D-01 · Spring Boot 4.1 sobre Java 21
**Por qué:** es la línea con soporte vigente. La 3.5 ya salió de soporte
open source y no tiene sentido arrancar un proyecto nuevo sobre ella.
**Implicaciones:** starters modulares (`spring-boot-starter-webmvc`,
`spring-boot-starter-flyway`) y `@MockitoBean` en lugar de `@MockBean`.

### D-02 · Esquema versionado con Flyway, Hibernate solo valida
**Por qué:** el enunciado pide entregar "script o migraciones" y además
suma como opcional. Con `ddl-auto: validate`, si una entidad y la tabla
no coinciden, la app no arranca: el error aparece en el arranque y no
en producción.
**Descartado:** `ddl-auto: update`, porque genera esquema implícito,
no reproducible y sin restricciones `CHECK`.

### D-03 · Eliminación lógica de tickets (`fecha_eliminacion`)
**Por qué:** un helpdesk necesita historial; borrar un ticket físicamente
rompe métricas y trazabilidad. Se usa una columna `fecha_eliminacion`
nula/no nula en vez de un booleano `activo`, porque responde dos preguntas
(si se borró y cuándo) con un solo dato.
**Por qué no reusar el estado `CERRADO`:** cerrado es un estado de
negocio (el problema se atendió); eliminado significa que el ticket no
debió existir (duplicado, spam). Mezclarlos ensucia los filtros.
**Efecto:** un ticket eliminado responde 404 en todos los endpoints
(`@SQLRestriction` en la entidad).

### D-04 · Etiquetas en tabla aparte (`ticket_etiquetas`)
**Por qué:** `@ElementCollection` es JPA estándar, portable a MySQL y
permite filtrar por etiqueta con SQL simple.
**Descartado:** columna `text[]` o JSONB, que atan el modelo a Postgres y
complican el mapeo sin beneficio real para 1 a 5 valores.

### D-05 · Enums como `VARCHAR` + restricción `CHECK`
**Por qué:** `@Enumerated(STRING)` es legible en la base y no se rompe si
se reordena el enum. El `CHECK` protege la integridad aunque alguien
inserte por fuera de la app.

### D-06 · Contraseñas con BCrypt usando solo `spring-security-crypto`
**Por qué:** el enunciado no pide login, pero guardar contraseñas en texto
plano no es aceptable. Se trae solo el módulo de criptografía, no el
starter de Spring Security, que bloquearía todos los endpoints por
defecto sin aportar nada al alcance obligatorio.

### D-07 · `.env` cargado por Spring, sin librerías extra
**Por qué:** `spring.config.import=optional:file:.env[.properties]` lee el
`.env` de forma nativa. Si no existe (CI, Docker), se usan las variables
de entorno del sistema. Cero dependencias nuevas.

### D-08 · Proveedor de IA: Groq vía `RestClient`, sin SDK ni Spring AI
**Por qué:** Groq expone una API compatible con OpenAI. Un cliente HTTP
propio de unas 80 líneas deja ver exactamente qué se envía, qué se recibe
y dónde se valida, que es justo lo que evalúa la rúbrica. Cambiar a
OpenAI u OpenRouter es cambiar `AI_BASE_URL` y `AI_MODEL`.
**Descartado:** Spring AI. Es válido, pero esconde el parseo y la
validación, y agrega una capa que no se puede justificar para un único
caso de uso.

### D-09 · El análisis de IA se hace ANTES de abrir la transacción
**Por qué:** llamar a un servicio externo dentro de `@Transactional`
retiene una conexión del pool durante toda la latencia de la IA. Se
analiza primero y se guarda todo (ticket + análisis) en una sola
transacción corta. Resultado: el ticket nunca queda guardado "a medias".

### D-10 · Respaldo automático: IA → reglas (patrón Decorator)
**Por qué:** `AnalizadorConRespaldo` implementa la misma interfaz,
envuelve al analizador de Groq y, ante timeout, error HTTP o respuesta
inválida, delega en el de reglas. El `TicketService` no sabe que existe
un respaldo. El ticket guarda `origen_analisis` (`IA` o `REGLAS`) para
que sea auditable qué lo clasificó.

### D-11 · La respuesta del modelo se trata como dato no confiable
**Por qué:** un LLM puede devolver `"conectividad "`, `"URGENTE"`, texto
antes del JSON o etiquetas de 40 caracteres. Antes de persistir:
- categoría y prioridad se normalizan (trim, mayúsculas, sin tildes) y se
  validan contra el enum; si no coinciden, cuenta como respuesta inválida
  y se usa el respaldo, nunca un valor inventado;
- el resumen se recorta a 30 palabras;
- las etiquetas se pasan a minúscula, se deduplican y se limitan a 5;
- `temperature: 0` y `response_format: json_object` para reducir la
  variabilidad desde el origen.

### D-12 · Transiciones de estado controladas
**Por qué:** que un ticket `CERRADO` vuelva a `ABIERTO` sin rastro
complica cualquier métrica. Transiciones permitidas:
`ABIERTO → EN_PROCESO | CERRADO`, `EN_PROCESO → RESUELTO | ABIERTO`,
`RESUELTO → CERRADO | EN_PROCESO`. `CERRADO` es final. Una transición
inválida responde 409 con un mensaje claro.

---

## Fuera de alcance a propósito (y por qué)

| Opcional | Decisión | Motivo |
|---|---|---|
| JWT + roles | Solo si sobra tiempo | No es obligatorio y agrega superficie de errores |
| Caché | No | Sin carga real que lo justifique |
| Reintentos a la IA | 1 reintento como máximo | Más reintentos alargan la petición del usuario; el respaldo ya cubre la caída |
| Testcontainers | Opcional | Requiere Docker en el entorno del evaluador |
