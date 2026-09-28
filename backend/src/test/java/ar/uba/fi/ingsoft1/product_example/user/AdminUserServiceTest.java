package ar.uba.fi.ingsoft1.product_example.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ar.uba.fi.ingsoft1.product_example.agenda.AgendaService;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoService;
import ar.uba.fi.ingsoft1.product_example.user.email_verification.EmailVerificationTokenService;
import ar.uba.fi.ingsoft1.product_example.user.password_recovery.PasswordRecoveryTokenService;
import ar.uba.fi.ingsoft1.product_example.user.refresh_token.RefreshTokenService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AdminUserServiceTest {

    private AdminUserService adminUserService;
    private UserRepository userRepository;
    private AgendaService agendaService;
    private TurnoService turnoService;

    private static final String USERNAME = "user@test.com";
    private static final String NOMBRE = "Test";
    private static final String APELLIDO = "User";
    private static final LocalDate FECHA_NAC = LocalDate.of(2000, 1, 1);

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        agendaService = mock(AgendaService.class);
        turnoService = mock(TurnoService.class);
        var passwordRecoveryService = mock(PasswordRecoveryTokenService.class);
        var emailVerificationService = mock(EmailVerificationTokenService.class);
        var refreshTokenService = mock(RefreshTokenService.class);

        adminUserService = new AdminUserService(userRepository, agendaService, turnoService,
            passwordRecoveryService, emailVerificationService, refreshTokenService);
    }

    @Test
    void getUserAdminDetailDevuelveDetalle() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
            new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null)
        ));

        var detalle = adminUserService.getUserAdminDetail(1L);
        assertEquals(USERNAME, detalle.email());
    }

    @Test
    void getUserAdminDetailConIdInexistenteLanzaExcepcion() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> adminUserService.getUserAdminDetail(999L));
    }

    @Test
    void adminUpdateUserActualizaDatos() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.adminUpdateUser(1L, new AdminUpdateUserDTO("Nuevo", "Apellido", "nuevo@test.com"));
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void adminUpdateUserConEmailDuplicadoLanzaConflicto() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("otro@test.com")).thenReturn(Optional.of(
            new User("otro@test.com", "hash", UserRole.CLIENTE, "Otro", "User", FECHA_NAC, null, null)
        ));

        assertThrows(ResponseStatusException.class, () ->
            adminUserService.adminUpdateUser(1L, new AdminUpdateUserDTO("Nuevo", "Apellido", "otro@test.com"))
        );
    }

    @Test
    void deleteUserEliminaUsuarioExistente() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.deleteUser(1L);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void deleteUserConIdInexistenteLanzaExcepcion() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> adminUserService.deleteUser(999L));
    }

    @Test
    void promoteToProfesionalCambiaRolYAgregaAgenda() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.promoteToProfesional(1L, new AdminPromoteDTO("Psicólogo", "SALUD_MENTAL", null));
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void promoteToProfesionalConRolIncorrectoLanzaExcepcion() {
        User user = new User(USERNAME, "hash", UserRole.PROFESIONAL, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(ResponseStatusException.class, () ->
            adminUserService.promoteToProfesional(1L, new AdminPromoteDTO("Psicólogo", "SALUD_MENTAL", null))
        );
    }

    @Test
    void promoteToProfesionalConSectorInvalidoLanzaExcepcion() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(ResponseStatusException.class, () ->
            adminUserService.promoteToProfesional(1L, new AdminPromoteDTO("Psicólogo", "SECTOR_INVALIDO", null))
        );
    }

    @Test
    void adminSetVerifiedActualizaVerificado() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.adminSetVerified(1L, true);
        assertTrue(user.isVerified());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void deleteUserConTurnosPendientesComoClienteLanzaConflicto() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(turnoService.hasPendingFutureTurnosByClienteId(1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> adminUserService.deleteUser(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUserConTurnosPendientesComoProfesionalLanzaConflicto() {
        User user = new User(USERNAME, "hash", UserRole.PROFESIONAL, NOMBRE, APELLIDO, FECHA_NAC, "Médico", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(turnoService.hasPendingFutureTurnosByClienteId(1L)).thenReturn(false);
        when(turnoService.hasPendingFutureTurnosByProfesionalId(1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> adminUserService.deleteUser(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void setUserActiveDesactivaUsuarioSinTurnosPendientes() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(turnoService.hasPendingFutureTurnosByClienteId(1L)).thenReturn(false);
        when(turnoService.hasPendingFutureTurnosByProfesionalId(1L)).thenReturn(false);

        adminUserService.setUserActive(1L, false);
        assertFalse(user.isActive());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void setUserActiveConTurnosPendientesLanzaConflicto() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(turnoService.hasPendingFutureTurnosByClienteId(1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> adminUserService.setUserActive(1L, false));
        verify(userRepository, never()).save(any());
    }

    @Test
    void setUserActiveActivaUsuarioSinVerificarTurnos() {
        User user = new User(USERNAME, "hash", UserRole.CLIENTE, NOMBRE, APELLIDO, FECHA_NAC, null, null);
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.setUserActive(1L, true);
        assertTrue(user.isActive());
        verify(turnoService, never()).hasPendingFutureTurnosByClienteId(any());
        verify(userRepository, times(1)).save(any());
    }
}
