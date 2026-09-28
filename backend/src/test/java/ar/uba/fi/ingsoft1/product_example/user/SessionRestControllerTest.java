package ar.uba.fi.ingsoft1.product_example.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class SessionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private static final String EMAIL = "maria@test.com";
    private static final String PASSWORD = "Password123!";

    @BeforeEach
    void setup() {
        var encoder = new BCryptPasswordEncoder();
        userRepository.save(
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
    }

    @Test
    void loginConCredencialesCorrectasDevuelve201() throws Exception {
        mockMvc
            .perform(
                post("/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                        """.formatted(EMAIL, PASSWORD)
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.roles").exists());
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc
            .perform(
                post("/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "email": "%s",
                                "password": "passwordIncorrecta"
                            }
                        """.formatted(EMAIL)
                    )
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConEmailInvalidoDevuelve400() throws Exception {
        mockMvc
            .perform(
                post("/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "email": "esto-no-es-un-email",
                                "password": "cualquiera"
                            }
                        """
                    )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void refreshConTokenInvalidoDevuelve401() throws Exception {
        mockMvc
            .perform(
                put("/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "refreshToken": "token-inventado-que-no-existe"
                            }
                        """
                    )
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recuperarPasswordConEmailExistenteDevuelve200() throws Exception {
        mockMvc
            .perform(
                post("/sessions/recover")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "email": "%s"
                            }
                        """.formatted(EMAIL)
                    )
            )
            .andExpect(status().isOk());
    }

    @Test
    void resetPasswordConTokenInvalidoDevuelve400() throws Exception {
        mockMvc
            .perform(
                post("/sessions/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token": "token-invalido",
                                "newPassword": "NuevaPassword123!"
                            }
                        """
                    )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginConCuentaDesactivadaDevuelve403() throws Exception {
        var encoder = new BCryptPasswordEncoder();
        var usuario = new User(
            "desactivado@test.com",
            encoder.encode(PASSWORD),
            UserRole.CLIENTE,
            "Carlos",
            "López",
            LocalDate.of(1990, 6, 15),
            null,
            null
        );
        usuario.setActive(false);
        userRepository.save(usuario);

        mockMvc
            .perform(
                post("/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "email": "desactivado@test.com",
                                "password": "%s"
                            }
                        """.formatted(PASSWORD)
                    )
            )
            .andExpect(status().isForbidden());
    }
}
