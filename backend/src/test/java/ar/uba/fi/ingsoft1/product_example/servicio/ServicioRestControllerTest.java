package ar.uba.fi.ingsoft1.product_example.servicio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.uba.fi.ingsoft1.product_example.agenda.Agenda;
import ar.uba.fi.ingsoft1.product_example.agenda.AgendaRepository;
import ar.uba.fi.ingsoft1.product_example.agenda.DiaSemana;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtService;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtUserDetails;
import ar.uba.fi.ingsoft1.product_example.user.SectorTrabajo;
import ar.uba.fi.ingsoft1.product_example.user.User;
import ar.uba.fi.ingsoft1.product_example.user.UserRepository;
import ar.uba.fi.ingsoft1.product_example.user.UserRole;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServicioRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    private User profesional;
    private User cliente;
    private Agenda agenda;
    private String tokenProfesional;
    private String tokenCliente;

    @BeforeEach
    void setup() {
        profesional = userRepository.save(
            new User(
                "prof@test.com",
                "encoded",
                UserRole.PROFESIONAL,
                "Carlos",
                "Lopez",
                LocalDate.of(1990, 1, 1),
                "Médico",
                SectorTrabajo.SALUD
            )
        );
        cliente = userRepository.save(
            new User(
                "cli@test.com",
                "encoded",
                UserRole.CLIENTE,
                "Maria",
                "Garcia",
                LocalDate.of(2000, 1, 1),
                null,
                null
            )
        );

        agenda = new Agenda(profesional, 30, 1);
        List.of(
            DiaSemana.LUNES,
            DiaSemana.MARTES,
            DiaSemana.MIERCOLES,
            DiaSemana.JUEVES,
            DiaSemana.VIERNES
        ).forEach(dia ->
            agenda.addRango(dia, LocalTime.of(8, 0), LocalTime.of(17, 0))
        );
        agenda = agendaRepository.save(agenda);

        tokenProfesional =
            "Bearer " +
            jwtService.createToken(
                new JwtUserDetails(
                    profesional.getUsername(),
                    UserRole.PROFESIONAL,
                    true
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
    }

    @Test
    void listarServiciosDeAgendaDevuelveListaVacia() throws Exception {
        mockMvc
            .perform(
                get("/servicio/agenda/" + agenda.getId()).header(
                    "Authorization",
                    tokenCliente
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listarServiciosDeAgendaConServiciosCargados() throws Exception {
        servicioRepository.save(new Servicio(agenda, "Consulta", 30, 5000.0));
        servicioRepository.save(new Servicio(agenda, "Control", 15, 3000.0));

        mockMvc
            .perform(
                get("/servicio/agenda/" + agenda.getId()).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].nombre").value("Consulta"))
            .andExpect(jsonPath("$[1].nombre").value("Control"));
    }

    @Test
    void listarServiciosDeAgendaInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                get("/servicio/agenda/99999").header(
                    "Authorization",
                    tokenCliente
                )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void profesionalPuedeCrearServicio() throws Exception {
        mockMvc
            .perform(
                post("/servicio")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "agendaId": %d,
                                "nombre": "Consulta",
                                "precio": 5000.0
                            }
                        """.formatted(agenda.getId())
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Consulta"))
            .andExpect(jsonPath("$.precio").value(5000.0));
    }

    @Test
    void clienteNoPuedeCrearServicio() throws Exception {
        mockMvc
            .perform(
                post("/servicio")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "agendaId": %d,
                                "nombre": "Consulta",
                                "precio": 5000.0
                            }
                        """.formatted(agenda.getId())
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalNoPuedeCrearServicioEnAgendaAjena() throws Exception {
        User otroProfesional = userRepository.save(
            new User(
                "otro@prof.com",
                "encoded",
                UserRole.PROFESIONAL,
                "Otro",
                "Prof",
                LocalDate.of(1985, 3, 3),
                "Abogado",
                SectorTrabajo.DERECHO
            )
        );
        String tokenOtro =
            "Bearer " +
            jwtService.createToken(
                new JwtUserDetails(
                    otroProfesional.getUsername(),
                    UserRole.PROFESIONAL,
                    true
                )
            );

        mockMvc
            .perform(
                post("/servicio")
                    .header("Authorization", tokenOtro)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "agendaId": %d,
                                "nombre": "Consulta",
                                "precio": 5000.0
                            }
                        """.formatted(agenda.getId())
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeEliminarSuServicio() throws Exception {
        Servicio servicio = servicioRepository.save(
            new Servicio(agenda, "Consulta", 30, 5000.0)
        );

        mockMvc
            .perform(
                delete("/servicio/" + servicio.getId()).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void clienteNoPuedeEliminarServicio() throws Exception {
        Servicio servicio = servicioRepository.save(
            new Servicio(agenda, "Consulta", 30, 5000.0)
        );

        mockMvc
            .perform(
                delete("/servicio/" + servicio.getId()).header(
                    "Authorization",
                    tokenCliente
                )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void eliminarServicioInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                delete("/servicio/99999").header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void profesionalPuedeToglearActivoDeServicioPropio() throws Exception {
        Servicio servicio = servicioRepository.save(
            new Servicio(agenda, "Consulta", 30, 5000.0)
        );

        mockMvc
            .perform(
                patch(
                    "/servicio/" + servicio.getId() + "/toggle-activo"
                ).header("Authorization", tokenProfesional)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void clienteNoPuedeToglearActivo() throws Exception {
        Servicio servicio = servicioRepository.save(
            new Servicio(agenda, "Consulta", 30, 5000.0)
        );

        mockMvc
            .perform(
                patch(
                    "/servicio/" + servicio.getId() + "/toggle-activo"
                ).header("Authorization", tokenCliente)
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalNoPuedeToglearActivoDeServicioAjeno() throws Exception {
        User otroProfesional = userRepository.save(
            new User(
                "otro@prof.com",
                "encoded",
                UserRole.PROFESIONAL,
                "Otro",
                "Prof",
                LocalDate.of(1985, 3, 3),
                "Abogado",
                SectorTrabajo.DERECHO
            )
        );
        String tokenOtro =
            "Bearer " +
            jwtService.createToken(
                new JwtUserDetails(
                    otroProfesional.getUsername(),
                    UserRole.PROFESIONAL,
                    true
                )
            );
        Servicio servicio = servicioRepository.save(
            new Servicio(agenda, "Consulta", 30, 5000.0)
        );

        mockMvc
            .perform(
                patch(
                    "/servicio/" + servicio.getId() + "/toggle-activo"
                ).header("Authorization", tokenOtro)
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void toggleActivoEnServicioInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                patch("/servicio/99999/toggle-activo").header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isNotFound());
    }
}
