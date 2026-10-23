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
