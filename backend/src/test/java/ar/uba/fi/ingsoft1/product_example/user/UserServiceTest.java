package ar.uba.fi.ingsoft1.product_example.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import ar.uba.fi.ingsoft1.product_example.agenda.AgendaService;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtService;
import ar.uba.fi.ingsoft1.product_example.email.EmailService;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioRepository;
import ar.uba.fi.ingsoft1.product_example.user.email_verification.EmailVerificationTokenService;
import ar.uba.fi.ingsoft1.product_example.user.password_recovery.PasswordRecoveryToken;
import ar.uba.fi.ingsoft1.product_example.user.password_recovery.PasswordRecoveryTokenRepository;
import ar.uba.fi.ingsoft1.product_example.user.password_recovery.PasswordRecoveryTokenService;
import ar.uba.fi.ingsoft1.product_example.user.refresh_token.RefreshToken;
import ar.uba.fi.ingsoft1.product_example.user.refresh_token.RefreshTokenRepository;
import ar.uba.fi.ingsoft1.product_example.user.refresh_token.RefreshTokenService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;
    private static final String USERNAME = "user@test.com";
    private static final String PASSWORD = "password";
    private static final String NOMBRE = "Test";
    private static final String APELLIDO = "User";
    private static final LocalDate FECHA_NAC = LocalDate.of(2000, 1, 1);

    private final BCryptPasswordEncoder passwordEncoder =
        new BCryptPasswordEncoder();

    private UserService construirServiceConRefreshToken(
        RefreshTokenRepository refreshTokenRepo
    ) {
        var key = "0".repeat(64);
        return new UserService(
            new JwtService(key, 1L),
            new BCryptPasswordEncoder(),
            userRepository,
            new RefreshTokenService(1L, 20, refreshTokenRepo),
            new PasswordRecoveryTokenService(mock()),
            mock(EmailVerificationTokenService.class),
            mock(AgendaService.class),
            mock(EmailService.class),
            mock(ServicioRepository.class)
        );
    }

    private RefreshToken crearRefreshToken(String valor, boolean valido) {
        var user = new User(
            USERNAME,
            "hash",
            UserRole.CLIENTE,
            NOMBRE,
            APELLIDO,
            FECHA_NAC,
            null,
            null
        );
        var expiracion = valido
            ? Instant.now().plus(99999, ChronoUnit.MILLIS)
            : Instant.now().minus(1, ChronoUnit.MILLIS);
        return new RefreshToken(valor, user, expiracion);
    }

    private UserService construirServiceConPasswordRecoverToken(
        PasswordRecoveryTokenRepository recoveryRepo
    ) {
        var key = "0".repeat(64);
        return new UserService(
            new JwtService(key, 1L),
            new BCryptPasswordEncoder(),
            userRepository,
            new RefreshTokenService(1L, 20, mock()),
            new PasswordRecoveryTokenService(recoveryRepo),
            mock(EmailVerificationTokenService.class),
            mock(AgendaService.class),
            mock(EmailService.class),
            mock(ServicioRepository.class)
        );
    }

    @BeforeEach
    void setup() {
        var passwordHash = passwordEncoder.encode(PASSWORD);

        userRepository = mock(UserRepository.class);

        when(userRepository.findByUsername(anyString())).thenReturn(
            Optional.empty()
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(
            Optional.of(
                new User(
                    USERNAME,
                    passwordHash,
                    UserRole.CLIENTE,
                    NOMBRE,
                    APELLIDO,
                    FECHA_NAC,
                    null,
                    null
                )
            )
        );

        var key = "0".repeat(64);
        userService = new UserService(
            new JwtService(key, 1L),
            passwordEncoder,
            userRepository,
            new RefreshTokenService(1L, 20, mock()),
            new PasswordRecoveryTokenService(mock()),
            mock(EmailVerificationTokenService.class),
            mock(AgendaService.class),
            mock(EmailService.class),
            mock(ServicioRepository.class)
        );
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @Test
    void iniciarSesionConCredencialesCorrectas() {
        var response = userService.loginUser(
            new UserLoginDTO(USERNAME, PASSWORD)
        );
        assertNotNull(response.orElseThrow());
    }

    @Test
    void iniciarSesionConPasswordIncorrecta() {
        var response = userService.loginUser(
            new UserLoginDTO(USERNAME, PASSWORD + "_wrong")
        );
        assertEquals(Optional.empty(), response);
    }

    @Test
    void iniciarSesionConUsuarioInexistente() {
        var response = userService.loginUser(
            new UserLoginDTO(USERNAME + "_wrong", PASSWORD)
        );
        assertEquals(Optional.empty(), response);
    }

    @Test
    void iniciarSesionDevuelveRolesCorrectosEnElToken() {
        var response = userService.loginUser(
            new UserLoginDTO(USERNAME, PASSWORD)
        );
        var roles = response.orElseThrow().roles();

        assertTrue(roles.contains(UserRole.CLIENTE));
        assertFalse(roles.contains(UserRole.PROFESIONAL));
        assertFalse(roles.contains(UserRole.SUPER_ADMIN));
    }

    // ─── REGISTRO ─────────────────────────────────────────────────────────────

    @Test
    void registrarNuevoClienteDevuelveTokens() {
        var dto = new UserCreateDTO(
            "nuevo@test.com",
            PASSWORD,
            NOMBRE,
            APELLIDO,
            FECHA_NAC,
            false,
            null,
            null,
            null
        );

        var result = userService.createUser(dto);

        assertTrue(
            result.isPresent(),
            "Debería devolver tokens al registrar usuario nuevo"
        );
    }

    @Test
    void registrarNuevoProfesionalDevuelveTokensConRolesCorrectos() {
        var dto = new UserCreateDTO(
            "prof@test.com",
            PASSWORD,
            NOMBRE,
            APELLIDO,
            FECHA_NAC,
            true,
            "Psicólogo",
            SectorTrabajo.SALUD_MENTAL,
            null
        );

        var result = userService.createUser(dto);
        var roles = result.get().roles();

        assertTrue(roles.contains(UserRole.PROFESIONAL));
        assertTrue(roles.contains(UserRole.CLIENTE));
        assertFalse(roles.contains(UserRole.SUPER_ADMIN));
    }

    @Test
    void registrarEmailExistenteDevuelveVacio() {
        var dto = new UserCreateDTO(
            USERNAME,
            PASSWORD,
            NOMBRE,
            APELLIDO,
            FECHA_NAC,
            false,
            null,
            null,
            null
        );

        var result = userService.createUser(dto);

        assertEquals(Optional.empty(), result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registrarNuevoUsuarioLlamaASave() {
        var dto = new UserCreateDTO(
            "nuevo@test.com",
            PASSWORD,
            NOMBRE,
            APELLIDO,
            FECHA_NAC,
            false,
            null,
            null,
            null
        );

        userService.createUser(dto);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void clienteSoloTieneRolCliente() {
        var roles = UserRole.CLIENTE.getImpliedRoles();

        assertTrue(roles.contains(UserRole.CLIENTE));
        assertFalse(roles.contains(UserRole.PROFESIONAL));
        assertFalse(roles.contains(UserRole.SUPER_ADMIN));
    }

    @Test
    void profesionalTieneRolesProfesionalYCliente() {
        var roles = UserRole.PROFESIONAL.getImpliedRoles();

        assertTrue(roles.contains(UserRole.PROFESIONAL));
        assertTrue(roles.contains(UserRole.CLIENTE));
        assertFalse(roles.contains(UserRole.SUPER_ADMIN));
    }

    @Test
    void superAdminTieneTodosLosRoles() {
        var roles = UserRole.SUPER_ADMIN.getImpliedRoles();

        assertTrue(roles.contains(UserRole.SUPER_ADMIN));
        assertTrue(roles.contains(UserRole.PROFESIONAL));
        assertTrue(roles.contains(UserRole.CLIENTE));
    }

    @Test
    void refreshTokenValidoDevuelveNuevosTokens() {
        var refreshTokenRepo = mock(RefreshTokenRepository.class);
        when(refreshTokenRepo.findById("token-valido")).thenReturn(
            Optional.of(crearRefreshToken("token-valido", true))
        );

        var result = construirServiceConRefreshToken(refreshTokenRepo).refresh(
            new RefreshDTO("token-valido")
        );

        assertTrue(result.isPresent());
    }

    @Test
    void refreshTokenVencidoDevuelveVacio() {
        var refreshTokenRepo = mock(RefreshTokenRepository.class);
        when(refreshTokenRepo.findById("token-vencido")).thenReturn(
            Optional.of(crearRefreshToken("token-vencido", false))
        );

        var result = construirServiceConRefreshToken(refreshTokenRepo).refresh(
            new RefreshDTO("token-vencido")
        );

        assertEquals(Optional.empty(), result);
    }

    @Test
    void refresshTokenInexistenteDevuelveVacio() {
        var refreshTokenRepo = mock(RefreshTokenRepository.class);
        when(refreshTokenRepo.findById(anyString())).thenReturn(
            Optional.empty()
        );

        var result = construirServiceConRefreshToken(refreshTokenRepo).refresh(
            new RefreshDTO("token-inexistente")
        );

        assertEquals(Optional.empty(), result);
    }

    @Test
    void refreshTokenValidoLoBorraDelRepositorio() {
        var token = crearRefreshToken("token-valido", true);
        var refreshTokenRepo = mock(RefreshTokenRepository.class);
        when(refreshTokenRepo.findById("token-valido")).thenReturn(
            Optional.of(token)
        );

        construirServiceConRefreshToken(refreshTokenRepo).refresh(
            new RefreshDTO("token-valido")
        );

        verify(refreshTokenRepo, times(1)).delete(token);
    }

    @Test
    void cargarUsuarioPorUsernameExistenteDevuelveUserNameCorrecto() {
        var result = userService.loadUserByUsername(USERNAME);

        assertEquals(USERNAME, result.getUsername());
    }

    @Test
    void cargarUsuarioPorUsernameInexistenteLanzaExcepcion() {
        assertThrows(UsernameNotFoundException.class, () ->
            userService.loadUserByUsername("noexiste@test.com")
        );
    }

    @Test
    void iniciarRecuperacionConEmailExistenteGuardaToken() {
        var recoveryRepo = mock(PasswordRecoveryTokenRepository.class);
        when(recoveryRepo.save(any())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        construirServiceConPasswordRecoverToken(
            recoveryRepo
        ).startPasswordRecovery(new PasswordRecoveryDTO(USERNAME), null);

        verify(recoveryRepo, times(1)).save(any());
    }

    @Test
    void iniciarRecuperacionConEmailInexistenteNoHaceNada() {
        var recoveryRepo = mock(PasswordRecoveryTokenRepository.class);

        construirServiceConPasswordRecoverToken(
            recoveryRepo
        ).startPasswordRecovery(
            new PasswordRecoveryDTO("noexiste@test.com"),
            null
        );

        verify(recoveryRepo, never()).save(any());
    }

    @Test
    void usuarioPuedeCambiarSuContrasniaSiIngresaSuContraseniaActualYSuCorreoCorrectamente() {
        var resultado = userService.updatePassword(
            USERNAME,
            PASSWORD,
            "nuevaPassword123"
        );

        assertTrue(resultado);
    }

    @Test
    void unUsuarioNoPuedeCambiarSuContraseniaSiEscribeMalSuContraseniaActual() {
        var resultado = userService.updatePassword(
            USERNAME,
            "passwordIncorrecta",
            "nuevaPassword123"
        );

        assertFalse(resultado);
    }

    @Test
    void siElUsuarioQuiereCambiarSuContraseniaConCorreoInvalidoLanzaExcepcion() {
        assertThrows(UsernameNotFoundException.class, () ->
            userService.updatePassword(
                "noexiste@test.com",
                PASSWORD,
                "nuevaPassword123"
            )
        );
    }

    @Test
    void unaContraseniaNuevadQuedaActualizadaEnElRepositorio() {
        userService.updatePassword(USERNAME, PASSWORD, "nuevaPassword123");

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void siLaContraseniaActualEsIncorrectaLaNuevaContraseniaNoSeGuardaEnElRespositorio() {
        userService.updatePassword(
            USERNAME,
            "passwordIncorrecta",
            "nuevaPassword123"
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmailConMismoEmailDevuelveTrue() {
        var resultado = userService.updateEmail(USERNAME, USERNAME, null);
        assertTrue(resultado);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmailConEmailNuevoValidoActualiza() {
        var resultado = userService.updateEmail(
            USERNAME,
            "nuevo@test.com",
            null
        );
        assertTrue(resultado);
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void updateEmailConEmailExistenteDevuelveFalse() {
        when(userRepository.findByUsername("existente@test.com")).thenReturn(
            Optional.of(
                new User(
                    "existente@test.com",
                    "hash",
                    UserRole.CLIENTE,
                    "Otro",
                    "User",
                    LocalDate.of(2000, 1, 1),
                    null,
                    null
                )
            )
        );

        var resultado = userService.updateEmail(
            USERNAME,
            "existente@test.com",
            null
        );
        assertFalse(resultado);
    }

    @Test
    void updateEmailConUsuarioInexistenteLanzaExcepcion() {
        assertThrows(UsernameNotFoundException.class, () ->
            userService.updateEmail("noexiste@test.com", "nuevo@test.com", null)
        );
    }

    @Test
    void updateFotoPerfilGuardaFoto() {
        userService.updateFotoPerfil(USERNAME, "data:image/png;base64,abc123");
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void updateFotoPerfilConUsuarioInexistenteLanzaExcepcion() {
        assertThrows(UsernameNotFoundException.class, () ->
            userService.updateFotoPerfil("noexiste@test.com", "foto")
        );
    }

    @Test
    void updateProfesionActualizaProfesion() {
        when(userRepository.findByUsername("prof@test.com")).thenReturn(
            Optional.of(
                new User(
                    "prof@test.com",
                    "hash",
                    UserRole.PROFESIONAL,
                    NOMBRE,
                    APELLIDO,
                    FECHA_NAC,
                    null,
                    null
                )
            )
        );

        userService.updateProfesion(
            "prof@test.com",
            "Psicólogo",
            "SALUD_MENTAL",
            null
        );
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void updateProfesionClienteLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
            userService.updateProfesion(
                USERNAME,
                "Psicólogo",
                "SALUD_MENTAL",
                null
            )
        );
    }

    @Test
    void updateProfesionConSectorInvalidoLanzaExcepcion() {
        when(userRepository.findByUsername("prof@test.com")).thenReturn(
            Optional.of(
                new User(
                    "prof@test.com",
                    "hash",
                    UserRole.PROFESIONAL,
                    NOMBRE,
                    APELLIDO,
                    FECHA_NAC,
                    null,
                    null
                )
            )
        );

        assertThrows(IllegalArgumentException.class, () ->
            userService.updateProfesion(
                "prof@test.com",
                "Psicólogo",
                "SECTOR_INVALIDO",
                null
            )
        );
    }

    @Test
    void updateProfesionConUsuarioInexistenteLanzaExcepcion() {
        assertThrows(UsernameNotFoundException.class, () ->
            userService.updateProfesion(
                "noexiste@test.com",
                "Psicólogo",
                "SALUD_MENTAL",
                null
            )
        );
    }

    @Test
    void updateDescripcionActualizaDescripcion() {
        when(userRepository.findByUsername("prof@test.com")).thenReturn(
            Optional.of(
                new User(
                    "prof@test.com",
                    "hash",
                    UserRole.PROFESIONAL,
                    NOMBRE,
                    APELLIDO,
                    FECHA_NAC,
                    null,
                    null
                )
            )
        );

        userService.updateDescripcion(
            "prof@test.com",
            "Especialista en kinesiología deportiva."
        );
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void updateDescripcionConUsuarioInexistenteLanzaExcepcion() {
        assertThrows(UsernameNotFoundException.class, () ->
            userService.updateDescripcion("noexiste@test.com", "descripcion")
        );
    }

    @Test
    void resetPasswordConTokenValidoActualizaPassword() {
        var recoveryRepo = mock(PasswordRecoveryTokenRepository.class);
        var user = new User(
            USERNAME,
            passwordEncoder.encode(PASSWORD),
            UserRole.CLIENTE,
            NOMBRE,
            APELLIDO,
            FECHA_NAC,
            null,
            null
        );
        var token = new PasswordRecoveryToken(user);
        when(recoveryRepo.findByToken("token-valido")).thenReturn(
            Optional.of(token)
        );
        when(recoveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        construirServiceConPasswordRecoverToken(recoveryRepo).resetPassword(
            "token-valido",
            "nuevaPassword123"
        );
        verify(recoveryRepo, times(1)).save(any());
    }

    @Test
    void resetPasswordConTokenInvalidoLanzaExcepcion() {
        var recoveryRepo = mock(PasswordRecoveryTokenRepository.class);
        when(recoveryRepo.findByToken("token-invalido")).thenReturn(
            Optional.empty()
        );

        assertThrows(ResponseStatusException.class, () ->
            construirServiceConPasswordRecoverToken(recoveryRepo).resetPassword(
                "token-invalido",
                "nuevaPassword123"
            )
        );
    }

    @Test
    void resetPasswordConMismaPasswordLanzaExcepcion() {
        var recoveryRepo = mock(PasswordRecoveryTokenRepository.class);
        var user = new User(
            USERNAME,
            passwordEncoder.encode(PASSWORD),
            UserRole.CLIENTE,
            NOMBRE,
            APELLIDO,
            FECHA_NAC,
            null,
            null
        );
        var token = new PasswordRecoveryToken(user);
        when(recoveryRepo.findByToken("token-valido")).thenReturn(
            Optional.of(token)
        );

        assertThrows(ResponseStatusException.class, () ->
            construirServiceConPasswordRecoverToken(recoveryRepo).resetPassword(
                "token-valido",
                PASSWORD
            )
        );
    }
}
