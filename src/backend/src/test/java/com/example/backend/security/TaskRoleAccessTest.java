package com.example.backend.security;

import com.example.backend.controller.TareaController;
import com.example.backend.controller.PerfilController;
import com.example.backend.controller.UsuarioController;
import com.example.backend.controller.EmailTestController;
import com.example.backend.controller.NotificationTemplateController;
import com.example.backend.dto.NotificationEmailRequest;
import com.example.backend.dto.NotificationTemplateResponse;
import com.example.backend.dto.NotificationTemplateUpdateRequest;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Tarea;
import com.example.backend.model.Usuario;
import com.example.backend.repository.RolRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.JwtService;
import com.example.backend.service.TareaService;
import com.example.backend.service.SmtpEmailService;
import com.example.backend.mail.NotificationTemplateService;
import com.example.backend.mail.NotificationTemplateType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({
        TareaController.class,
        PerfilController.class,
        UsuarioController.class,
        EmailTestController.class,
        NotificationTemplateController.class
})
@Import({com.example.backend.SecurityConfig.class, JwtAuthenticationFilter.class})
class TaskRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TareaService tareaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private RolRepository rolRepository;

    @MockitoBean
    private VoluntarioRepository voluntarioRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SmtpEmailService smtpEmailService;

    @MockitoBean
    private NotificationTemplateService notificationTemplateService;

    @Test
    void voluntarioPuedeConsultarTareas() throws Exception {
        when(tareaService.listarTareas()).thenReturn(List.of());
        mockMvc.perform(get("/api/tareas").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void tiPuedeConsultarTareas() throws Exception {
        when(tareaService.listarTareas()).thenReturn(List.of());
        mockMvc.perform(get("/api/tareas").with(user("ti").roles("TI")))
                .andExpect(status().isOk());
    }

    @Test
    void gestorPuedeCrearTareas() throws Exception {
        when(tareaService.crearTarea(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/tareas")
                        .with(user("gestor").roles("GESTOR_TAREAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Actividad","descripcion":"Detalle","idCategoria":1}
                                """))
                .andExpect(status().isCreated());

        verify(tareaService).crearTarea(any(Tarea.class));
    }

    @Test
    void administradorPuedeCrearTareas() throws Exception {
        when(tareaService.crearTarea(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/tareas")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Actividad","descripcion":"Detalle","idCategoria":1}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void voluntarioRecibe403AlCrearTareas() throws Exception {
        mockMvc.perform(post("/api/tareas")
                        .with(user("voluntario").roles("VOLUNTARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Actividad","descripcion":"Detalle","idCategoria":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void tiRecibe403AlCrearTareas() throws Exception {
        mockMvc.perform(post("/api/tareas")
                        .with(user("ti").roles("TI"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Actividad","descripcion":"Detalle","idCategoria":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorPuedeEditarTareas() throws Exception {
        when(tareaService.editarTarea(any(), any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(put("/api/tareas/1")
                        .with(user("gestor").roles("GESTOR_TAREAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Actividad","descripcion":"Detalle","idCategoria":1}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void voluntarioRecibe403AlEditarTareas() throws Exception {
        mockMvc.perform(put("/api/tareas/1")
                        .with(user("voluntario").roles("VOLUNTARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Actividad","descripcion":"Detalle","idCategoria":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorPuedeRegistrarAvance() throws Exception {
        when(usuarioRepository.findByEmail("gestor")).thenReturn(java.util.Optional.of(usuario(2, "gestor", new RolEntity(3, "GESTOR_TAREAS"))));
        when(tareaService.registrarAvance(any(), any(), any())).thenReturn(new Tarea());

        mockMvc.perform(patch("/api/tareas/1/avance")
                        .with(user("gestor").roles("GESTOR_TAREAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"avance":50, "observacion":"Todo correcto"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void voluntarioRecibe403AlRegistrarAvance() throws Exception {
        mockMvc.perform(patch("/api/tareas/1/avance")
                        .with(user("voluntario").roles("VOLUNTARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"avance":50, "observacion":"Todo correcto"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void tiRecibe403AlConsultarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios").with(user("ti").roles("TI")))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticionSinAutenticacionRecibe401() throws Exception {
        mockMvc.perform(get("/api/tareas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorPuedeEnviarCorreoDePrueba() throws Exception {
        mockMvc.perform(post("/api/admin/email/test")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"destinatario@example.com"}
                                """))
                .andExpect(status().isOk());

        verify(smtpEmailService).sendEmail(any(), any(), any(), any());
    }

    @Test
    void usuarioNoAdministradorNoPuedeEnviarCorreoDePrueba() throws Exception {
        mockMvc.perform(post("/api/admin/email/test")
                        .with(user("voluntario").roles("VOLUNTARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"destinatario@example.com"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(smtpEmailService);
    }

    @Test
    void administradorPuedeConsultarPlantillasYUsuarioSinRolAdminRecibe403() throws Exception {
        when(notificationTemplateService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/email-templates")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/email-templates")
                        .with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isForbidden());

        verify(notificationTemplateService).list();
    }

    @Test
    void usuarioSinRolAdminNoPuedeEditarPlantillas() throws Exception {
        mockMvc.perform(put("/api/admin/email-templates/ASIGNACION")
                        .with(user("gestor").roles("GESTOR_TAREAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject":"Asignación {{actividad}}",
                                  "htmlBody":"<p>{{nombre}} {{actividad}} {{fecha}}</p>",
                                  "textBody":"{{nombre}} {{actividad}} {{fecha}}",
                                  "active":true
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void administradorPuedeEditarYEnviarUnaPlantilla() throws Exception {
        NotificationTemplateResponse updated = new NotificationTemplateResponse(
                "ASIGNACION",
                "Asignación",
                "Nueva actividad: {{actividad}}",
                "<p>{{nombre}} {{actividad}} {{fecha}}</p>",
                "{{nombre}} {{actividad}} {{fecha}}",
                true,
                java.time.LocalDateTime.now());
        when(notificationTemplateService.update(
                eq(NotificationTemplateType.ASIGNACION), any(NotificationTemplateUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/admin/email-templates/ASIGNACION")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject":"Nueva actividad: {{actividad}}",
                                  "htmlBody":"<p>{{nombre}} {{actividad}} {{fecha}}</p>",
                                  "textBody":"{{nombre}} {{actividad}} {{fecha}}",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Nueva actividad: {{actividad}}"));

        mockMvc.perform(post("/api/admin/email-templates/ASIGNACION/send")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient":"ana@example.com",
                                  "name":"Ana",
                                  "activity":"Limpieza",
                                  "date":"20/12/2026"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(notificationTemplateService).update(
                eq(NotificationTemplateType.ASIGNACION), any(NotificationTemplateUpdateRequest.class));
        verify(notificationTemplateService).send(
                eq(NotificationTemplateType.ASIGNACION), any(NotificationEmailRequest.class));
    }

    @Test
    void voluntarioPuedeConsultarSoloSuPerfil() throws Exception {
        Usuario voluntario = usuario(7, "voluntario@example.com", new RolEntity(4, "VOLUNTARIO"));
        when(usuarioRepository.findByEmail("voluntario@example.com"))
                .thenReturn(java.util.Optional.of(voluntario));

        mockMvc.perform(get("/api/perfil/me").with(user("voluntario@example.com").roles("VOLUNTARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("voluntario@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(usuarioRepository).findByEmail("voluntario@example.com");
    }

    @Test
    void administradorPuedeConsultarElPerfilDeOtroUsuario() throws Exception {
        when(usuarioRepository.findById(7))
                .thenReturn(java.util.Optional.of(usuario(7, "voluntario@example.com", new RolEntity(4, "VOLUNTARIO"))));

        mockMvc.perform(get("/api/usuarios/7").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("voluntario@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void voluntarioRecibe403AlConsultarElPerfilDeOtro() throws Exception {
        mockMvc.perform(get("/api/usuarios/7").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioRepository);
    }

    private Usuario usuario(Integer id, String email, RolEntity rol) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre("Voluntario")
                .email(email)
                .password("{bcrypt}hash-secreto")
                .rol(rol)
                .estado("ACTIVO")
                .build();
    }
}