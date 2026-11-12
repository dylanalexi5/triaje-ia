# CONTEXT.md

## Qué es este proyecto

API REST en Spring Boot que centraliza las solicitudes de soporte
técnico de los empleados de una empresa (una mesa de ayuda interna).
Cuando un usuario reporta un problema en texto libre, el sistema guarda
el ticket y usa inteligencia artificial para clasificarlo: categoría,
prioridad, resumen y etiquetas.

Es la prueba técnica de Campuslands (Bucaramanga) para el cargo de
Trainer en Desarrollo de Software + AI. El código no es el producto
final: es la evidencia de que se sabe diseñar una API bien estructurada
y una integración de IA correctamente desacoplada.

## Por qué existe cada pieza

- **Servicio de IA aparte (`ai/`)**: la rúbrica evalúa el
  desacoplamiento del proveedor. Si mañana cambia Groq por OpenAI u
  Ollama, se reemplaza una clase y nada más del sistema se entera.
- **Clasificador de reglas como respaldo**: el enunciado pide manejar
  la caída del proveedor de IA. Sin respaldo, una caída externa tumbaría
  la creación de tickets, y eso es un fallo de diseño.
- **DTOs en vez de entidades**: no se filtran campos internos (el
  password, por ejemplo) y el contrato de la API no depende de las tablas.
- **Variables de entorno para credenciales**: el repo es público.

## Para quién es cada parte

- **Evaluador técnico**: arquitectura limpia, separación de
  responsabilidades, manejo de errores y pruebas, no solo "que funcione".
- **Empleado que reporta**: crear un ticket debe ser simple y siempre
  recibir una respuesta clara, incluso si la IA falla.
- **Técnico (rol SOPORTE)**: listar, filtrar y actualizar el estado de
  los tickets.

## Stack

Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, PostgreSQL 16,
Flyway, Maven. Proveedor de IA: Groq (API compatible con OpenAI),
consumido con `RestClient`, sin SDK.

## Reglas de arquitectura (no se rompen en ninguna sesión)

1. Los controllers NUNCA devuelven entidades JPA, solo DTOs (`dto/`).
2. Capas: `controller` → `service` → `repository`. Nada se salta una capa.
3. El analizador de IA vive en `ai/`, implementa `AnalizadorTicketService`
   (la interfaz del enunciado) y tiene al menos dos implementaciones:
   la real (proveedor externo) y la de respaldo (reglas). El resto del
   sistema solo habla con `ClasificadorTicket` (D-17).
4. La salida del modelo es **dato no confiable**: se valida contra los
   enums y se limpia antes de tocar la base de datos.
5. Todo error pasa por un único `@RestControllerAdvice`
   (`exception/GlobalExceptionHandler`), con el formato de error del
   enunciado. Nunca stack traces ni nombres de tablas hacia el cliente.
6. Ninguna credencial en código ni en `application.yml`: todo vía
   variables de entorno, documentado en `.env.example` sin valores reales.
7. Solo un usuario con rol `SOPORTE` puede ser técnico asignado. Esa
   validación vive en el `service`; no se confía en el cliente.
8. El esquema de base de datos lo manda Flyway. Hibernate solo valida
   (`ddl-auto: validate`), nunca crea ni altera tablas.

## Ciclo de trabajo por cada commit

1. Un cambio pequeño y con un solo propósito.
2. `./mvnw verify` en verde (compila y pasan todas las pruebas).
3. Prueba manual del comportamiento nuevo contra la app corriendo
   (curl o Postman), incluyendo al menos un caso de error.
4. Commit con mensaje en formato `tipo: descripción` y un cuerpo que
   explique el porqué.
5. **Pausa:** se revisa el diff y se valida antes de pasar al siguiente
   paso. No se encadenan commits sin esa validación.

## Reglas para trabajar con IA en este repo (anti-alucinación del proceso)

- Cambios acotados: una funcionalidad por sesión, nunca "hazme todo".
- Toda afirmación técnica se verifica ejecutándola, no leyendo un log.
  Ejemplo: la carga del `.env` se probó arrancando con el archivo, sin él
  y con una contraseña incorrecta.
- Nada se da por hecho sin compilar: `./mvnw verify` antes de cada commit.
- No se agregan dependencias que no estén justificadas en `DECISIONS.md`.
- No se inventan endpoints, campos ni enums que no pida el enunciado;
  si algo extra se agrega, queda escrito en `DECISIONS.md` con el porqué.
- Cada diff se revisa línea por línea: todo tiene que poder explicarse
  en vivo.

## Qué NO es parte del alcance obligatorio

JWT, autorización por roles, caché, paginación, Docker y métricas son
opcionales (sección 14). Solo se tocan cuando el núcleo esté terminado,
probado y documentado.

## Estado del proyecto

- [x] Proyecto inicializado (Spring Boot 4.1 + dependencias)
- [x] Entidades, enums y migración inicial (Flyway)
- [x] Registro y consulta de usuarios
- [~] Tickets: crear, consultar y filtrar ✔ — faltan estado, asignación y eliminación
- [ ] Integración con proveedor de IA real
- [x] Clasificador de respaldo por reglas
- [ ] Manejo global de errores
- [ ] Validaciones de entrada
- [ ] Pruebas (los 5 casos mínimos del enunciado)
- [ ] README + diagrama + colección Postman o Swagger
