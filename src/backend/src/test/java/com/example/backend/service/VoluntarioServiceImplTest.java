package com.example.backend.service;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.notification.AccountEmailNotificationEvent;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    private VoluntarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoluntarioServiceImpl(repository, usuarioRepository, rolRepository, passwordEncoder, eventPublisher);
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

    // Criterio 1
    @Test
    void registrar_conDatosValidos_quedaActivoYNormalizado() {
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario guardado = service.registrarVoluntario(nuevo(), false, null);

        assertTrue(guardado.getActivo());
        assertEquals("Ana", guardado.getNombre());
        assertEquals("1234567", guardado.getDocumento());
        assertEquals("ana@correo.com", guardado.getEmail());
        assertNull(guardado.getTelefono());
    }

    // Criterio 2
    @Test
    void registrar_documentoDuplicado_lanzaConflictoYNoGuarda() {
        when(repository.existsByDocumentoIgnoreCase("1234567")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.registrarVoluntario(nuevo(), false, null));

        assertTrue(ex.getMessage().contains("documento"));
        verify(repository, never()).save(any());
    }

    // Criterio 2
    @Test
    void registrar_correoDuplicado_lanzaConflictoYNoGuarda() {
        when(repository.existsByDocumentoIgnoreCase(any())).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.registrarVoluntario(nuevo(), false, null));

        assertTrue(ex.getMessage().contains("correo"));
        verify(repository, never()).save(any());
    }

    // Criterio 3
    @Test
    void actualizar_conDatosValidos_guardaCambios() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("Viejo").apellido("Apellido")
                .documento("111").email("viejo@correo.com").build();

        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario resultado = service.actualizarVoluntario(1, nuevo());

        assertEquals("Ana", resultado.getNombre());
        assertEquals("ana@correo.com", resultado.getEmail());
        assertEquals(1, resultado.getIdVoluntario());
        assertTrue(resultado.getActivo());
    }

    @Test
    void actualizar_inexistente_lanzaNoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarVoluntario(99, nuevo()));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conDocumentoDeOtroVoluntario_lanzaConflicto() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("X").apellido("Y")
                .documento("111").email("x@correo.com").build();

        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByDocumentoIgnoreCaseAndIdVoluntarioNot("1234567", 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.actualizarVoluntario(1, nuevo()));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conCorreoDeOtroVoluntario_lanzaConflicto() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("X").apellido("Y")
                .documento("111").email("x@correo.com").build();

        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailIgnoreCaseAndIdVoluntarioNot("ana@correo.com", 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.actualizarVoluntario(1, nuevo()));
        verify(repository, never()).save(any());
    }

    // Criterio 1: baja lógica conserva el registro, solo cambia el estado
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

    // Criterio 3: reactivación vuelve a estar disponible
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
}