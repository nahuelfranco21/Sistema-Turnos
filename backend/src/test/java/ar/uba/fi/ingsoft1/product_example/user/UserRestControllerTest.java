package ar.uba.fi.ingsoft1.product_example.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtService;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtUserDetails;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private static final String EMAIL = "maria@test.com";
    private static final String PASSWORD = "Password123!";

    private String tokenCliente;
    private String tokenProfesional;

    @BeforeEach
    void setup() {
        var encoder = new BCryptPasswordEncoder();

        var cliente = userRepository.save(
            new User(
                EMAIL,
                encoder.encode(PASSWORD),
                UserRole.CLIENTE,
                "María",
                "García",
                LocalDate.of(2000, 1, 1),
                null,
                null
            )
        );
        var profesional = userRepository.save(
            new User(
                "prof@test.com",
                encoder.encode(PASSWORD),
                UserRole.PROFESIONAL,
                "Carlos",
                "López",
                LocalDate.of(1990, 1, 1),
                "Profesor",
                SectorTrabajo.EDUCACION
            )
        );

        tokenCliente =
            "Bearer " +
            jwtService.createToken(
                new JwtUserDetails(
                    cliente.getUsername(),
                    UserRole.CLIENTE,
                    true
                )
            );
        tokenProfesional =
            "Bearer " +
            jwtService.createToken(
                new JwtUserDetails(
                    profesional.getUsername(),
                    UserRole.PROFESIONAL,
                    true
                )
            );
    }

    @Test
    void registrarNuevoUsuarioDevuelve201() throws Exception {
        mockMvc
            .perform(
                post("/users").contentType(MediaType.APPLICATION_JSON).content(
                    """
                        {
                            "email": "nuevo@test.com",
                            "password": "Password123!",
                            "nombre": "Juan",
                            "apellido": "Pérez",
                            "fechaNacimiento": "1995-06-15",
                            "esProfesional": false
                        }
                    """
                )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.roles").exists());
    }

    @Test
    void registrarEmailDuplicadoDevuelve409() throws Exception {
        mockMvc
            .perform(
                post("/users").contentType(MediaType.APPLICATION_JSON).content(
                    """
                        {
                            "email": "%s",
                            "password": "Password123!",
                            "nombre": "Otro",
                            "apellido": "Usuario",
                            "fechaNacimiento": "1995-06-15",
                            "esProfesional": false
                        }
                    """.formatted(EMAIL)
                )
            )
            .andExpect(status().isConflict());
    }

    @Test
    void registrarConFechaFuturaDevuelve400() throws Exception {
        mockMvc
            .perform(
                post("/users").contentType(MediaType.APPLICATION_JSON).content(
                    """
                        {
                            "email": "futuro@test.com",
                            "password": "Password123!",
                            "nombre": "Juan",
                            "apellido": "Pérez",
                            "fechaNacimiento": "2099-01-01",
                            "esProfesional": false
                        }
                    """
                )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerPerfilConTokenValidoDevuelve200() throws Exception {
        mockMvc
            .perform(get("/users/me").header("Authorization", tokenCliente))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.nombre").value("María"))
            .andExpect(jsonPath("$.apellido").value("García"));
    }

    @Test
    void obtenerPerfilSinTokenDevuelve403() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeBuscarClientes() throws Exception {
        mockMvc
            .perform(
                get("/users")
                    .header("Authorization", tokenProfesional)
                    .param("role", "CLIENTE")
                    .param("q", "")
            )
            .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeBuscarOtrosClientesSoloLosProfesionalesPueden()
        throws Exception {
        mockMvc
            .perform(
                get("/users")
                    .header("Authorization", tokenCliente)
                    .param("role", "CLIENTE")
                    .param("q", "")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void clientePuedeBuscarProfesionales() throws Exception {
        mockMvc
            .perform(
                get("/users")
                    .header("Authorization", tokenCliente)
                    .param("role", "PROFESIONAL")
                    .param("q", "")
            )
            .andExpect(status().isOk());
    }

    @Test
    void cambiarPasswordConCredencialesCorrectasDevuelve200() throws Exception {
        mockMvc
            .perform(
                put("/users/password")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "currentPassword": "%s",
                                "newPassword": "NuevaPassword123!"
                            }
                        """.formatted(PASSWORD)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Contraseña actualizada"));
    }

    @Test
    void cambiarPasswordConPasswordIncorrectaDevuelve400() throws Exception {
        mockMvc
            .perform(
                put("/users/password")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "currentPassword": "passwordIncorrecta",
                                "newPassword": "NuevaPassword123!"
                            }
                        """
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.error").value("Contraseña actual incorrecta")
            );
    }

    @Test
    void cambiarPasswordSinTokenDevuelve403() throws Exception {
        mockMvc
            .perform(
                put("/users/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "currentPassword": "cualquiera",
                                "newPassword": "NuevaPassword123!"
                            }
                        """
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void actualizarEmailConNuevoEmailDevuelve200() throws Exception {
        mockMvc
            .perform(
                put("/users/email")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "email": "nuevo@test.com"
                            }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.message").value(
                    "Email actualizado. Verificá tu nueva dirección de correo."
                )
            );
    }

    @Test
    void actualizarEmailSinAutenticacionDevuelve403() throws Exception {
        mockMvc
            .perform(
                put("/users/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "email": "nuevo@test.com"
                            }
                        """
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeActualizarSuProfesion() throws Exception {
        mockMvc
            .perform(
                put("/users/profesion")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "profesion": "Psicólogo",
                                "sector": "SALUD_MENTAL",
                                "ubicacion": "Oficina central"
                            }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Profesión actualizada"));
    }

    @Test
    void clienteNoPuedeActualizarProfesion() throws Exception {
        mockMvc
            .perform(
                put("/users/profesion")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "profesion": "Psicólogo",
                                "sector": "SALUD_MENTAL",
                                "ubicacion": "Oficina central"
                            }
                        """
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.error").value(
                    "Los clientes no pueden cambiar su profesión"
                )
            );
    }

    @Test
    void actualizarProfesionSinTokenDevuelve403() throws Exception {
        mockMvc
            .perform(
                put("/users/profesion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "profesion": "Psicólogo",
                                "sector": "SALUD_MENTAL",
                                "ubicacion": "Oficina central"
                            }
                        """
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void actualizarFotoPerfilDevuelve200() throws Exception {
        mockMvc
            .perform(
                put("/users/foto")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fotoPerfil": "data:image/png;base64,abc123"
                            }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Foto actualizada"));
    }

    @Test
    void actualizarFotoPerfilSinTokenDevuelve403() throws Exception {
        mockMvc
            .perform(
                put("/users/foto")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fotoPerfil": "data:image/png;base64,abc123"
                            }
                        """
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeActualizarDescripcion() throws Exception {
        mockMvc
            .perform(
                put("/users/descripcion")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "descripcion": "Especialista en kinesiología deportiva."
                            }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Descripción actualizada"));
    }

    @Test
    void actualizarDescripcionSinTokenDevuelve403() throws Exception {
        mockMvc
            .perform(
                put("/users/descripcion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "descripcion": "descripcion cualquiera"
                            }
                        """
                    )
            )
            .andExpect(status().isForbidden());
    }
}
