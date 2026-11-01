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
