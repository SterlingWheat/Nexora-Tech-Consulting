import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

const String _apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api',
);

/// Punto de inyección para pruebas: por defecto usa el cliente real de `http`,
/// pero las pruebas de widgets pueden reemplazarlo para simular respuestas
/// del backend sin hacer peticiones de red reales (KAN-35).
@visibleForTesting
Future<http.Response> Function(Uri url, {Map<String, String>? headers})
httpGetOverride = http.get;

@visibleForTesting
Future<http.Response> Function(Uri url, {Map<String, String>? headers})
httpPatchOverride = http.patch;

// Misma paleta de colores que el sitio web (ver :root en src/frontend/src/App.vue)
const Color _salvia = Color(0xFFA5AA94);
const Color _salviaClaro = Color(0xFFC4C9B2);
const Color _crema = Color(0xFFEFE3D3);
const Color _cremaClaro = Color(0xFFFCF8F3);
const Color _durazno = Color(0xFFF1CDB7);
const Color _texto = Color(0xFF51483E);
const Color _textoSuave = Color(0xFF7B766D);
const Color _verdeOscuro = Color(0xFF737D61);
const Color _errorTexto = Color(0xFFA34D41);
const Color _errorFondo = Color(0xFFFFF0EB);

final ColorScheme _nexoraColorScheme = ColorScheme.light(
  primary: _verdeOscuro,
  onPrimary: Colors.white,
  primaryContainer: _salviaClaro,
  onPrimaryContainer: _texto,
  secondary: _salvia,
  onSecondary: _texto,
  secondaryContainer: _crema,
  onSecondaryContainer: _texto,
  tertiary: _durazno,
  onTertiary: _texto,
  error: _errorTexto,
  onError: Colors.white,
  errorContainer: _errorFondo,
  onErrorContainer: _errorTexto,
  surface: Colors.white,
  onSurface: _texto,
  surfaceContainerHighest: _cremaClaro,
  outline: _crema,
  outlineVariant: _salviaClaro,
);

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Nexora Voluntarios',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: _nexoraColorScheme,
        useMaterial3: true,
        scaffoldBackgroundColor: _cremaClaro,
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          foregroundColor: _texto,
          elevation: 0,
        ),
        cardTheme: CardThemeData(
          color: Colors.white,
          surfaceTintColor: Colors.transparent,
          elevation: 1,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        ),
        chipTheme: ChipThemeData(
          backgroundColor: _crema.withValues(alpha: 0.6),
          labelStyle: const TextStyle(color: _texto, fontWeight: FontWeight.w600),
          side: BorderSide(color: _crema),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: _cremaClaro,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(10),
            borderSide: BorderSide(color: _crema),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(10),
            borderSide: const BorderSide(color: _verdeOscuro, width: 1.5),
          ),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            backgroundColor: _verdeOscuro,
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            foregroundColor: _verdeOscuro,
            side: const BorderSide(color: _crema),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
          ),
        ),
        textTheme: const TextTheme(
          bodyMedium: TextStyle(color: _texto),
          bodySmall: TextStyle(color: _textoSuave),
        ),
      ),
      home: const SessionGate(),
    );
  }
}

class SessionGate extends StatefulWidget {
  const SessionGate({super.key});

  @override
  State<SessionGate> createState() => _SessionGateState();
}

class _SessionGateState extends State<SessionGate> {
  bool _loading = true;
  bool _authenticated = false;
  String _role = '';
  String _email = '';
  String _token = '';

  @override
  void initState() {
    super.initState();
    _restoreSession();
  }

  Future<void> _restoreSession() async {
    final preferences = await SharedPreferences.getInstance();
    final token = preferences.getString('authToken');
    final role = preferences.getString('authRole');

    if (token == null ||
        token.isEmpty ||
        role == null ||
        role.toUpperCase() != 'VOLUNTARIO' ||
        _isTokenExpired(token)) {
      await _clearSession();
      if (!mounted) return;
      setState(() {
        _loading = false;
        _authenticated = false;
      });
      return;
    }

    try {
      final response = await http
          .get(
            Uri.parse('$_apiBaseUrl/perfil/me'),
            headers: {'Authorization': 'Bearer $token'},
          )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode != 200) {
        await _clearSession();
        if (!mounted) return;
        setState(() {
          _loading = false;
          _authenticated = false;
        });
        return;
      }

      final profile =
          jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
      if (!mounted) return;
      setState(() {
        _email = profile['email']?.toString() ?? '';
        _role = role;
        _token = token;
        _authenticated = true;
        _loading = false;
      });
    } catch (_) {
      await _clearSession();
      if (!mounted) return;
      setState(() {
        _loading = false;
        _authenticated = false;
      });
    }
  }

  bool _isTokenExpired(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return true;
      final payload = base64Url.normalize(parts[1]);
      final decoded = utf8.decode(base64Url.decode(payload));
      final claims = jsonDecode(decoded) as Map<String, dynamic>;
      final exp = claims['exp'];
      if (exp is! num) return true;
      return exp * 1000 <= DateTime.now().millisecondsSinceEpoch;
    } catch (_) {
      return true;
    }
  }

  Future<void> _clearSession() async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.remove('authToken');
    await preferences.remove('authRole');
  }

  Future<void> _onLogin(String token, String role, String email) async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.setString('authToken', token);
    await preferences.setString('authRole', role);
    if (!mounted) return;
    setState(() {
      _email = email;
      _role = role;
      _token = token;
      _authenticated = true;
    });
  }

  Future<void> _onProfileUpdated(String token, String email) async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.setString('authToken', token);
    if (!mounted) return;
    setState(() {
      _token = token;
      _email = email;
    });
  }

  Future<void> _logout() async {
    await _clearSession();
    if (!mounted) return;
    setState(() {
      _authenticated = false;
      _email = '';
      _role = '';
      _token = '';
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (!_authenticated) {
      return LoginPage(onLogin: _onLogin);
    }
    return VolunteerHomePage(
      email: _email,
      role: _role,
      token: _token,
      onProfileUpdated: _onProfileUpdated,
      onLogout: _logout,
    );
  }
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.onLogin});

  final Future<void> Function(String token, String role, String email) onLogin;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _loading = false;
  bool _obscurePassword = true;
  String? _error;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final response = await http
          .post(
            Uri.parse('$_apiBaseUrl/auth/login'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'email': _emailController.text.trim(),
              'password': _passwordController.text,
            }),
          )
          .timeout(const Duration(seconds: 15));
      final body =
          jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;

      if (response.statusCode != 200) {
        setState(() {
          _error = body['message']?.toString() ?? 'No se pudo iniciar sesión.';
        });
        return;
      }

      final token = body['token']?.toString();
      final role = body['rol']?.toString();
      if (token == null ||
          token.isEmpty ||
          role?.toUpperCase() != 'VOLUNTARIO') {
        setState(() {
          _error =
              'Solo usuarios con rol Voluntario pueden usar la aplicación.';
        });
        return;
      }

      await widget.onLogin(token, role!, _emailController.text.trim());
    } catch (_) {
      setState(() {
        _error = 'No se pudo conectar con el servidor. Verifica tu conexión.';
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Icon(Icons.volunteer_activism, size: 72),
                    const SizedBox(height: 12),
                    Text(
                      'Nexora Voluntarios',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.headlineMedium,
                    ),
                    const SizedBox(height: 32),
                    TextFormField(
                      key: const Key('emailField'),
                      controller: _emailController,
                      keyboardType: TextInputType.emailAddress,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(
                        labelText: 'Correo electrónico',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.email_outlined),
                      ),
                      validator: (value) {
                        if (value == null || value.trim().isEmpty) {
                          return 'Ingresa tu correo';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      key: const Key('passwordField'),
                      controller: _passwordController,
                      obscureText: _obscurePassword,
                      onFieldSubmitted: (_) => _loading ? null : _login(),
                      decoration: InputDecoration(
                        labelText: 'Contraseña',
                        border: const OutlineInputBorder(),
                        prefixIcon: const Icon(Icons.lock_outline),
                        suffixIcon: IconButton(
                          onPressed: () {
                            setState(
                              () => _obscurePassword = !_obscurePassword,
                            );
                          },
                          icon: Icon(
                            _obscurePassword
                                ? Icons.visibility_outlined
                                : Icons.visibility_off_outlined,
                          ),
                        ),
                      ),
                      validator: (value) {
                        if (value == null || value.isEmpty) {
                          return 'Ingresa tu contraseña';
                        }
                        return null;
                      },
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 16),
                      Text(
                        _error!,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ],
                    const SizedBox(height: 24),
                    FilledButton(
                      key: const Key('loginButton'),
                      onPressed: _loading ? null : _login,
                      child: _loading
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('Iniciar sesión'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class VolunteerHomePage extends StatefulWidget {
  const VolunteerHomePage({
    super.key,
    required this.email,
    required this.role,
    required this.token,
    required this.onProfileUpdated,
    required this.onLogout,
  });

  final String email;
  final String role;
  final String token;
  final Future<void> Function(String token, String email) onProfileUpdated;
  final Future<void> Function() onLogout;

  @override
  State<VolunteerHomePage> createState() => _VolunteerHomePageState();
}

class _VolunteerHomePageState extends State<VolunteerHomePage> {
  int _selectedPage = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(switch (_selectedPage) {
          0 => 'Mis asignaciones',
          1 => 'Notificaciones',
          _ => 'Mi perfil',
        }),
        actions: [
          IconButton(
            tooltip: 'Cerrar sesión',
            onPressed: widget.onLogout,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: IndexedStack(
        index: _selectedPage,
        children: [
          VolunteerAssignmentsPage(
            email: widget.email,
            role: widget.role,
            token: widget.token,
          ),
          if (_selectedPage == 1)
            NotificationsCenterPage(token: widget.token)
          else
            const SizedBox.shrink(),
          VolunteerProfilePage(
            token: widget.token,
            onProfileUpdated: widget.onProfileUpdated,
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedPage,
        onDestinationSelected: (index) {
          setState(() => _selectedPage = index);
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.assignment_outlined),
            selectedIcon: Icon(Icons.assignment),
            label: 'Asignaciones',
          ),
          NavigationDestination(
            icon: Icon(Icons.notifications_none),
            selectedIcon: Icon(Icons.notifications),
            label: 'Notificaciones',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}

class VolunteerProfilePage extends StatefulWidget {
  const VolunteerProfilePage({
    super.key,
    required this.token,
    required this.onProfileUpdated,
  });

  final String token;
  final Future<void> Function(String token, String email) onProfileUpdated;

  @override
  State<VolunteerProfilePage> createState() => _VolunteerProfilePageState();
}

class _VolunteerProfilePageState extends State<VolunteerProfilePage> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _surnameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _documentController = TextEditingController();
  final _roleController = TextEditingController();
  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _surnameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _documentController.dispose();
    _roleController.dispose();
    super.dispose();
  }

  Future<void> _loadProfile() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final response = await http.get(
        Uri.parse('$_apiBaseUrl/movil/perfil/me'),
        headers: {'Authorization': 'Bearer ${widget.token}'},
      );
      if (response.statusCode != 200) {
        throw Exception('No se pudo cargar tu perfil.');
      }
      final profile =
          jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
      if (!mounted) return;
      _nameController.text = profile['nombre']?.toString() ?? '';
      _surnameController.text = profile['apellido']?.toString() ?? '';
      _emailController.text = profile['email']?.toString() ?? '';
      _phoneController.text = profile['telefono']?.toString() ?? '';
      _documentController.text = profile['documento']?.toString() ?? '';
      _roleController.text = profile['rol']?.toString() ?? '';
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _saveProfile() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final response = await http.put(
        Uri.parse('$_apiBaseUrl/movil/perfil/me'),
        headers: {
          'Authorization': 'Bearer ${widget.token}',
          'Content-Type': 'application/json',
        },
        // Nombre, apellido y correo no son editables: solo se envía el teléfono
        // (evita que el voluntario pueda hacerse pasar por otra persona)
        body: jsonEncode({'telefono': _phoneController.text.trim()}),
      );
      if (response.statusCode != 200) {
        final content = utf8.decode(response.bodyBytes).trim();
        throw Exception(
          content.isEmpty ? 'No se pudo guardar tu perfil.' : content,
        );
      }
      final result =
          jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
      final profile = result['perfil'] as Map<String, dynamic>;
      final token = result['token']?.toString();
      if (token == null || token.isEmpty) {
        throw const FormatException(
          'El servidor no devolvió la sesión actualizada.',
        );
      }
      await widget.onProfileUpdated(token, profile['email']?.toString() ?? '');
      if (!mounted) return;
      setState(() {
        _nameController.text = profile['nombre']?.toString() ?? '';
        _surnameController.text = profile['apellido']?.toString() ?? '';
        _emailController.text = profile['email']?.toString() ?? '';
        _phoneController.text = profile['telefono']?.toString() ?? '';
        _documentController.text = profile['documento']?.toString() ?? '';
        _roleController.text = profile['rol']?.toString() ?? '';
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Perfil actualizado correctamente.')),
      );
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null && _nameController.text.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: _loadProfile,
              child: const Text('Reintentar'),
            ),
          ],
        ),
      );
    }
    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 24, 20, 32),
        children: [
          _encabezadoPerfil(),
          const SizedBox(height: 20),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _etiquetaSeccion('Datos personales'),
                  const SizedBox(height: 16),
                  // Nombre, apellido y correo identifican al voluntario: se muestran "transparentes"
                  // (sin caja de texto) para que quede claro que no son editables desde aquí.
                  _campoFijo(
                    etiquetaKey: 'profileNameField',
                    etiqueta: 'Nombre',
                    valor: _nameController.text,
                    icono: Icons.person_outline,
                  ),
                  _campoFijo(
                    etiquetaKey: 'profileSurnameField',
                    etiqueta: 'Apellido',
                    valor: _surnameController.text,
                    icono: Icons.person_outline,
                  ),
                  _campoFijo(
                    etiquetaKey: 'profileEmailField',
                    etiqueta: 'Correo electrónico',
                    valor: _emailController.text,
                    icono: Icons.mail_outline,
                  ),
                  _campoFijo(
                    etiquetaKey: 'profileDocumentField',
                    etiqueta: 'Documento',
                    valor: _documentController.text,
                    icono: Icons.badge_outlined,
                  ),
                  _campoFijo(
                    etiquetaKey: 'profileRoleField',
                    etiqueta: 'Rol',
                    valor: _roleController.text,
                    icono: Icons.verified_user_outlined,
                    ultimo: true,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _etiquetaSeccion('Contacto'),
                  const SizedBox(height: 16),
                  TextFormField(
                    key: const Key('profilePhoneField'),
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    maxLength: 20,
                    // Solo números y los signos propios de un teléfono (+, guiones, paréntesis,
                    // espacios), para poder escribir el código de país si es de otro país.
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[0-9+\-() ]')),
                    ],
                    decoration: InputDecoration(
                      labelText: 'Teléfono',
                      hintText: 'Ej: +591 700 00000',
                      helperText:
                          'Si es de otro país, agrega el código (ej. +591).',
                      helperMaxLines: 2,
                      prefixIcon: const Icon(Icons.phone_outlined),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) return null;
                      return RegExp(r'^[0-9+()\-\s]{6,20}$').hasMatch(value)
                          ? null
                          : 'El teléfono no tiene un formato válido.';
                    },
                  ),
                ],
              ),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: _errorFondo,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: _errorTexto.withValues(alpha: 0.25)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.error_outline, size: 18, color: _errorTexto),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      _error!,
                      style: const TextStyle(color: _errorTexto, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 22),
          FilledButton.icon(
            key: const Key('saveProfileButton'),
            onPressed: _saving ? null : _saveProfile,
            icon: _saving
                ? const SizedBox(
                    height: 18,
                    width: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Icon(Icons.check_circle_outline, size: 20),
            label: Text(_saving ? 'Guardando...' : 'Guardar cambios'),
            style: FilledButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 14),
              minimumSize: const Size.fromHeight(48),
            ),
          ),
        ],
      ),
    );
  }

  // Etiqueta de sección en mayúsculas, igual al estilo del sitio web
  Widget _etiquetaSeccion(String texto) {
    return Text(
      texto.toUpperCase(),
      style: const TextStyle(
        fontSize: 11,
        fontWeight: FontWeight.w700,
        letterSpacing: 1.6,
        color: _verdeOscuro,
      ),
    );
  }

  String _iniciales() {
    final nombre = _nameController.text.trim();
    final apellido = _surnameController.text.trim();
    final iniciales = '${nombre.isNotEmpty ? nombre[0] : ''}${apellido.isNotEmpty ? apellido[0] : ''}'
        .toUpperCase();
    return iniciales.isEmpty ? '?' : iniciales;
  }

  Widget _encabezadoPerfil() {
    final nombreCompleto = '${_nameController.text} ${_surnameController.text}'.trim();
    return Row(
      children: [
        CircleAvatar(
          radius: 32,
          backgroundColor: _salviaClaro,
          child: Text(
            _iniciales(),
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: _texto,
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'MI PERFIL',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 1.6,
                  color: _verdeOscuro,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                nombreCompleto.isEmpty ? 'Voluntario' : nombreCompleto,
                style: const TextStyle(
                  fontSize: 21,
                  fontWeight: FontWeight.w600,
                  color: _texto,
                ),
              ),
              if (_roleController.text.isNotEmpty) ...[
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: _crema,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    _roleController.text,
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: _texto,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }

  // Campo "transparente": sin caja de texto, para dejar claro visualmente que no es editable
  Widget _campoFijo({
    required String etiquetaKey,
    required String etiqueta,
    required String valor,
    required IconData icono,
    bool ultimo = false,
  }) {
    return Container(
      key: Key(etiquetaKey),
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: ultimo
          ? null
          : const BoxDecoration(
              border: Border(bottom: BorderSide(color: _crema)),
            ),
      child: Row(
        children: [
          Icon(icono, size: 18, color: _textoSuave),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  etiqueta,
                  style: const TextStyle(fontSize: 11, color: _textoSuave),
                ),
                const SizedBox(height: 2),
                Text(
                  valor.isEmpty ? '—' : valor,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: _texto,
                  ),
                ),
              ],
            ),
          ),
          Tooltip(
            message: 'No editable',
            child: Icon(
              Icons.lock_outline,
              size: 16,
              color: _textoSuave.withValues(alpha: 0.55),
            ),
          ),
        ],
      ),
    );
  }
}

class VolunteerAssignmentsPage extends StatefulWidget {
  const VolunteerAssignmentsPage({
    super.key,
    required this.email,
    required this.role,
    required this.token,
  });

  final String email;
  final String role;
  final String token;

  @override
  State<VolunteerAssignmentsPage> createState() =>
      _VolunteerAssignmentsPageState();
}

class _VolunteerAssignmentsPageState extends State<VolunteerAssignmentsPage>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  bool _loading = true;
  String? _error;
  List<Map<String, dynamic>> _tasks = [];
  List<Map<String, dynamic>> _shifts = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadAssignments();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadAssignments() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final headers = {'Authorization': 'Bearer ${widget.token}'};
      final responses = await Future.wait([
        httpGetOverride(
          Uri.parse('$_apiBaseUrl/movil/asignaciones/tareas'),
          headers: headers,
        ),
        httpGetOverride(
          Uri.parse('$_apiBaseUrl/movil/asignaciones/turnos'),
          headers: headers,
        ),
      ]);

      for (final response in responses) {
        if (response.statusCode != 200) {
          throw Exception('No se pudieron cargar tus asignaciones.');
        }
      }

      final taskResponse =
          jsonDecode(utf8.decode(responses[0].bodyBytes)) as List<dynamic>;
      final shiftResponse =
          jsonDecode(utf8.decode(responses[1].bodyBytes)) as List<dynamic>;
      if (!mounted) return;
      setState(() {
        _tasks = taskResponse.cast<Map<String, dynamic>>();
        _shifts = shiftResponse.cast<Map<String, dynamic>>();
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _respond({
    required bool isTask,
    required Map<String, dynamic> assignment,
    required bool accept,
  }) async {
    final entity = isTask ? 'la tarea' : 'el turno';
    final decision = accept ? 'aceptar' : 'rechazar';
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('${accept ? 'Aceptar' : 'Rechazar'} asignación'),
        content: Text('¿Confirmas que deseas $decision $entity?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Volver'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Confirmar'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    final type = isTask ? 'tareas' : 'turnos';
    final parentId = isTask ? assignment['idTarea'] : assignment['idTurno'];
    final assignmentId = assignment['idAsignacion'];
    final action = accept ? 'confirmar' : 'rechazar';
    try {
      final response = await httpPatchOverride(
        Uri.parse(
          '$_apiBaseUrl/movil/asignaciones/$type/$parentId/$assignmentId/$action',
        ),
        headers: {'Authorization': 'Bearer ${widget.token}'},
      );
      if (response.statusCode != 204) {
        final content = utf8.decode(response.bodyBytes).trim();
        String message = 'No se pudo actualizar la asignación.';
        if (content.isNotEmpty) {
          try {
            final body = jsonDecode(content);
            message = body is Map<String, dynamic>
                ? body['message']?.toString() ?? message
                : body.toString();
          } on FormatException {
            message = content;
          }
        }
        throw Exception(message);
      }
      await _loadAssignments();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString().replaceFirst('Exception: ', '')),
        ),
      );
    }
  }

  bool _isPending(String? status) =>
      status == null ||
      status.toLowerCase() == 'asignado' ||
      status.toLowerCase() == 'pendiente';

  // Chip de estado con el mismo código de colores que usa el sitio web
  // (verde = cubierto/confirmado, durazno = pendiente, coral = rechazado/cancelado)
  Widget _estadoChip(String estado) {
    final normalizado = estado.toLowerCase();
    Color fondo;
    Color texto;
    if (normalizado.contains('rechaz') || normalizado.contains('cancel')) {
      fondo = _errorFondo;
      texto = _errorTexto;
    } else if (normalizado.contains('pendiente') ||
        normalizado.contains('asignado')) {
      fondo = _durazno.withValues(alpha: 0.45);
      texto = const Color(0xFF8A5A36);
    } else {
      fondo = const Color(0xFFE8F2E5);
      texto = const Color(0xFF50704B);
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: fondo,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        estado,
        style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: texto),
      ),
    );
  }

  Widget _assignmentList(
    List<Map<String, dynamic>> assignments, {
    required bool isTask,
  }) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: _errorFondo,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: _errorTexto.withValues(alpha: 0.25)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, size: 18, color: _errorTexto),
                    const SizedBox(width: 10),
                    Flexible(
                      child: Text(
                        _error!,
                        style: const TextStyle(color: _errorTexto, fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              OutlinedButton.icon(
                onPressed: _loadAssignments,
                icon: const Icon(Icons.refresh),
                label: const Text('Reintentar'),
              ),
            ],
          ),
        ),
      );
    }
    if (assignments.isEmpty) {
      // RF-38/39 (KAN-35) Criterio 3: mensaje vacío amigable y propio de cada sección
      final emptyMessage = isTask
          ? 'Todavía no tienes tareas asignadas.\n¡Vuelve a revisar más tarde!'
          : 'Todavía no tienes turnos asignados.\n¡Vuelve a revisar más tarde!';
      return RefreshIndicator(
        onRefresh: _loadAssignments,
        child: ListView(
          children: [
            const SizedBox(height: 120),
            Center(
              child: Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: _salviaClaro.withValues(alpha: 0.4),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  isTask ? Icons.task_alt : Icons.event_available,
                  size: 40,
                  color: _verdeOscuro,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Center(
              child: Text(
                emptyMessage,
                textAlign: TextAlign.center,
                style: const TextStyle(color: _textoSuave, fontSize: 14),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadAssignments,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: assignments.length,
        itemBuilder: (context, index) {
          final assignment = assignments[index];
          final status =
              assignment['estado']?.toString() ??
              assignment['estadoRespuesta']?.toString() ??
              'Pendiente';
          final pending = _isPending(status);
          final title = isTask
              ? assignment['titulo']?.toString() ?? 'Tarea'
              : assignment['actividad']?.toString() ?? 'Turno';
          final details = isTask
              ? assignment['descripcion']?.toString() ?? ''
              : _shiftSchedule(assignment);
          // RF-38 (KAN-35) Criterio 1: la tarea muestra su fecha de asignación
          final date = isTask ? _taskDate(assignment) : '';

          return Card(
            margin: const EdgeInsets.only(bottom: 14),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(9),
                        decoration: BoxDecoration(
                          color: (isTask ? _durazno : _salviaClaro)
                              .withValues(alpha: 0.5),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(
                          isTask ? Icons.task_alt : Icons.event_available,
                          size: 20,
                          color: _texto,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              title,
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                                color: _texto,
                              ),
                            ),
                            if (date.isNotEmpty) ...[
                              const SizedBox(height: 2),
                              Text(
                                date,
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: _textoSuave,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      _estadoChip(status),
                    ],
                  ),
                  if (details.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    Text(
                      details,
                      style: const TextStyle(fontSize: 13, color: _textoSuave),
                    ),
                  ],
                  if (pending) ...[
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        Expanded(
                          child: FilledButton.icon(
                            onPressed: () => _respond(
                              isTask: isTask,
                              assignment: assignment,
                              accept: true,
                            ),
                            icon: const Icon(Icons.check, size: 18),
                            label: const Text('Aceptar'),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _respond(
                              isTask: isTask,
                              assignment: assignment,
                              accept: false,
                            ),
                            icon: const Icon(Icons.close, size: 18),
                            label: const Text('Rechazar'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  // RF-38 (KAN-35) Criterio 1: formatea la fecha de asignación de la tarea (ISO -> dd/MM/yyyy)
  String _taskDate(Map<String, dynamic> task) {
    final raw = task['fechaAsignacion']?.toString();
    if (raw == null || raw.isEmpty) return '';
    final parsed = DateTime.tryParse(raw);
    if (parsed == null) return '';
    final day = parsed.day.toString().padLeft(2, '0');
    final month = parsed.month.toString().padLeft(2, '0');
    return '$day/$month/${parsed.year}';
  }

  String _shiftSchedule(Map<String, dynamic> shift) {
    final date = shift['fecha']?.toString() ?? '';
    final start = shift['horaInicio']?.toString() ?? '';
    final end = shift['horaFin']?.toString() ?? '';
    return [
      date,
      '$start - $end',
    ].where((part) => part.trim().isNotEmpty).join(' · ');
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          color: Colors.white,
          padding: const EdgeInsets.fromLTRB(20, 14, 20, 0),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  widget.email,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 12, color: _textoSuave),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: _crema,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  widget.role,
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: _texto,
                  ),
                ),
              ),
            ],
          ),
        ),
        Material(
          color: Colors.white,
          child: TabBar(
            controller: _tabController,
            labelColor: _verdeOscuro,
            unselectedLabelColor: _textoSuave,
            indicatorColor: _verdeOscuro,
            tabs: const [
              Tab(icon: Icon(Icons.task_alt), text: 'Tareas'),
              Tab(icon: Icon(Icons.event_available), text: 'Turnos'),
            ],
          ),
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              _assignmentList(_tasks, isTask: true),
              _assignmentList(_shifts, isTask: false),
            ],
          ),
        ),
      ],
    );
  }
}

class NotificationsCenterPage extends StatefulWidget {
  const NotificationsCenterPage({super.key, required this.token});

  final String token;

  @override
  State<NotificationsCenterPage> createState() =>
      _NotificationsCenterPageState();
}

class _NotificationsCenterPageState extends State<NotificationsCenterPage> {
  bool _loading = true;
  String? _error;
  List<Map<String, dynamic>> _notifications = [];

  @override
  void initState() {
    super.initState();
    _loadNotifications();
  }

  Future<void> _loadNotifications() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final response = await http.get(
        Uri.parse('$_apiBaseUrl/movil/notificaciones'),
        headers: {'Authorization': 'Bearer ${widget.token}'},
      );
      if (response.statusCode != 200) {
        throw Exception('No se pudieron cargar tus notificaciones.');
      }
      final decoded =
          jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>;
      if (!mounted) return;
      setState(() {
        _notifications = decoded.cast<Map<String, dynamic>>();
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openNotification(Map<String, dynamic> notification) async {
    if (notification['leida'] != true) {
      try {
        final response = await http.patch(
          Uri.parse(
            '$_apiBaseUrl/movil/notificaciones/${notification['id']}/leida',
          ),
          headers: {'Authorization': 'Bearer ${widget.token}'},
        );
        if (response.statusCode != 204) {
          final message = utf8.decode(response.bodyBytes).trim();
          throw Exception(
            message.isEmpty
                ? 'No se pudo marcar la notificación como leída.'
                : message,
          );
        }
        if (!mounted) return;
        setState(() {
          notification['leida'] = true;
        });
      } catch (error) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(error.toString().replaceFirst('Exception: ', '')),
          ),
        );
        return;
      }
    }

    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(notification['tipo']?.toString() ?? 'Notificación'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(notification['actividad']?.toString() ?? ''),
            if ((notification['fechaActividad']?.toString() ?? '')
                .isNotEmpty) ...[
              const SizedBox(height: 12),
              Text('Fecha: ${notification['fechaActividad']}'),
            ],
            if ((notification['fechaAviso']?.toString() ?? '').isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('Recibida: ${_formatDate(notification['fechaAviso'])}'),
            ],
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }

  String _formatDate(Object? value) {
    if (value == null) return '';
    final date = DateTime.tryParse(value.toString())?.toLocal();
    if (date == null) return value.toString();
    String twoDigits(int number) => number.toString().padLeft(2, '0');
    return '${twoDigits(date.day)}/${twoDigits(date.month)}/${date.year} '
        '${twoDigits(date.hour)}:${twoDigits(date.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: _errorFondo,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: _errorTexto.withValues(alpha: 0.25)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, size: 18, color: _errorTexto),
                    const SizedBox(width: 10),
                    Flexible(
                      child: Text(
                        _error!,
                        style: const TextStyle(color: _errorTexto, fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              OutlinedButton.icon(
                onPressed: _loadNotifications,
                icon: const Icon(Icons.refresh),
                label: const Text('Reintentar'),
              ),
            ],
          ),
        ),
      );
    }
    if (_notifications.isEmpty) {
      return RefreshIndicator(
        onRefresh: _loadNotifications,
        child: ListView(
          children: [
            const SizedBox(height: 120),
            Center(
              child: Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: _salviaClaro.withValues(alpha: 0.4),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.notifications_none,
                  size: 40,
                  color: _verdeOscuro,
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Center(
              child: Text(
                'No tienes avisos recientes.',
                style: TextStyle(color: _textoSuave, fontSize: 14),
              ),
            ),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _loadNotifications,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _notifications.length,
        separatorBuilder: (context, index) => const SizedBox(height: 10),
        itemBuilder: (context, index) {
          final notification = _notifications[index];
          final isRead = notification['leida'] == true;
          return Card(
            key: Key('notification-${notification['id']}'),
            margin: EdgeInsets.zero,
            child: InkWell(
              borderRadius: BorderRadius.circular(14),
              onTap: () => _openNotification(notification),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(9),
                      decoration: BoxDecoration(
                        color: (isRead ? _crema : _durazno)
                            .withValues(alpha: isRead ? 0.5 : 0.55),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Icon(
                        isRead
                            ? Icons.notifications_none
                            : Icons.notifications_active,
                        size: 20,
                        color: _texto,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            notification['tipo']?.toString() ?? 'Notificación',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight:
                                  isRead ? FontWeight.w500 : FontWeight.w700,
                              color: _texto,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            [
                              notification['actividad']?.toString() ?? '',
                              _formatDate(notification['fechaAviso']),
                            ].where((part) => part.isNotEmpty).join(' · '),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 12,
                              color: _textoSuave,
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (!isRead) ...[
                      const SizedBox(width: 8),
                      const Padding(
                        padding: EdgeInsets.only(top: 4),
                        child: Icon(Icons.circle, size: 9, color: _verdeOscuro),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
