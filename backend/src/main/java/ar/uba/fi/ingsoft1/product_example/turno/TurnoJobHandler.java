package ar.uba.fi.ingsoft1.product_example.turno;

import ar.uba.fi.ingsoft1.product_example.config.TurnoProperties;
import ar.uba.fi.ingsoft1.product_example.notificacion.NotificacionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TurnoJobHandler {

    private final TurnoRepository turnoRepository;
    private final TurnoProperties turnoProperties;
    private final NotificacionService notificacionService;

    public TurnoJobHandler(TurnoRepository turnoRepository,
                           TurnoProperties turnoProperties,
                           NotificacionService notificacionService) {
        this.turnoRepository = turnoRepository;
        this.turnoProperties = turnoProperties;
        this.notificacionService = notificacionService;
    }

    public int expirarTurnosSinPago() {
        LocalDateTime limite = LocalDateTime.now().minusHours(turnoProperties.getExpirationHours());
        List<Turno> vencidos =
            turnoRepository.findByEstadoAndClienteIsNotNullAndCreatedAtBefore(
                TurnoEstado.OCUPADO_SIN_CONFIRMAR, limite
            );

        for (Turno t : vencidos) {
            notificacionService.onTurnoExpirado(t);
            turnoRepository.delete(t);
        }

        return vencidos.size();
    }

    public int recordatorioPagoPendiente() {
        LocalDateTime limite = LocalDateTime.now().minusHours(turnoProperties.getReminderHours());
        List<Turno> pendientes =
            turnoRepository.findByEstadoAndClienteIsNotNullAndReminderSentFalseAndCreatedAtBefore(
                TurnoEstado.OCUPADO_SIN_CONFIRMAR, limite
            );

        int recordatorios = 0;
        for (Turno t : pendientes) {
            if (notificacionService.onTurnoPagoPendienteRecordatorio(t)) {
                t.setReminderSent(true);
                turnoRepository.save(t);
                recordatorios++;
            }
        }
        return recordatorios;
    }

    public int enviarRecordatorios24Hs() {
        LocalDate manana = LocalDate.now().plusDays(1);
        List<Turno> turnosManana =
            turnoRepository.findByEstadoAndClienteIsNotNullAndFechaAndRecordatorio24hEnviadoFalse(
                TurnoEstado.CONFIRMADO, manana
            );
        ZoneId zonaArgentina = ZoneId.of("America/Argentina/Buenos_Aires");
        ZonedDateTime ahoraLocal = ZonedDateTime.now(zonaArgentina);
        int windowMinutes = turnoProperties.getReminder24hWindowMinutes();
        ZonedDateTime desdeLocal = ahoraLocal.plusHours(24).minusMinutes(windowMinutes);
        ZonedDateTime hastaLocal = ahoraLocal.plusHours(24).plusMinutes(windowMinutes);
        int recordatorios = 0;
        for (Turno t : turnosManana) {
            ZonedDateTime fechaHoraTurno = ZonedDateTime.of(t.getFecha(), t.getBloqueHorario(), zonaArgentina);
            if (!fechaHoraTurno.isBefore(desdeLocal) && !fechaHoraTurno.isAfter(hastaLocal)) {
                if (notificacionService.onRecordatorio24Hs(t)) {
                    t.setRecordatorio24hEnviado(true);
                    turnoRepository.save(t);
                    recordatorios++;
                }
            }
        }
        return recordatorios;
    }
}
