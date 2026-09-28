package ar.uba.fi.ingsoft1.product_example.turno;

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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
class TurnoRestControllerTest {

    private static LocalDate proximoDiaHabil(int daysAhead) {
        LocalDate d = LocalDate.now().plusDays(daysAhead);
        while (
            d.getDayOfWeek() == DayOfWeek.SATURDAY ||
            d.getDayOfWeek() == DayOfWeek.SUNDAY
        ) {
            d = d.plusDays(1);
        }
        return d;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

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
                "Profesor",
                SectorTrabajo.EDUCACION
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
    void clientePuedeVerSusTurnos() throws Exception {
        mockMvc
            .perform(
                get("/turno/mis-turnos").header("Authorization", tokenCliente)
            )
            .andExpect(status().isOk());
    }

    @Test
    void misTurnosSinTokenDevuelve403() throws Exception {
        mockMvc
            .perform(get("/turno/mis-turnos"))
            .andExpect(status().isForbidden());
    }

    @Test
    void clientePuedeVerProfesionalesRecientes() throws Exception {
        mockMvc
            .perform(
                get("/turno/profesionales-recientes").header(
                    "Authorization",
                    tokenCliente
                )
            )
            .andExpect(status().isOk());
    }

    @Test
    void profesionalPuedeModificarTurno() throws Exception {
        Turno turno = turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoDiaHabil(1),
                LocalTime.of(9, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
            )
        );

        mockMvc
            .perform(
                put("/turno/" + turno.getId() + "/modificar")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fecha": "%s",
                                "bloqueHorario": "10:00",
                                "clienteId": %d
                            }
                        """.formatted(
                            proximoDiaHabil(2).toString(),
                            cliente.getId()
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fecha").value(proximoDiaHabil(2).toString()))
            .andExpect(jsonPath("$.bloqueHorario").value("10:00"));
    }

    @Test
    void modificarTurnoInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                put("/turno/99999/modificar")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fecha": "%s",
                                "bloqueHorario": "10:00"
                            }
                        """.formatted(proximoDiaHabil(2).toString())
                    )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void clienteNoPuedeModificarTurno() throws Exception {
        Turno turno = turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoDiaHabil(1),
                LocalTime.of(9, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
            )
        );

        mockMvc
            .perform(
                put("/turno/" + turno.getId() + "/modificar")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fecha": "%s",
                                "bloqueHorario": "10:00"
                            }
                        """.formatted(proximoDiaHabil(2).toString())
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeCancelarEnLote() throws Exception {
        turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoDiaHabil(1),
                LocalTime.of(9, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
            )
        );
        turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoDiaHabil(1),
                LocalTime.of(10, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
            )
        );

        mockMvc
            .perform(
                post("/turno/cancelar-en-lote")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "agendaId": %d,
                                "fecha": "%s"
                            }
                        """.formatted(
                            agenda.getId(),
                            proximoDiaHabil(1).toString()
                        )
                    )
            )
            .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeCancelarEnLote() throws Exception {
        mockMvc
            .perform(
                post("/turno/cancelar-en-lote")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "agendaId": %d,
                                "fecha": "%s"
                            }
                        """.formatted(
                            agenda.getId(),
                            proximoDiaHabil(1).toString()
                        )
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void clientePuedeReprogramarTurnoEnEstadoReprogramar() throws Exception {
        Turno turno = turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoDiaHabil(1),
                LocalTime.of(9, 0),
                TurnoEstado.REPROGRAMAR
            )
        );

        mockMvc
            .perform(
                put("/turno/" + turno.getId() + "/reprogramar")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fecha": "%s",
                                "bloqueHorario": "10:00"
                            }
                        """.formatted(proximoDiaHabil(2).toString())
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andExpect(
                jsonPath("$.fecha").value(proximoDiaHabil(2).toString())
            );
    }

    @Test
    void reprogramarTurnoConEstadoConfirmadoDevuelve400() throws Exception {
        Turno turno = turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoDiaHabil(1),
                LocalTime.of(9, 0),
                TurnoEstado.CONFIRMADO
            )
        );

        mockMvc
            .perform(
                put("/turno/" + turno.getId() + "/reprogramar")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fecha": "%s",
                                "bloqueHorario": "10:00"
                            }
                        """.formatted(proximoDiaHabil(2).toString())
                    )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void clienteNoPuedeReservarConProfesionalSinServicios() throws Exception {
        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "agendaId": %d,
                                "fecha": "%s",
                                "bloqueHorario": "09:00"
                            }
                        """.formatted(
                            agenda.getId(),
                            proximoDiaHabil(1).toString()
                        )
                    )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void otroClienteNoPuedeReprogramarTurnoAjeno() throws Exception {
        User otroCliente = userRepository.save(
            new User(
                "otro@test.com",
                "encoded",
                UserRole.CLIENTE,
                "Otro",
                "Cliente",
                LocalDate.of(1995, 5, 5),
                null,
                null
            )
        );
        String tokenOtro =
            "Bearer " +
            jwtService.createToken(
                new JwtUserDetails(
                    otroCliente.getUsername(),
                    UserRole.CLIENTE,
                    true
                )
            );

        Turno turno = turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoDiaHabil(1),
                LocalTime.of(9, 0),
                TurnoEstado.REPROGRAMAR
            )
        );

        mockMvc
            .perform(
                put("/turno/" + turno.getId() + "/reprogramar")
                    .header("Authorization", tokenOtro)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "fecha": "%s",
                                "bloqueHorario": "10:00"
                            }
                        """.formatted(proximoDiaHabil(2).toString())
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeReservarConNombreCliente() throws Exception {
        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "agendaId": %d,
                                "fecha": "%s",
                                "bloqueHorario": "09:00",
                                "nombreCliente": "Juan Externo"
                            }
                        """.formatted(
                            agenda.getId(),
                            proximoDiaHabil(1).toString()
                        )
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombreCliente").value("Juan Externo"));
    }
}
