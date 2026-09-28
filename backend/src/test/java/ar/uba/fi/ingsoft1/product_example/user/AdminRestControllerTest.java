package ar.uba.fi.ingsoft1.product_example.user;

import ar.uba.fi.ingsoft1.product_example.config.security.JwtService;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String tokenSuperAdmin;
    private String tokenCliente;
    private Long clienteId;

    @BeforeEach
    void setup() {
        var encoder = new BCryptPasswordEncoder();

        var superAdmin = userRepository.save(new User(
                "super@test.com", encoder.encode("Password123!"), UserRole.SUPER_ADMIN,
                "Admin", "User", LocalDate.of(1990, 1, 1), null, null
        ));
        var cliente = userRepository.save(new User(
                "cliente@test.com", encoder.encode("Password123!"), UserRole.CLIENTE,
                "Maria", "Garcia", LocalDate.of(2000, 1, 1), null, null
        ));

        clienteId = cliente.getId();

        tokenSuperAdmin = "Bearer " + jwtService.createToken(
                new JwtUserDetails(superAdmin.getUsername(), UserRole.SUPER_ADMIN, true));
        tokenCliente = "Bearer " + jwtService.createToken(
                new JwtUserDetails(cliente.getUsername(), UserRole.CLIENTE, true));
    }

    @Test
    void getUserAdminDetailComoSuperAdminDevuelve200() throws Exception {
        mockMvc.perform(get("/admin/users/" + clienteId)
                        .header("Authorization", tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@test.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE"));
    }

    @Test
    void getUserAdminDetailSinSuperAdminDevuelve403() throws Exception {
        mockMvc.perform(get("/admin/users/" + clienteId)
                        .header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserAdminDetailSinTokenDevuelve403() throws Exception {
        mockMvc.perform(get("/admin/users/" + clienteId))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminPuedeActualizarUsuario() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId)
                        .header("Authorization", tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nombre": "NuevoNombre",
                                "apellido": "NuevoApellido",
                                "email": "nuevo@test.com"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario actualizado"));
    }

    @Test
    void clienteNoPuedeActualizarUsuarioPorAdmin() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId)
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "nombre": "NuevoNombre",
                                "apellido": "NuevoApellido",
                                "email": "nuevo@test.com"
                            }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminPuedeEliminarUsuario() throws Exception {
        mockMvc.perform(delete("/admin/users/" + clienteId)
                        .header("Authorization", tokenSuperAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    void clienteNoPuedeEliminarUsuarioPorAdmin() throws Exception {
        mockMvc.perform(delete("/admin/users/" + clienteId)
                        .header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminPuedePromoverAClienteAProfesional() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId + "/role")
                        .header("Authorization", tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "profesion": "Psicólogo",
                                "sector": "SALUD_MENTAL",
                                "ubicacion": "Oficina central"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario promovido a profesional"));
    }

    @Test
    void clienteNoPuedePromoverUsuarios() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId + "/role")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "profesion": "Psicólogo",
                                "sector": "SALUD_MENTAL",
                                "ubicacion": "Oficina central"
                            }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminPuedeVerTurnosDeCliente() throws Exception {
        mockMvc.perform(get("/admin/users/" + clienteId + "/turnos")
                        .header("Authorization", tokenSuperAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeVerTurnosDeOtroPorAdmin() throws Exception {
        mockMvc.perform(get("/admin/users/" + clienteId + "/turnos")
                        .header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminPuedeDesactivarUsuario() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId + "/active")
                        .header("Authorization", tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "active": false }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta desactivada"));
    }

    @Test
    void superAdminPuedeActivarUsuario() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId + "/active")
                        .header("Authorization", tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "active": true }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta activada"));
    }

    @Test
    void clienteNoPuedeActivarDesactivarUsuario() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId + "/active")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "active": false }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void setActiveConCampoFaltanteDevuelve400() throws Exception {
        mockMvc.perform(put("/admin/users/" + clienteId + "/active")
                        .header("Authorization", tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserAdminDetailIncluyeCampoActive() throws Exception {
        mockMvc.perform(get("/admin/users/" + clienteId)
                        .header("Authorization", tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }
}
