import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

const String _apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api',
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
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF2F5D62)),
        useMaterial3: true,
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

class VolunteerHomePage extends StatelessWidget {
  const VolunteerHomePage({
    super.key,
    required this.email,
    required this.role,
    required this.token,
    required this.onLogout,
  });

  final String email;
  final String role;
  final String token;
  final Future<void> Function() onLogout;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mis asignaciones'),
        actions: [
          IconButton(
            tooltip: 'Cerrar sesión',
            onPressed: onLogout,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: VolunteerAssignmentsPage(email: email, role: role, token: token),
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
        http.get(
          Uri.parse('$_apiBaseUrl/movil/asignaciones/tareas'),
          headers: headers,
        ),
        http.get(
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
      final response = await http.patch(
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

  Widget _assignmentList(
    List<Map<String, dynamic>> assignments, {
    required bool isTask,
  }) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: _loadAssignments,
              child: const Text('Reintentar'),
            ),
          ],
        ),
      );
    }
    if (assignments.isEmpty) {
      return RefreshIndicator(
        onRefresh: _loadAssignments,
        child: ListView(
          children: const [
            SizedBox(height: 180),
            Center(child: Text('No tienes asignaciones.')),
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

          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.titleMedium),
                  if (details.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Text(details),
                  ],
                  const SizedBox(height: 8),
                  Chip(label: Text(status)),
                  if (pending) ...[
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      children: [
                        FilledButton.icon(
                          onPressed: () => _respond(
                            isTask: isTask,
                            assignment: assignment,
                            accept: true,
                          ),
                          icon: const Icon(Icons.check),
                          label: const Text('Aceptar'),
                        ),
                        OutlinedButton.icon(
                          onPressed: () => _respond(
                            isTask: isTask,
                            assignment: assignment,
                            accept: false,
                          ),
                          icon: const Icon(Icons.close),
                          label: const Text('Rechazar'),
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
        Material(
          color: Theme.of(context).colorScheme.surface,
          child: TabBar(
            controller: _tabController,
            tabs: const [
              Tab(text: 'Tareas'),
              Tab(text: 'Turnos'),
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
        Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: Text('${widget.email} · ${widget.role}'),
        ),
      ],
    );
  }
}
