# Matriz de Permisos del Sistema (RBAC)
Esta matriz define qué acciones puede realizar cada rol dentro del sistema de gestión de voluntarios de la Casa Hogar Amandita.

| Acción / Funcionalidad | Administrador | TI | Gestor de Tareas | Voluntario |
| :--- | :---: | :---: | :---: | :---: |
| Iniciar Sesión | ✅ | ✅ | ✅ | ✅ |
| Ver su propio perfil | ✅ | ✅ | ✅ | ✅ |
| Ver perfiles de otros voluntarios | ✅ | ❌ | ❌ | ❌ |
| Crear/Consultar/Editar Usuarios | ✅ | ❌ | ❌ | ❌ |
| Desactivar Usuarios | ✅ | ❌ | ❌ | ❌ |
| Consultar tareas operativas | ✅ | ✅ | ✅ | ✅ |
| Crear/Editar Tareas Operativas | ✅ | ❌ | ✅ | ❌ |
| Ver reportes de deserción/horas | ✅ | ✅ | ❌ | ❌ |
| Configurar el sistema (TI) | ❌ | ✅ | ❌ | ❌ |
| Marcar entrada/salida (asistencia) | ✅ | ✅ | ✅ | ✅ |
*Nota: El Administrador tiene acceso total. El rol de TI se encarga del mantenimiento técnico, no de la gestión de personal.*

## Aplicación en la API y la interfaz

| Recurso actual | Métodos | Roles autorizados | Respuesta sin autorización |
| --- | --- | --- | --- |
| `/api/auth/**` | Operaciones de autenticación | Público para iniciar sesión | — |
| `/api/perfil/me` | `GET` | Cualquier usuario autenticado; solo devuelve el perfil del titular del token | `401` sin sesión válida |
| `/api/usuarios` | `GET`, `POST` | Administrador; consulta filtrable por rol y estado | `401` sin sesión; `403` para otros roles |
| `/api/usuarios/{id}` | `GET`, `PUT` | Administrador | `401` sin sesión; `403` para otros roles |
| `/api/usuarios/{id}/desactivar` | `PUT` | Administrador | `401` sin sesión; `403` para otros roles |
| `/api/tareas/**` | `GET` | Administrador, TI, Gestor de Tareas, Voluntario | `401` si no hay sesión válida |
| `/api/tareas/**` | `POST`, `PUT` | Administrador, Gestor de Tareas | `401` sin sesión; `403` con otro rol |
| `/api/tareas/**` | Otros métodos | Ninguno hasta que se definan | `403` |
| `/api/usuarios/**` | `GET`, `POST`, `PUT` | Administrador | `401` sin sesión; `403` con otro rol |

La interfaz muestra a todos los usuarios autenticados los enlaces a tareas y a su propio perfil. Solo el Administrador ve el directorio de voluntarios y puede abrir perfiles de otras personas; el backend aplica las mismas restricciones independientemente de la interfaz. Los módulos de reportes, asistencia y configuración TI todavía no tienen endpoints ni pantallas y deberán recibir sus permisos al implementarse.

Los módulos de perfil, reportes, asistencia y configuración TI descritos en la matriz funcional todavía no tienen endpoints ni pantallas en esta aplicación; sus permisos deberán aplicarse al implementar esos módulos.