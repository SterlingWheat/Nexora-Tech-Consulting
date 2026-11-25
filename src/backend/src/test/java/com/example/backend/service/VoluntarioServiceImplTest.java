package com.example.backend.service;

import com.example.backend.dto.HistorialUnificadoResponseDTO;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Habilidad;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.notification.AccountEmailNotificationEvent;
import com.example.backend.repository.HabilidadRepository;
import com.example.backend.repository.RolRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.VoluntarioServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoluntarioServiceImplTest {

    @Mock
    private VoluntarioRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private HabilidadRepository habilidadRepository;

    private VoluntarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoluntarioServiceImpl(repository, usuarioRepository, rolRepository, passwordEncoder, eventPublisher, habilidadRepository);
    }

    private Voluntario nuevo() {
        Voluntario v = new Voluntario();
        v.setNombre("  Ana ");
        v.setApellido("Quispe");
        v.setDocumento(" 1234567 ");
        v.setEmail(" ANA@Correo.com ");
        v.setTelefono("   ");
        return v;
    }

    @Test
    void registrar_conDatosValidosYHabilidades_quedaActivoNormalizadoYConHabilidades() {
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));
        Habilidad habilidad = Habilidad.builder().id(1L).nombre("Primeros Auxilios").build();
        when(habilidadRepository.findAllById(List.of(1L))).thenReturn(List.of(habilidad));

        Voluntario guardado = service.registrarVoluntario(nuevo(), false, null, List.of(1L));

        assertTrue(guardado.getActivo());
        assertEquals("Ana", guardado.getNombre());
        assertEquals("1234567", guardado.getDocumento());
        assertEquals("ana@correo.com", guardado.getEmail());
        assertNull(guardado.getTelefono());
        assertEquals(1, guardado.getHabilidades().size());
        assertTrue(guardado.getHabilidades().contains(habilidad));
    }

    @Test
    void registrar_documentoDuplicado_lanzaConflictoYNoGuarda() {
        when(repository.existsByDocumentoIgnoreCase("1234567")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.registrarVoluntario(nuevo(), false, null, null));

        assertTrue(ex.getMessage().contains("documento"));
        verify(repository, never()).save(any());
    }

    @Test
    void registrar_correoDuplicado_lanzaConflictoYNoGuarda() {
        when(repository.existsByDocumentoIgnoreCase(any())).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.registrarVoluntario(nuevo(), false, null, null));

        assertTrue(ex.getMessage().contains("correo"));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarPerfil_normalizaCamposSinModificarDocumentoNiRolYActualizaLaCuenta() {
        Usuario usuario = Usuario.builder()
                .idUsuario(15)
                .nombre("Nombre anterior")
                .email("ana@correo.com")
                .rol(new com.example.backend.model.RolEntity(4, "VOLUNTARIO"))
                .build();
        Voluntario voluntario = Voluntario.builder()
                .idVoluntario(7)
                .usuario(usuario)
                .nombre("Ana")
                .apellido("Quispe")
                .documento("DOC-123")
                .email("ana@correo.com")
                .build();
        
        when(repository.findByUsuarioIdUsuario(15)).thenReturn(Optional.of(voluntario));
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario actualizado = service.actualizarPerfil(
                15, "  María ", "  López ", " MARIA@CORREO.COM ", "  12345678  ");

        assertEquals("María", actualizado.getNombre());
        assertEquals("López", actualizado.getApellido());
        assertEquals("DOC-123", actualizado.getDocumento());
        assertEquals("maria@correo.com", actualizado.getEmail());
        assertEquals("12345678", actualizado.getTelefono());
        assertEquals("María López", usuario.getNombre());
        assertEquals("maria@correo.com", usuario.getEmail());
        assertEquals("VOLUNTARIO", usuario.getRol().getNombre());
        verify(usuarioRepository).save(usuario);
        verify(repository).save(voluntario);
    }

    @Test
    void actualizar_conDatosValidos_guardaCambios() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("Viejo").apellido("Apellido")
                .documento("111").email("viejo@correo.com").build();

        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario resultado = service.actualizarVoluntario(1, nuevo(), null);

        assertEquals("Ana", resultado.getNombre());
        assertEquals("ana@correo.com", resultado.getEmail());
        assertEquals(1, resultado.getIdVoluntario());
        assertTrue(resultado.getActivo());
    }

    @Test
    void actualizar_inexistente_lanzaNoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarVoluntario(99, nuevo(), null));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conDocumentoDeOtroVoluntario_lanzaConflicto() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("X").apellido("Y")
                .documento("111").email("x@correo.com").build();

        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByDocumentoIgnoreCaseAndIdVoluntarioNot("1234567", 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.actualizarVoluntario(1, nuevo(), null));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conCorreoDeOtroVoluntario_lanzaConflicto() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("X").apellido("Y")
                .documento("111").email("x@correo.com").build();

        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailIgnoreCaseAndIdVoluntarioNot("ana@correo.com", 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.actualizarVoluntario(1, nuevo(), null));
        verify(repository, never()).save(any());
    }

    @Test
    void cambiarEstado_aInactivo_conservaElRegistroYSoloActualizaElEstado() {
        Voluntario voluntario = Voluntario.builder().idVoluntario(1).nombre("Ana").apellido("Quispe")
                .documento("111").email("ana@correo.com").activo(true).build();

        when(repository.findById(1)).thenReturn(Optional.of(voluntario));
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario resultado = service.cambiarEstado(1, false);

        assertFalse(resultado.getActivo());
        assertEquals(1, resultado.getIdVoluntario());
        assertEquals("Ana", resultado.getNombre());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cambiarEstado_aActivo_reactivaAlVoluntario() {
        Voluntario voluntario = Voluntario.builder().idVoluntario(1).nombre("Ana").apellido("Quispe")
                .documento("111").email("ana@correo.com").activo(false).build();

        when(repository.findById(1)).thenReturn(Optional.of(voluntario));
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario resultado = service.cambiarEstado(1, true);

        assertTrue(resultado.getActivo());
    }

    @Test
    void cambiarEstado_conUsuarioAsociado_sincronizaElAccesoYPublicaNotificacion() {
        Usuario usuario = Usuario.builder().idUsuario(9).nombre("Ana Quispe").email("ana@correo.com")
                .estado("ACTIVO").build();
        Voluntario voluntario = Voluntario.builder().idVoluntario(1).nombre("Ana").apellido("Quispe")
                .documento("111").email("ana@correo.com").activo(true).usuario(usuario).build();

        when(repository.findById(1)).thenReturn(Optional.of(voluntario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario resultado = service.cambiarEstado(1, false);

        assertFalse(resultado.getActivo());
        assertFalse(usuario.getActivo());
        verify(usuarioRepository).save(usuario);
        verify(eventPublisher).publishEvent(any(AccountEmailNotificationEvent.class));
    }

    @Test
    void cambiarEstado_inexistente_lanzaNoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cambiarEstado(99, false));
        verify(repository, never()).save(any());
    }

    @Test
    void buscarVoluntarios_conTextoConEspacios_armaElPatronLikeEnMinusculas() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Voluntario> pagina = new PageImpl<>(List.of(nuevo()), pageable, 1);
        when(repository.buscarVoluntarios(eq("%ana%"), isNull(), isNull(), isNull(), eq(pageable))).thenReturn(pagina);

        Page<Voluntario> resultado = service.buscarVoluntarios("  Ana  ", null, null, null, pageable);

        assertEquals(1, resultado.getTotalElements());
        verify(repository).buscarVoluntarios("%ana%", null, null, null, pageable);
    }

    @Test
    void buscarVoluntarios_conTextoEnBlanco_envianNullAlRepositorio() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.buscarVoluntarios(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.buscarVoluntarios("   ", null, null, null, pageable);

        verify(repository).buscarVoluntarios(null, null, null, null, pageable);
    }

    @Test
    void buscarVoluntarios_conFiltrosDeEstadoYDisponibilidadYCatologo_losPropagaAlRepositorio() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.buscarVoluntarios(isNull(), eq(true), eq(DiaSemana.LUNES), eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.buscarVoluntarios(null, true, DiaSemana.LUNES, 1L, pageable);

        verify(repository).buscarVoluntarios(null, true, DiaSemana.LUNES, 1L, pageable);
    }

    @Test
    void buscarVoluntarios_devuelveLaPaginaTalComoLaEntregaElRepositorio() {
        Pageable pageable = PageRequest.of(2, 10);
        Page<Voluntario> pagina = new PageImpl<>(List.of(nuevo(), nuevo()), pageable, 37);
        when(repository.buscarVoluntarios(any(), any(), any(), any(), eq(pageable))).thenReturn(pagina);

        Page<Voluntario> resultado = service.buscarVoluntarios(null, null, null, null, pageable);

        assertEquals(37, resultado.getTotalElements());
        assertEquals(4, resultado.getTotalPages());
        assertEquals(2, resultado.getNumber());
    }

    @Test
    void obtenerHistorialUnificado_verificaExistenciaVoluntario_retornaLista() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        
        List<HistorialUnificadoResponseDTO> historial = service.obtenerHistorialUnificado(1, LocalDate.now(), LocalDate.now());
        
        assertNotNull(historial);
        verify(repository).findById(1);
    }
}