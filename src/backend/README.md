# Backend: autenticación JWT

## Configuración local

El backend necesita una clave JWT que se mantenga igual entre reinicios. Configúrala fuera del repositorio como variable de entorno `JWT_SECRET`, con al menos 32 caracteres aleatorios. No reutilices una clave de ejemplo ni la guardes en Git.

Variables opcionales:

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `JWT_EXPIRATION` | `PT24H` | Duración del token en formato ISO-8601, por ejemplo `PT2H`. |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Origen permitido para solicitudes del navegador. |

El frontend usa `http://localhost:8080/api` como API predeterminada. Si el backend se ejecuta en otra dirección, configura `VITE_API_BASE_URL` con la URL base de la API. `VITE_API_URL` sigue disponible para sobrescribir solo la URL de tareas.

## Inicio de sesión

`POST /api/auth/login` recibe un JSON con `email` y `password`. Las credenciales inválidas o las cuentas inactivas responden `401` con el mensaje genérico `Credenciales inválidas`. Si el acceso es correcto, la respuesta contiene `token` y `rol`. Envía el token en las rutas protegidas mediante `Authorization: Bearer <token>`.

Los tokens usan la clave de `JWT_SECRET` y expiran según `JWT_EXPIRATION`. El endpoint administrativo de creación de usuarios almacena las contraseñas con BCrypt y las respuestas de usuario no incluyen el campo de contraseña. Para facilitar la transición de cuentas existentes, una contraseña guardada en texto plano se convierte a un hash BCrypt al iniciar sesión correctamente.

## Perfiles de voluntarios

`GET /api/perfil/me` devuelve el perfil asociado al correo del token del solicitante autenticado. `GET /api/usuarios/{id}` y `GET /api/usuarios` están limitados al Administrador. Ambos exponen solo los campos del DTO de usuario; la contraseña no se devuelve nunca. Un voluntario que intente acceder a `/api/usuarios/{id}` recibe `403`.

## Administración de usuarios

El Administrador puede crear cuentas con `POST /api/usuarios`, editar nombre, correo, rol y opcionalmente contraseña con `PUT /api/usuarios/{id}`, consultar cuentas con `GET /api/usuarios` y desactivarlas con `PUT /api/usuarios/{id}/desactivar`. La consulta acepta los filtros opcionales `rol` y `activo` (por ejemplo, `/api/usuarios?rol=VOLUNTARIO&activo=false`). Los correos se normalizan y se comprueban sin distinguir mayúsculas; al desactivar una cuenta se conserva el registro y el historial, y esa cuenta no puede iniciar sesión.

## Envío de correo por SMTP

El backend proporciona un servicio reutilizable de correo y un endpoint de prueba restringido al rol `ADMINISTRADOR`. Para Gmail se recomienda SMTP en el puerto `465` con TLS implícito. También se admite el puerto `587` con STARTTLS.

Configura estas variables en el entorno donde se inicia el backend (por ejemplo, en las variables de entorno de la configuración de ejecución del IDE). Spring Boot no carga archivos `.env` automáticamente; si guardas los valores en `src/backend/.env`, configura además tu herramienta de ejecución para exportarlos. El archivo `.env` está excluido de Git:

| Variable | Requerida | Ejemplo |
| --- | --- | --- |
| `SMTP_HOST` | Sí | `smtp.gmail.com` |
| `SMTP_PORT` | No | `465` |
| `SMTP_SECURE` | No | `true` para TLS en 465; `false` para STARTTLS en 587 |
| `SMTP_USER` | Sí | La cuenta Gmail autorizada |
| `SMTP_PASS` | Sí | Contraseña de aplicación de Google |
| `SMTP_FROM` | Sí | Dirección remitente autorizada |
| `APP_MAIL_SYSTEM_NAME` | No | Nombre que aparece en las plantillas; predeterminado: `Nexora Tech Consulting` |

`SMTP_PORT` y `SMTP_SECURE` tienen valores predeterminados `465` y `true`. Para 587, usa `SMTP_PORT=587` y `SMTP_SECURE=false`. Nunca guardes credenciales reales en `.env.example`, código fuente o archivos versionados.

Gmail normalmente requiere que la cuenta tenga activada la verificación en dos pasos y que se genere una contraseña de aplicación desde la configuración de seguridad de la cuenta Google. Usa esa contraseña solo como `SMTP_PASS`; no uses ni compartas tu contraseña personal de Gmail.

Inicia el backend desde `src/backend` con `mvn spring-boot:run`. Ejecuta las pruebas con `mvn test`.

Con un token JWT de Administrador, realiza la prueba:

```http
POST /api/admin/email/test
Authorization: Bearer <token-de-administrador>
Content-Type: application/json

{"recipient":"destinatario@example.com"}
```

Una respuesta `200` indica que el servidor SMTP aceptó el mensaje para envío, pero no confirma por sí sola su entrega en la bandeja del destinatario. Verifica la recepción manualmente para completar la prueba real. Las pruebas automatizadas simulan el transporte y no requieren una cuenta Gmail.

Respuestas esperadas:

- `200`: `{"message":"Correo de prueba enviado correctamente."}`
- `400`: el destinatario no es válido.
- `401` / `403`: token ausente o usuario sin permisos de Administrador.
- `503`: configuración SMTP incompleta.
- `502`: falla de autenticación, conexión o envío; la respuesta no incluye detalles internos ni credenciales.

## Plantillas editables de actividad (KAN-33 / RF-32)

Las plantillas de **Asignación**, **Recordatorio** y **Cambio de turno** se guardan en la tabla `notification_template` de PostgreSQL. El proyecto ya usa Hibernate con `spring.jpa.hibernate.ddl-auto=update`; al iniciar el backend se crea la tabla si hace falta y se insertan las plantillas iniciales solo cuando no existen. También se agrega **Cancelación de asignación** para avisar cuando se cancela una asignación de tarea. Los cambios guardados no se reemplazan en reinicios. No se añadió un sistema de versiones separado.

La administración se realiza desde **Plantillas de correo** en el panel, disponible solo para `ADMINISTRADOR`. El backend también protege estos endpoints con Spring Security:

| Método | Endpoint | Uso |
| --- | --- | --- |
| `GET` | `/api/admin/email-templates` | Consultar las plantillas y su contenido |
| `GET` | `/api/admin/email-templates/{tipo}` | Consultar una plantilla (`ASIGNACION`, `RECORDATORIO`, `CAMBIO_TURNO`, `CANCELACION_ASIGNACION`) |
| `PUT` | `/api/admin/email-templates/{tipo}` | Guardar asunto, HTML, texto plano y estado activo |
| `POST` | `/api/admin/email-templates/{tipo}/send` | Renderizar la versión persistida y enviar por el SMTP existente |

El envío recibe `recipient`, `name`, `activity` y `date`; las variables `{{nombre}}`, `{{actividad}}` y `{{fecha}}` se sustituyen en asunto, HTML y texto plano. El backend rechaza plantillas con variables desconocidas o datos requeridos ausentes, y escapa los valores dinámicos insertados en HTML. No permite enviar una plantilla inactiva.

Las notificaciones automáticas usan la tabla persistente `notification_outbox`; el registro se crea en la misma transacción que la asignación/cambio/cancelación y conserva los datos dinámicos sin guardar el correo ya renderizado. El worker programado reclama lotes con `FOR UPDATE SKIP LOCKED`, usa la plantilla vigente y el `SmtpEmailService` existente, aplica reintentos exponenciales e idempotencia, y recupera leases vencidos. La solicitud del gestor no espera la respuesta SMTP.

La asignación de tareas y de turnos encola `ASIGNACION`. Al cancelar una asignación de tarea, se conserva su fila con estado `Cancelada`, se registra la acción en el historial y se encola `CANCELACION_ASIGNACION`; el administrador/gestor puede consultar y cancelar desde el modal de asignación. Si era la última asignación activa de una tarea en estado `Asignada`, la tarea vuelve a `Pendiente`. Al cancelar una asignación de turno o un turno se conserva el historial y se encola `CAMBIO_TURNO`; los turnos cancelados mantienen su fila con estado `CANCELADO`. El historial de asignaciones de turnos puede consultarse por turno.

Cada 15 minutos se revisan asignaciones vigentes de turnos que comienzan dentro de las próximas 24 horas y se encola un `RECORDATORIO` por turno, voluntario y fecha. Las asignaciones `Cancelada` o `Rechazada` y los turnos `CANCELADO` se excluyen. La fecha límite de tareas no existe en el modelo, así que no se genera un recordatorio de vencimiento de tareas.

Endpoints de gestión de tareas y turnos (solo `ADMINISTRADOR` o `GESTOR_TAREAS` para escribir; el historial de asignaciones de turnos también se limita a esos roles):

| Método | Endpoint | Uso |
| --- | --- | --- |
| `PUT` | `/api/turnos/{id}` | Editar fecha, horario, actividad o cupos; valida cruces y notifica cambios de agenda. |
| `POST` | `/api/turnos/{id}/cancelar` | Cambiar estado a `CANCELADO`, conservar el turno y cancelar sus asignaciones vigentes. |
| `POST` | `/api/turnos/{id}/asignaciones` | Crear asignaciones sin sobrescribir registros anteriores; valida cupos y cruces. |
| `GET` | `/api/turnos/{id}/asignaciones` | Consultar todas las asignaciones, incluidas las canceladas. |
| `POST` | `/api/turnos/{id}/asignaciones/{idAsignacion}/cancelar` | Cancelar una asignación conservando su fila histórica. |
| `POST` | `/api/tareas/{idTarea}/asignaciones/{idAsignacion}/cancelar` | Cancelar una asignación de tarea, guardar su historial y encolar el correo de cancelación. |

`src/backend/src/main/resources/db/migration/V1__create_notification_outbox.sql` define la cola. `V2__preserve_turn_assignments_and_add_cancellation.sql` agrega el estado y reemplaza la restricción única por pareja con unicidad solo para asignaciones vigentes; es necesaria para poder reasignar sin borrar el historial. El proyecto no ejecuta Flyway/Liquibase: Hibernate crea columnas/tablas nuevas con `ddl-auto=update`, pero la migración V2 debe ejecutarse manualmente en Neon antes de habilitar reasignaciones. No se ha aplicado SQL directamente a Neon desde este cambio. SMTP real y Neon no se validan en las pruebas unitarias.

Propiedades del worker (valores predeterminados):

| Propiedad | Predeterminado | Uso |
| --- | --- | --- |
| `app.notification.outbox.poll-interval-ms` | `5000` | Intervalo de consulta del worker. |
| `app.notification.outbox.batch-size` | `20` | Máximo de mensajes por lote. |
| `app.notification.outbox.max-attempts` | `5` | Intentos máximos de entrega. |
| `app.notification.outbox.lease-seconds` | `300` | Tiempo tras el cual se recupera un trabajo abandonado. |
| `app.notification.outbox.retry-base-seconds` | `30` | Base del backoff exponencial, con tope de una hora. |
| `app.notification.reminders.poll-interval-ms` | `900000` | Frecuencia de búsqueda de turnos próximos. |
| `app.notification.reminders.window-hours` | `24` | Anticipación máxima del recordatorio de turno. |

## Plantillas adicionales de correo de cuenta

Las notificaciones reutilizan `SmtpEmailService` y la configuración SMTP anterior; no crean otro transporte ni requieren nuevas credenciales. Las plantillas incluyen versión HTML adaptable y alternativa de texto plano, escapan los datos dinámicos en HTML y validan el destinatario en el servicio SMTP existente.

Están disponibles las plantillas de bienvenida, recuperación de contraseña, confirmación de restablecimiento, notificación administrativa, cambios de cuenta y confirmación de operación. Los flujos actuales envían bienvenida al crear una cuenta de usuario o habilitar acceso a un voluntario, y notifican cambios de perfil, cambio administrativo de contraseña y activación o desactivación de acceso. Los avisos se procesan después del commit cuando la operación usa una transacción; si SMTP falla, se registra únicamente el tipo de notificación y la categoría segura del error.

La plantilla de recuperación está lista para reutilizarse, pero el sistema todavía no tiene un flujo de recuperación de extremo a extremo. No se generan ni persisten tokens, no existe un endpoint de recuperación y no se envían enlaces de recuperación desde la aplicación. Cuando ese flujo se implemente, debe proporcionar un enlace HTTPS con token de expiración; no envíes contraseñas ni tokens mediante logs.
