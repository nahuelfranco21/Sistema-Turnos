package ar.uba.fi.ingsoft1.product_example.agenda;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.uba.fi.ingsoft1.product_example.config.security.JwtService;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtUserDetails;
import ar.uba.fi.ingsoft1.product_example.servicio.Servicio;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioRepository;
import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoEstado;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AgendaTurnoIntegrationTest {

    private static final String FECHA = proximoDiaHabil().toString();

    private static LocalDate proximoDiaHabil() {
        LocalDate d = LocalDate.now().plusDays(1);
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
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

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
                "López",
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
                "María",
                "García",
                LocalDate.of(1995, 5, 20),
                null,
                null
            )
        );

        // Creamos la agenda del profesional con rangos de lunes a viernes 8-17
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

        servicioRepository.save(
            new Servicio(agenda, "Consulta general", 30, 1000)
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
    void accesoSinTokenAAgendaNoSePuedeVerAgendaDevuelve403() throws Exception {
        mockMvc
            .perform(get("/agenda/" + agenda.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void accesoSinTokenATurnoNoSePuedeVerDevuelve403() throws Exception {
        mockMvc.perform(get("/turno/1")).andExpect(status().isForbidden());
    }

    // ─── AGENDA ──────────────────────────────────────────────────────

    @Test
    void obtenerAgendaInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                get("/agenda/99999").header("Authorization", tokenProfesional)
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void profesionalOSuperAdminPuedeVerSuPropiaAgendaPorId() throws Exception {
        MvcResult resultado = mockMvc
            .perform(
                get("/agenda/" + agenda.getId()).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isOk())
            .andReturn();

        var detalle = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );
        var ag = (Map<String, Object>) detalle.get("agenda");
        var rangos = (List<Map<String, Object>>) ag.get("rangos");

        assertEquals(agenda.getId(), Long.valueOf(ag.get("id").toString()));
        assertEquals(
            profesional.getId(),
            Long.valueOf(ag.get("profesionalId").toString())
        );
        assertEquals(30, ag.get("bloqueMinutos"));

        assertEquals(5, rangos.size());
        assertTrue(rangos.stream().anyMatch(r -> "LUNES".equals(r.get("dia"))));

        assertEquals(0, ((List<?>) detalle.get("turnos")).size());
    }

    @Test
    void clienteNoPuedeModificarAgenda() throws Exception {
        Map<String, Object> body = Map.of(
            "bloqueMinutos",
            60,
            "mesesAnticipacion",
            1,
            "rangos",
            List.of(
                Map.of(
                    "dia",
                    "LUNES",
                    "horaInicio",
                    "10:00",
                    "horaFin",
                    "18:00"
                )
            )
        );

        mockMvc
            .perform(
                put("/agenda/" + agenda.getId())
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeActualizarSuAgenda() throws Exception {
        Map<String, Object> body = Map.of(
            "bloqueMinutos",
            60,
            "mesesAnticipacion",
            1,
            "rangos",
            List.of(
                Map.of(
                    "dia",
                    "LUNES",
                    "horaInicio",
                    "10:00",
                    "horaFin",
                    "18:00"
                ),
                Map.of(
                    "dia",
                    "MIERCOLES",
                    "horaInicio",
                    "10:00",
                    "horaFin",
                    "18:00"
                ),
                Map.of(
                    "dia",
                    "VIERNES",
                    "horaInicio",
                    "10:00",
                    "horaFin",
                    "18:00"
                )
            )
        );

        MvcResult resultado = mockMvc
            .perform(
                put("/agenda/" + agenda.getId())
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isOk())
            .andReturn();

        var ag = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );
        assertEquals(60, ag.get("bloqueMinutos"));
        assertEquals(3, ((List<?>) ag.get("rangos")).size());
    }

    @Test
    void profesionalPuedeCrearTurnoConCliente() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult resultado = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        var turno = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );

        assertNotNull(turno.get("id"));
        assertEquals(
            agenda.getId(),
            Long.valueOf(turno.get("agendaId").toString())
        );
        assertEquals(
            cliente.getId(),
            Long.valueOf(turno.get("clienteId").toString())
        );
        assertEquals("OCUPADO_SIN_CONFIRMAR", turno.get("estado"));
    }

    @Test
    void profesionalPuedeCrearUnBloqueDeshabilitadoParaNoAtenderClientes()
        throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00"
        );

        MvcResult resultado = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        var turno = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );

        assertNull(turno.get("clienteId"));
        assertEquals("DESHABILITADO", turno.get("estado"));
    }

    @Test
    void clientePuedeReservarSuPropioTurnoSinMandarSuIdElSistemaLoeDetectaPorToken()
        throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00"
        );

        MvcResult resultado = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        var turno = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );

        assertEquals(
            cliente.getId(),
            Long.valueOf(turno.get("clienteId").toString())
        );
        assertEquals("OCUPADO_SIN_CONFIRMAR", turno.get("estado"));
    }

    @Test
    void unClienteUsaIdDeOtroCLienteElSistemaLoIgnora() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            99999L
        ); // ID inventado, no existe

        MvcResult resultado = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        var turno = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );

        assertEquals(
            cliente.getId(),
            Long.valueOf(turno.get("clienteId").toString())
        );
    }

    @Test
    void crearTurnoDuplicadoEnMismoBloquesDevuelve409() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated());

        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isConflict());
    }

    @Test
    void crearTurnoEnAgendaInexistenteDevuelve404() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            99999L,
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00"
        );

        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void obtenerTurnoInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                get("/turno/99999").header("Authorization", tokenProfesional)
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void profesionalPuedeVerTurnoDeSuAgenda() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        MvcResult getResult = mockMvc
            .perform(
                get("/turno/" + turnoId).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isOk())
            .andReturn();

        var turno = objectMapper.readValue(
            getResult.getResponse().getContentAsString(),
            Map.class
        );

        assertEquals(turnoId, Long.valueOf(turno.get("id").toString()));
        assertEquals(
            agenda.getId(),
            Long.valueOf(turno.get("agendaId").toString())
        );
        assertEquals(
            cliente.getId(),
            Long.valueOf(turno.get("clienteId").toString())
        );
        assertEquals(FECHA, turno.get("fecha"));
        assertEquals("09:00", turno.get("bloqueHorario"));
    }

    @Test
    void otroProfesionalNoPuedeVerTurnoAjeno() throws Exception {
        User otroProf = userRepository.save(
            new User(
                "otro@test.com",
                "encoded",
                UserRole.PROFESIONAL,
                "Lucas",
                "Martínez",
                LocalDate.of(1988, 3, 10),
                "Profesor",
                SectorTrabajo.EDUCACION
            )
        );
        String tokenOtroProf =
            "Bearer " +
            jwtService.createToken(
                new JwtUserDetails(
                    otroProf.getUsername(),
                    UserRole.PROFESIONAL,
                    true
                )
            );

        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        mockMvc
            .perform(
                get("/turno/" + turnoId).header("Authorization", tokenOtroProf)
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void clientePuedeVerSuPropioTurno() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        mockMvc
            .perform(
                get("/turno/" + turnoId).header("Authorization", tokenCliente)
            )
            .andExpect(status().isOk());
    }

    @Test
    void clientePuedeConfirmarSuPropioTurno() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        MvcResult confirmResult = mockMvc
            .perform(
                put("/turno/" + turnoId)
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("estado", "CONFIRMADO")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andReturn();

        var turnoConfirmado = objectMapper.readValue(
            confirmResult.getResponse().getContentAsString(),
            Map.class
        );

        assertEquals("CONFIRMADO", turnoConfirmado.get("estado"));
    }

    @Test
    void clienteNoPuedeCambiarEstadoADeshabilitadoSoloProfesionalPuede()
        throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        mockMvc
            .perform(
                put("/turno/" + turnoId)
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("estado", "DESHABILITADO")
                        )
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void profesionalPuedeCambiarCualquierEstado() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        for (String estado : List.of(
            "CONFIRMADO",
            "DESHABILITADO",
            "OCUPADO_SIN_CONFIRMAR"
        )) {
            MvcResult result = mockMvc
                .perform(
                    put("/turno/" + turnoId)
                        .header("Authorization", tokenProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of("estado", estado)
                            )
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

            var turnoActualizado = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Map.class
            );

            assertEquals(estado, turnoActualizado.get("estado"));
        }
    }

    @Test
    void actualizarTurnoInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                put("/turno/99999")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("estado", "CONFIRMADO")
                        )
                    )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void profesionalPuedeEliminarTurno() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        mockMvc
            .perform(
                delete("/turno/" + turnoId).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isNoContent());

        mockMvc
            .perform(
                get("/turno/" + turnoId).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void clientePuedeEliminarSuPropiTurnoSiNoPuedeIr() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        mockMvc
            .perform(
                delete("/turno/" + turnoId).header(
                    "Authorization",
                    tokenCliente
                )
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void eliminarTurnoInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                delete("/turno/99999").header("Authorization", tokenProfesional)
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void clientePuedeCancelarSuPropioTurno() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        MvcResult createResult = mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Long turnoId = Long.valueOf(
            objectMapper
                .readValue(
                    createResult.getResponse().getContentAsString(),
                    Map.class
                )
                .get("id")
                .toString()
        );

        MvcResult cancelResult = mockMvc
            .perform(
                put("/turno/" + turnoId)
                    .header("Authorization", tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("estado", "CANCELADO")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andReturn();

        var turnoCancelado = objectMapper.readValue(
            cancelResult.getResponse().getContentAsString(),
            Map.class
        );

        assertEquals("CANCELADO", turnoCancelado.get("estado"));
    }

    @Test
    void crearTurnoEnFechaPasadaDevuelve400() throws Exception {
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            "2020-01-01",
            "bloqueHorario",
            "09:00",
            "clienteId",
            cliente.getId()
        );

        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void crearTurnoEnHorarioFueraDeRangoDevuelve400() throws Exception {
        // La agenda tiene rango LUNES 08:00-17:00. FECHA se elige como próximo día hábil.
        // 07:00 está antes del inicio del rango → slot inválido.
        Map<String, Object> body = Map.of(
            "agendaId",
            agenda.getId(),
            "fecha",
            FECHA,
            "bloqueHorario",
            "07:00",
            "clienteId",
            cliente.getId()
        );

        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void turnosCreadosAparecenEnLaAgendaCorrectamente() throws Exception {
        MvcResult antes = mockMvc
            .perform(
                get("/agenda/" + agenda.getId()).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isOk())
            .andReturn();
        var detailAntes = objectMapper.readValue(
            antes.getResponse().getContentAsString(),
            Map.class
        );
        assertEquals(0, ((List<?>) detailAntes.get("turnos")).size());

        mockMvc
            .perform(
                post("/turno")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "agendaId",
                                agenda.getId(),
                                "fecha",
                                FECHA,
                                "bloqueHorario",
                                "09:00",
                                "clienteId",
                                cliente.getId()
                            )
                        )
                    )
            )
            .andExpect(status().isCreated());

        MvcResult despues = mockMvc
            .perform(
                get("/agenda/" + agenda.getId()).header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isOk())
            .andReturn();
        var detailDespues = objectMapper.readValue(
            despues.getResponse().getContentAsString(),
            Map.class
        );

        assertEquals(1, ((List<?>) detailDespues.get("turnos")).size());
    }

    @Test
    void profesionalPuedeVerSuAgendaPropiaSinNecesitarID() throws Exception {
        MvcResult resultado = mockMvc
            .perform(
                get("/agenda/mi-agenda").header(
                    "Authorization",
                    tokenProfesional
                )
            )
            .andExpect(status().isOk())
            .andReturn();

        var detalle = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );
        var ag = (Map<String, Object>) detalle.get("agenda");

        assertEquals(
            profesional.getId(),
            Long.valueOf(ag.get("profesionalId").toString())
        );
    }

    @Test
    void clienteNoPuedeAccederAlEndopointMiAgendaSoloParaProfesionales()
        throws Exception {
        mockMvc
            .perform(
                get("/agenda/mi-agenda").header("Authorization", tokenCliente)
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenNoPuedeAccederAMiAgenda() throws Exception {
        mockMvc
            .perform(get("/agenda/mi-agenda"))
            .andExpect(status().isForbidden());
    }

    @Test
    void clientePuedeVerAgendaPublicaDeUnProfesional() throws Exception {
        MvcResult resultado = mockMvc
            .perform(
                get("/agenda/profesional/" + profesional.getId()).header(
                    "Authorization",
                    tokenCliente
                )
            )
            .andExpect(status().isOk())
            .andReturn();

        var detalle = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );

        assertNotNull(detalle.get("agenda"));
        assertNotNull(detalle.get("turnos"));
    }

    @Test
    void agendaPublicaDeUnProfesionalInexistenteDevuelve404() throws Exception {
        mockMvc
            .perform(
                get("/agenda/profesional/99999").header(
                    "Authorization",
                    tokenCliente
                )
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void actualizarAgendaSinConflictosConElNuevoHorarioActualizaCorrectamente()
        throws Exception {
        Map<String, Object> body = Map.of(
            "bloqueMinutos",
            60,
            "mesesAnticipacion",
            1,
            "rangos",
            List.of(
                Map.of(
                    "dia",
                    "LUNES",
                    "horaInicio",
                    "09:00",
                    "horaFin",
                    "17:00"
                )
            )
        );

        MvcResult resultado = mockMvc
            .perform(
                put("/agenda/" + agenda.getId())
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isOk())
            .andReturn();

        var ag = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );

        assertEquals(60, ag.get("bloqueMinutos"));
    }

    @Test
    void actualizarAgendaConTurnosConflictivosDevuelve409() throws Exception {
        LocalDate proximoLunes = LocalDate.now().with(
            java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY)
        );

        turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoLunes,
                LocalTime.of(9, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
            )
        );

        Map<String, Object> body = Map.of(
            "bloqueMinutos",
            30,
            "mesesAnticipacion",
            1,
            "rangos",
            List.of(
                Map.of(
                    "dia",
                    "LUNES",
                    "horaInicio",
                    "10:00",
                    "horaFin",
                    "17:00"
                )
            )
        );

        MvcResult resultado = mockMvc
            .perform(
                put("/agenda/" + agenda.getId())
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isConflict())
            .andReturn();

        var respuesta = objectMapper.readValue(
            resultado.getResponse().getContentAsString(),
            Map.class
        );

        var conflictos = (List<?>) respuesta.get("turnosConflictivos");
        assertEquals(1, conflictos.size());
    }

    @Test
    void actualizarAgendaConConflictosYAccionCancelarCancelaTurnos()
        throws Exception {
        LocalDate proximoLunes = LocalDate.now().with(
            java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY)
        );

        Turno turnoConflictivo = turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoLunes,
                LocalTime.of(9, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
            )
        );

        Map<String, Object> body = Map.of(
            "bloqueMinutos",
            30,
            "mesesAnticipacion",
            1,
            "rangos",
            List.of(
                Map.of(
                    "dia",
                    "LUNES",
                    "horaInicio",
                    "10:00",
                    "horaFin",
                    "17:00"
                )
            )
        );

        mockMvc
            .perform(
                put("/agenda/" + agenda.getId())
                    .param("conflictos", "cancelar")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isOk());

        Turno turnoActualizado = turnoRepository
            .findById(turnoConflictivo.getId())
            .orElseThrow();
        assertEquals(TurnoEstado.CANCELADO, turnoActualizado.getEstado());
    }

    @Test
    void actualizarAgendaConConflictosYAccionMantenerNoTocaLosTurnos()
        throws Exception {
        LocalDate proximoLunes = LocalDate.now().with(
            java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY)
        );

        Turno turnoExistente = turnoRepository.save(
            new Turno(
                agenda,
                cliente,
                proximoLunes,
                LocalTime.of(9, 0),
                TurnoEstado.OCUPADO_SIN_CONFIRMAR
            )
        );

        Map<String, Object> body = Map.of(
            "bloqueMinutos",
            30,
            "mesesAnticipacion",
            1,
            "rangos",
            List.of(
                Map.of(
                    "dia",
                    "LUNES",
                    "horaInicio",
                    "10:00",
                    "horaFin",
                    "17:00"
                )
            )
        );

        mockMvc
            .perform(
                put("/agenda/" + agenda.getId())
                    .param("conflictos", "mantener")
                    .header("Authorization", tokenProfesional)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            )
            .andExpect(status().isOk());

        Turno turnoSinCambios = turnoRepository
            .findById(turnoExistente.getId())
            .orElseThrow();
        assertEquals(
            TurnoEstado.OCUPADO_SIN_CONFIRMAR,
            turnoSinCambios.getEstado()
        );
    }
}
