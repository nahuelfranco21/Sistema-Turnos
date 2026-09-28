package ar.uba.fi.ingsoft1.product_example.user;

import ar.uba.fi.ingsoft1.product_example.agenda.AgendaService;
import ar.uba.fi.ingsoft1.product_example.turno.TurnoService;
import ar.uba.fi.ingsoft1.product_example.user.email_verification.EmailVerificationTokenService;
import ar.uba.fi.ingsoft1.product_example.user.password_recovery.PasswordRecoveryTokenService;
import ar.uba.fi.ingsoft1.product_example.user.refresh_token.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final AgendaService agendaService;
    private final TurnoService turnoService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final RefreshTokenService refreshTokenService;

    public AdminUserService(UserRepository userRepository,
                            AgendaService agendaService,
                            TurnoService turnoService,
                            PasswordRecoveryTokenService passwordRecoveryTokenService,
                            EmailVerificationTokenService emailVerificationTokenService,
                            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.agendaService = agendaService;
        this.turnoService = turnoService;
        this.passwordRecoveryTokenService = passwordRecoveryTokenService;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    public UserAdminDTO getUserAdminDetail(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Long agendaId = agendaService.findAgendaIdByProfesionalId(id).orElse(null);
        return UserAdminDTO.fromUser(user, agendaId);
    }

    public void adminUpdateUser(Long id, AdminUpdateUserDTO data) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!user.getUsername().equalsIgnoreCase(data.email())) {
            if (userRepository.findByUsername(data.email()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
            }
        }
        user.setNombre(data.nombre());
        user.setApellido(data.apellido());
        user.setUsername(data.email());
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (turnoService.hasPendingFutureTurnosByClienteId(id)
                || turnoService.hasPendingFutureTurnosByProfesionalId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El usuario tiene turnos pendientes. Cancelalos antes de eliminarlo.");
        }
        turnoService.deleteAllTurnosByClienteId(id);
        agendaService.deleteAgendaAndTurnosByProfesionalId(id, turnoService);
        passwordRecoveryTokenService.deleteAllByUser(user);
        emailVerificationTokenService.deleteAllByUser(user);
        refreshTokenService.deleteAllByUser(user);
        userRepository.delete(user);
    }

    public void setUserActive(Long id, boolean active) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!active) {
            if (turnoService.hasPendingFutureTurnosByClienteId(id)
                    || turnoService.hasPendingFutureTurnosByProfesionalId(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario tiene turnos pendientes. Cancelalos antes de desactivarlo.");
            }
        }
        user.setActive(active);
        userRepository.save(user);
    }

    public void promoteToProfesional(Long id, AdminPromoteDTO data) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getRole() != UserRole.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya es profesional o superadmin");
        }
        SectorTrabajo sec;
        try {
            sec = SectorTrabajo.valueOf(data.sector());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sector inválido");
        }
        user.setRole(UserRole.PROFESIONAL);
        user.setProfesion(data.profesion(), sec);
        user.setUbicacion(data.ubicacion());
        userRepository.save(user);
        agendaService.createDefaultAgenda(user);
    }

    public void adminSetVerified(Long id, boolean verified) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        user.setVerified(verified);
        userRepository.save(user);
    }
}
