import 'dart:convert';

import 'package:app_flutter/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

// RF-38/39 (KAN-35): pruebas de la pantalla "Mis tareas y turnos" con datos vacíos y con
// datos, simulando las respuestas del backend mediante httpGetOverride/httpPatchOverride
// (ver main.dart), ya que la pantalla llama directamente a `http.get`/`http.patch` sin
// inyección de dependencias de por sí.
void main() {
  tearDown(() {
    httpGetOverride = http.get;
    httpPatchOverride = http.patch;
  });

  http.Response jsonResponse(Object body, {int statusCode = 200}) {
    return http.Response(jsonEncode(body), statusCode);
  }

  Future<void> pumpPage(
    WidgetTester tester, {
    required List<Map<String, dynamic>> tasks,
    required List<Map<String, dynamic>> shifts,
  }) async {
    httpGetOverride = (Uri url, {Map<String, String>? headers}) async {
      if (url.path.endsWith('/movil/asignaciones/tareas')) {
        return jsonResponse(tasks);
      }
      if (url.path.endsWith('/movil/asignaciones/turnos')) {
        return jsonResponse(shifts);
      }
      throw Exception('URL inesperada en la prueba: $url');
    };

    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: VolunteerAssignmentsPage(
            email: 'ana@example.com',
            role: 'VOLUNTARIO',
            token: 'token-de-prueba',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  group('VolunteerAssignmentsPage - sin asignaciones (Criterio 3)', () {
    testWidgets('muestra un mensaje vacío propio para la pestaña de tareas', (
      tester,
    ) async {
      await pumpPage(tester, tasks: [], shifts: []);

      expect(find.textContaining('no tienes tareas asignadas'), findsOneWidget);
    });

    testWidgets('muestra un mensaje vacío propio para la pestaña de turnos', (
      tester,
    ) async {
      await pumpPage(tester, tasks: [], shifts: []);

      await tester.tap(find.text('Turnos'));
      await tester.pumpAndSettle();

      expect(find.textContaining('no tienes turnos asignados'), findsOneWidget);
    });
  });

  group('VolunteerAssignmentsPage - con asignaciones (Criterios 1 y 2)', () {
    testWidgets('la tarea muestra título, fecha, detalle y estado', (
      tester,
    ) async {
      await pumpPage(
        tester,
        tasks: [
          {
            'idAsignacion': 101,
            'idTarea': 10,
            'titulo': 'Limpieza de sala',
            'descripcion': 'Barrer y trapear',
            'estado': 'Pendiente',
            'fechaAsignacion': '2026-11-20T09:00:00',
          },
        ],
        shifts: [],
      );

      expect(find.text('Limpieza de sala'), findsOneWidget);
      expect(find.text('20/11/2026'), findsOneWidget);
      expect(find.text('Barrer y trapear'), findsOneWidget);
      expect(find.text('Pendiente'), findsOneWidget);
    });

    testWidgets('los turnos se muestran ordenados y con su horario', (
      tester,
    ) async {
      await pumpPage(
        tester,
        tasks: [],
        shifts: [
          {
            'idAsignacion': 1,
            'idTurno': 7,
            'actividad': 'Reparto de alimentos',
            'fecha': '2026-11-23',
            'horaInicio': '08:00:00',
            'horaFin': '12:00:00',
            'estado': 'Confirmada',
          },
        ],
      );

      await tester.tap(find.text('Turnos'));
      await tester.pumpAndSettle();

      expect(find.text('Reparto de alimentos'), findsOneWidget);
      expect(find.textContaining('2026-11-23'), findsOneWidget);
      expect(find.text('Confirmada'), findsOneWidget);
    });

    testWidgets('aceptar una tarea pendiente llama al endpoint de confirmación y recarga', (
      tester,
    ) async {
      var confirmCalled = false;
      await pumpPage(
        tester,
        tasks: [
          {
            'idAsignacion': 101,
            'idTarea': 10,
            'titulo': 'Limpieza de sala',
            'descripcion': 'Barrer y trapear',
            'estado': 'Pendiente',
            'fechaAsignacion': '2026-11-20T09:00:00',
          },
        ],
        shifts: [],
      );

      httpPatchOverride = (Uri url, {Map<String, String>? headers}) async {
        expect(
          url.path,
          endsWith('/movil/asignaciones/tareas/10/101/confirmar'),
        );
        confirmCalled = true;
        return http.Response('', 204);
      };

      await tester.tap(find.widgetWithText(FilledButton, 'Aceptar'));
      await tester.pumpAndSettle();
      // El diálogo de confirmación debe aparecer antes de llamar al backend
      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar'));
      await tester.pumpAndSettle();

      expect(confirmCalled, isTrue);
    });
  });
}
