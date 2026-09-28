package ar.uba.fi.ingsoft1.product_example.user;

import ar.uba.fi.ingsoft1.product_example.agenda.AgendaService;
import ar.uba.fi.ingsoft1.product_example.common.SecurityUtils;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtService;
import ar.uba.fi.ingsoft1.product_example.config.security.JwtUserDetails;
import ar.uba.fi.ingsoft1.product_example.email.EmailService;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioPublicoDTO;
import ar.uba.fi.ingsoft1.product_example.servicio.ServicioRepository;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoService;
import ar.uba.fi.ingsoft1.product_example.user.email_verification.EmailVerificationToken;
import ar.uba.fi.ingsoft1.product_example.user.email_verification.EmailVerificationTokenService;
import ar.uba.fi.ingsoft1.product_example.user.password_recovery.PasswordRecoveryToken;
import ar.uba.fi.ingsoft1.product_example.user.password_recovery.PasswordRecoveryTokenService;
import ar.uba.fi.ingsoft1.product_example.user.refresh_token.RefreshToken;
import ar.uba.fi.ingsoft1.product_example.user.refresh_token.RefreshTokenService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final AgendaService agendaService;
    private final EmailService emailService;
    private final ServicioRepository servicioRepository;

    @Autowired
    UserService(
        JwtService jwtService,
        PasswordEncoder passwordEncoder,
        UserRepository userRepository,
        RefreshTokenService refreshTokenService,
        PasswordRecoveryTokenService passwordRecoveryTokenService,
        EmailVerificationTokenService emailVerificationTokenService,
        AgendaService agendaService,
        EmailService emailService,
        ServicioRepository servicioRepository
    ) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordRecoveryTokenService = passwordRecoveryTokenService;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.agendaService = agendaService;
        this.emailService = emailService;
        this.servicioRepository = servicioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> {
            var msg = String.format("Username '%s' not found", username);
            return new UsernameNotFoundException(msg);
        });
    }

    Optional<TokenDTO> createUser(UserCreateDTO data) {
        return createUser(data, null);
    }

    Optional<TokenDTO> createUser(UserCreateDTO data, String frontendUrl) {
        if (userRepository.findByUsername(data.username()).isPresent()) {
            return Optional.empty();
        }

        var user = data.asUser(passwordEncoder::encode);
        userRepository.save(user);

        if (user.getRole() == UserRole.PROFESIONAL) {
            agendaService.createDefaultAgenda(user);
        }

        EmailVerificationToken verificationToken = emailVerificationTokenService.createFor(user);
        try {
            emailService.sendVerificationEmail(user.getUsername(), verificationToken.getToken(), frontendUrl);
        } catch (Exception e) {
            log.warn("Error al enviar email de verificación a {}: {}", user.getUsername(), e.getMessage());
        }

        return Optional.of(generateTokens(user));
    }

    Optional<TokenDTO> loginUser(UserCredentials data) {
        Optional<User> maybeUser = userRepository.findByUsername(data.username());

        return maybeUser
            .filter(user -> passwordEncoder.matches(data.password(), user.getPassword()))
            .map(user -> {
                if (!user.isActive()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tu cuenta está desactivada. Comunicate con el administrador para restaurarla.");
                }
                return generateTokens(user);
            });
    }

    Optional<TokenDTO> refresh(RefreshDTO data) {
        return refreshTokenService
            .findByValue(data.refreshToken())
            .map(RefreshToken::user)
            .map(this::generateTokens);
    }

    private TokenDTO generateTokens(User user) {
        String accessToken = jwtService.createToken(
            new JwtUserDetails(user.getUsername(), user.getRole(), user.isVerified())
        );

        RefreshToken refreshToken = refreshTokenService.createFor(user);

        return new TokenDTO(accessToken, refreshToken.value(), user.getRole().getImpliedRoles(), user.isVerified());
    }

    UserProfileDTO getUserProfile(String username) {
        return userRepository
            .findByUsername(username)
            .map(UserProfileDTO::fromUser)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    List<ProfessionalSearchResultDTO> searchUsers(
        UserRole role, String q, String sectorStr, String profesion, String servicio
    ) {
        String augmentedQ = q == null ? "" : q;
        if (sectorStr != null && !sectorStr.isBlank()) augmentedQ = augmentedQ + "&sector=" + sectorStr;
        if (profesion != null && !profesion.isBlank()) augmentedQ = augmentedQ + "&profesion=" + profesion;
        if (servicio != null && !servicio.isBlank()) augmentedQ = augmentedQ + "&servicio=" + servicio;
        return searchUsers(role, augmentedQ);
    }

    List<ProfessionalSearchResultDTO> searchUsers(UserRole role, String q) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        String text = q;
        SectorTrabajo sector = null;
        String profesion = "";
        String servicio = "";

        if (q != null && q.contains("&")) {
            String[] parts = q.split("&");
            text = parts[0];
            for (int i = 1; i < parts.length; i++) {
                String p = parts[i];
                int eq = p.indexOf('=');
                if (eq <= 0) continue;
                String k = p.substring(0, eq);
                String v = p.substring(eq + 1);
                if (k.equals("sector") && v != null && !v.isBlank()) {
                    try { sector = SectorTrabajo.valueOf(v); } catch (IllegalArgumentException ignored) {}
                } else if (k.equals("profesion")) {
                    profesion = v == null ? "" : v;
                } else if (k.equals("servicio")) {
                    servicio = v == null ? "" : v;
                }
            }
        }

        List<User> users;
        boolean hasName = text != null && !text.isBlank();
        boolean hasFilters = (sector != null) || (profesion != null && !profesion.isBlank())
            || (servicio != null && !servicio.isBlank());
        String roleStr = role.name();
        String sectorStr = sector != null ? sector.name() : null;

        if (!hasName && hasFilters) {
            users = userRepository.searchByRoleAndFiltersOnly(roleStr, currentUsername,
                sectorStr, profesion == null ? "" : profesion, servicio == null ? "" : servicio);
        } else if (!hasFilters) {
            users = userRepository.searchByRoleAndQuery(roleStr, text, currentUsername,
                servicio == null ? "" : servicio);
        } else {
            users = userRepository.searchByRoleAndQueryWithFilters(roleStr, text, currentUsername,
                sectorStr, profesion == null ? "" : profesion, servicio == null ? "" : servicio);
        }

        String finalServicio = servicio;
        return users.stream().map(user -> {
            List<ServicioPublicoDTO> servicios = List.of();
            if (finalServicio != null && !finalServicio.isBlank()) {
                var agendaIdOpt = agendaService.findAgendaIdByProfesionalId(user.getId());
                if (agendaIdOpt.isPresent()) {
                    servicios = servicioRepository.findByAgendaIdAndNombreContaining(agendaIdOpt.get(), finalServicio)
                        .stream().map(s -> new ServicioPublicoDTO(s.getId(), s.getNombre(), s.getPrecio(), s.isActivo()))
                        .toList();
                }
            }
            return ProfessionalSearchResultDTO.fromUser(user, servicios);
        }).toList();
    }

    UserPublicDTO getUserById(Long id) {
        return userRepository
            .findById(id)
            .map(UserPublicDTO::fromUser)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + id));
    }

    void startPasswordRecovery(PasswordRecoveryDTO data, String frontendUrl) {
        Optional<User> maybeUser = userRepository.findByUsername(data.email());
        if (maybeUser.isEmpty()) return;

        User user = maybeUser.get();
        PasswordRecoveryToken token = passwordRecoveryTokenService.createFor(user);
        try {
            emailService.sendPasswordRecoveryEmail(user.getUsername(), token.getToken(), frontendUrl);
        } catch (Exception e) {
            log.warn("Error al enviar email de recuperación a {}: {}", user.getUsername(), e.getMessage());
        }
    }

    @Transactional
    void resetPassword(String token, String newPassword) {
        PasswordRecoveryToken t = passwordRecoveryTokenService.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado"));

        if (t.isExpired() || t.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado");
        }

        User user = t.getUser();
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede ser igual a la actual");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        t.markAsUsed();
        passwordRecoveryTokenService.save(t);
    }

    @Transactional
    boolean updatePassword(String username, String currentPassword, String newPassword) {
        Optional<User> maybeUser = userRepository.findByUsername(username);
        if (maybeUser.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }

        User user = maybeUser.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Transactional
    boolean updateEmail(String currentUsername, String newEmail, String frontendUrl) {
        Optional<User> maybeUser = userRepository.findByUsername(currentUsername);
        if (maybeUser.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + currentUsername);
        }

        User user = maybeUser.get();
        if (currentUsername.equalsIgnoreCase(newEmail)) return true;

        Optional<User> conflict = userRepository.findByUsername(newEmail);
        if (conflict.isPresent()) return false;

        user.setUsername(newEmail);
        user.setVerified(false);
        userRepository.save(user);

        emailVerificationTokenService.deleteAllByUser(user);
        EmailVerificationToken token = emailVerificationTokenService.createFor(user);
        try {
            emailService.sendVerificationEmail(newEmail, token.getToken(), frontendUrl);
        } catch (Exception e) {
            log.warn("Error al enviar email de verificación tras cambio de email a {}: {}", newEmail, e.getMessage());
        }

        return true;
    }

    void updateFotoPerfil(String username, String fotoPerfil) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        user.setFotoPerfil(fotoPerfil);
        userRepository.save(user);
    }

    void updateDescripcion(String username, String descripcion) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (user.getRole() == UserRole.CLIENTE) {
            throw new IllegalArgumentException("Los clientes no pueden tener descripción profesional");
        }

        user.setDescripcion(descripcion);
        userRepository.save(user);
    }

    @Transactional
    void updateProfesion(String username, String profesion, String sector, String ubicacion) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (user.getRole() == UserRole.CLIENTE) {
            throw new IllegalArgumentException("Los clientes no pueden cambiar su profesión");
        }

        SectorTrabajo sec = null;
        if (sector != null && !sector.isBlank()) {
            try {
                sec = SectorTrabajo.valueOf(sector);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Sector inválido");
            }
        }

        user.setProfesion(profesion, sec);
        user.setUbicacion(ubicacion);
        userRepository.save(user);
    }

    @Transactional
    void verifyEmail(String token) {
        EmailVerificationToken t = emailVerificationTokenService.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado"));

        if (t.isExpired() || t.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado");
        }

        User user = t.getUser();
        user.verify();
        userRepository.save(user);
        t.markAsUsed();
        emailVerificationTokenService.save(t);
    }

    void resendVerificationEmail(String email, String frontendUrl) {
        Optional<User> maybeUser = userRepository.findByUsername(email);
        if (maybeUser.isEmpty() || maybeUser.get().isVerified()) return;

        User user = maybeUser.get();
        emailVerificationTokenService.deleteAllByUser(user);
        EmailVerificationToken token = emailVerificationTokenService.createFor(user);
        try {
            emailService.sendVerificationEmail(user.getUsername(), token.getToken(), frontendUrl);
        } catch (Exception e) {
            log.warn("Error al reenviar email de verificación a {}: {}", user.getUsername(), e.getMessage());
        }
    }
}
