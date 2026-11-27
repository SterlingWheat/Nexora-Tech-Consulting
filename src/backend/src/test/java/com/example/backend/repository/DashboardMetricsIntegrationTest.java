package com.example.backend.repository;

import com.example.backend.model.AsignacionTarea;
import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Turno;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RF-44 (KAN-49): valida, contra la base de datos real configurada en .env (sin mocks), que
 * las consultas de conteo del dashboard son exactas. Como la base ya tiene datos de otras
 * pruebas y del equipo, cada caso compara el conteo ANTES y DESPUÉS de insertar filas
 * conocidas (delta), en vez de asumir un total absoluto. Cada prueba corre en una
 * transacción que se revierte al finalizar.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// AsignacionTurnoRepository es una clase @Repository basada en JdbcTemplate (no una interfaz de
// Spring Data), así que el slice de @DataJpaTest no la detecta sola: hay que importarla.
@Import(AsignacionTurnoRepository.class)
class DashboardMetricsIntegrationTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private VoluntarioRepository voluntarioRepository;
    @Autowired private TareaRepository tareaRepository;
    @Autowired private AsignacionTareaRepository asignacionTareaRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private AsignacionTurnoRepository asignacionTurnoRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Voluntario nuevoVoluntario(String sufijo, boolean activo, Usuario usuario) {
        return entityManager.persistFlushFind(Voluntario.builder()
                .nombre("IT").apellido("Dashboard")
                .documento("IT-DASH-" + sufijo).email("it-dash-" + sufijo + "@example.test")
                .activo(activo).usuario(usuario).build());
    }

    private CategoriaTarea nuevaCategoria(String sufijo) {
        return entityManager.persistFlushFind(CategoriaTarea.builder().nombre("Dash-" + sufijo).build());
    }

    private Tarea nuevaTarea(String sufijo, CategoriaTarea categoria, String estado) {
        return entityManager.persistFlushFind(
                Tarea.builder().titulo("Dash-" + sufijo).categoria(categoria).estado(estado).build());
    }

    private Turno nuevoTurno(LocalDate fecha, String estado) {
        Turno turno = Turno.builder()
                .fecha(fecha).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(12, 0))
                .actividad("Dashboard-IT").cuposTotales(3).build();
        Turno guardado = entityManager.persistFlushFind(turno);
        if (!"ACTIVO".equals(estado)) {
            jdbcTemplate.update("update turno set estado = ? where id_turno = ?", estado, guardado.getIdTurno());
        }
        return guardado;
    }

    // RF-44 Criterio 1: voluntarios activos
    @Test
    void contarActivos_cuentaSoloLosRealmenteActivos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        long antes = voluntarioRepository.contarActivos();

        nuevoVoluntario(sufijo + "-a", true, null);

        var rol = rolRepository.findByNombre("VOLUNTARIO")
                .orElseThrow(() -> new IllegalStateException("No existe el rol VOLUNTARIO"));
        Usuario usuarioInactivo = entityManager.persistFlushFind(Usuario.builder()
                .nombre("IT").email("it-dash-usr-" + sufijo + "@example.test")
                .password("hash").rol(rol).estado("INACTIVO").build());
        nuevoVoluntario(sufijo + "-b", true, usuarioInactivo); // activo=true pero su cuenta está INACTIVA
        nuevoVoluntario(sufijo + "-c", false, null); // inactivo directamente
        entityManager.clear();

        long despues = voluntarioRepository.contarActivos();

        // Solo el primero (activo=true y sin cuenta, o con cuenta ACTIVA) debe contarse
        assertEquals(antes + 1, despues);
    }

    // RF-44 Criterio 1: tareas abiertas (y su desglose por estado para el gráfico)
    @Test
    void contarAbiertasYPorEstado_soloCuentaLasQueNoLlegaronAUnEstadoTerminal() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        CategoriaTarea categoria = nuevaCategoria(sufijo);
        long abiertasAntes = tareaRepository.contarAbiertas(Tarea.ESTADOS_TERMINALES);

        nuevaTarea(sufijo + "-pendiente", categoria, "Pendiente");
        nuevaTarea(sufijo + "-progreso", categoria, "En progreso");
        nuevaTarea(sufijo + "-finalizada", categoria, "Finalizada");
        nuevaTarea(sufijo + "-cancelada", categoria, "Cancelada");
        entityManager.clear();

        long abiertasDespues = tareaRepository.contarAbiertas(Tarea.ESTADOS_TERMINALES);
        assertEquals(abiertasAntes + 2, abiertasDespues);

        List<ConteoPorEstado> porEstado = tareaRepository.contarPorEstado();
        long pendientes = porEstado.stream()
                .filter(c -> "Pendiente".equals(c.getEstado()))
                .mapToLong(c -> c.getCantidad())
                .sum();
        // Al menos la que acabamos de crear debe estar reflejada en el desglose
        assertEquals(true, pendientes >= 1);
    }

    // RF-44 Criterio 1: turnos próximos (vigentes desde hoy, no cancelados)
    @Test
    void contarTurnosProximos_excluyePasadosYCancelados() {
        long antes = turnoRepository.countByFechaGreaterThanEqualAndEstadoNot(LocalDate.now(), "CANCELADO");

        nuevoTurno(LocalDate.now().plusDays(5), "ACTIVO");
        nuevoTurno(LocalDate.now().minusDays(5), "ACTIVO"); // pasado: no debe contarse
        nuevoTurno(LocalDate.now().plusDays(5), "CANCELADO"); // cancelado: no debe contarse
        entityManager.clear();

        long despues = turnoRepository.countByFechaGreaterThanEqualAndEstadoNot(LocalDate.now(), "CANCELADO");
        assertEquals(antes + 1, despues);
    }

    // RF-44 Criterio 1: asignaciones de tarea pendientes de respuesta
    @Test
    void contarAsignacionesDeTareaPendientes_soloCuentaLasSinRespuestaConfirmada() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        CategoriaTarea categoria = nuevaCategoria(sufijo);
        // Una asignación por tarea+voluntario (restricción única de la tabla): se usan dos tareas
        Tarea tareaPendiente = nuevaTarea(sufijo + "-pend", categoria, "Asignada");
        Tarea tareaConfirmada = nuevaTarea(sufijo + "-conf", categoria, "Asignada");
        Voluntario voluntario = nuevoVoluntario(sufijo, true, null);

        long antes = asignacionTareaRepository.countPendientes(Tarea.ESTADOS_TERMINALES);

        entityManager.persistFlushFind(AsignacionTarea.builder()
                .tarea(tareaPendiente).voluntario(voluntario).estadoRespuesta("Pendiente").build());
        entityManager.persistFlushFind(AsignacionTarea.builder()
                .tarea(tareaConfirmada).voluntario(voluntario).estadoRespuesta("Confirmada").build());
        entityManager.clear();

        long despues = asignacionTareaRepository.countPendientes(Tarea.ESTADOS_TERMINALES);
        assertEquals(antes + 1, despues);
    }

    // Bug corregido (QA): una asignación "Asignado" de una tarea ya Cancelada no debe seguir
    // inflando el KPI, porque cancelar una tarea no cancela en cascada sus asignaciones.
    @Test
    void contarAsignacionesDeTareaPendientes_excluyeLasDeTareasCanceladas() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        CategoriaTarea categoria = nuevaCategoria(sufijo);
        Tarea tareaCancelada = nuevaTarea(sufijo + "-cancelada-pend", categoria, "Cancelada");
        Voluntario voluntario = nuevoVoluntario(sufijo, true, null);

        long antes = asignacionTareaRepository.countPendientes(Tarea.ESTADOS_TERMINALES);

        entityManager.persistFlushFind(AsignacionTarea.builder()
                .tarea(tareaCancelada).voluntario(voluntario).estadoRespuesta("Asignado").build());
        entityManager.clear();

        long despues = asignacionTareaRepository.countPendientes(Tarea.ESTADOS_TERMINALES);
        assertEquals(antes, despues);
    }

    // RF-44 Criterio 1: asignaciones de turno pendientes de respuesta
    @Test
    void contarAsignacionesDeTurnoPendientes_soloCuentaLasSinRespuestaConfirmada() {
        Turno turno = nuevoTurno(LocalDate.now().plusDays(2), "ACTIVO");
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Voluntario voluntarioA = nuevoVoluntario(sufijo + "-a", true, null);
        Voluntario voluntarioB = nuevoVoluntario(sufijo + "-b", true, null);

        int antes = asignacionTurnoRepository.countPendientes();

        jdbcTemplate.update("insert into asignacion_turno (id_turno, id_voluntario, estado) values (?, ?, ?)",
                turno.getIdTurno(), voluntarioA.getIdVoluntario(), "Pendiente");
        jdbcTemplate.update("insert into asignacion_turno (id_turno, id_voluntario, estado) values (?, ?, ?)",
                turno.getIdTurno(), voluntarioB.getIdVoluntario(), "Confirmada");

        int despues = asignacionTurnoRepository.countPendientes();
        assertEquals(antes + 1, despues);
    }
}
