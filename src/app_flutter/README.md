# Nexora Voluntarios (Flutter)

La app permite iniciar sesión con las credenciales de la plataforma, consultar
las tareas y turnos asignados y aceptar o rechazar las asignaciones pendientes.
Las respuestas se guardan en el backend compartido y se reflejan en la plataforma
web.

Desde la pestaña **Perfil**, el voluntario puede editar nombre, apellido,
correo electrónico y teléfono. Documento y rol se muestran como solo lectura.
Si se cambia el correo, el backend sincroniza la cuenta y devuelve un JWT nuevo
para conservar la sesión móvil.

## Ejecutar

Desde esta carpeta:

```powershell
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
```

`10.0.2.2` apunta al equipo anfitrión desde el emulador Android. Para un
dispositivo físico, usa la dirección IP accesible del servidor, por ejemplo:

```powershell
flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080/api
```

La URL debe terminar en `/api`. El backend debe estar accesible desde el
dispositivo. La sesión se conserva localmente; al abrir la app se verifica el
JWT con `/api/perfil/me`, y las sesiones inválidas o expiradas vuelven al login.

La pestaña **Notificaciones** muestra los avisos enviados más recientes primero.
Al abrir un aviso no leído, se guarda su estado como leído en el backend.
