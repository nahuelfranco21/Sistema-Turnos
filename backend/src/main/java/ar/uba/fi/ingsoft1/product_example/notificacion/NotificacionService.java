package ar.uba.fi.ingsoft1.product_example.notificacion;

import ar.uba.fi.ingsoft1.product_example.email.EmailService;
import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final EmailService emailService;

    public NotificacionService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void onTurnoCancelado(Turno turno) {
        if (turno == null || turno.getCliente() == null) {
            return;
        }
        try {
            emailService.sendTurnoCancelledByProfesionalEmail(turno);
        } catch (Exception e) {
            log.error("No se pudo enviar el email de cancelación al cliente del turno {}: {}",
                    turno.getId(), e.getMessage(), e);
        }
    }

    public void onTurnoExpirado(Turno turno) {
        if (turno == null || turno.getCliente() == null) {
            return;
        }
        try {
            emailService.sendTurnoExpiredEmail(turno);
        } catch (Exception e) {
            log.error("No se pudo enviar el email de vencimiento al cliente del turno {}: {}",
                    turno.getId(), e.getMessage(), e);
        }
    }

    public boolean onTurnoPagoPendienteRecordatorio(Turno turno) {
        if (turno == null || turno.getCliente() == null) {
            return false;
        }
        try {
            emailService.sendTurnoPaymentReminderEmail(turno);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el recordatorio de pago al cliente del turno {}: {}",
                    turno.getId(), e.getMessage(), e);
            return false;
        }
    }

    public boolean onRecordatorio24Hs(Turno turno) {
        if (turno == null || turno.getCliente() == null) {
            return false;
        }
        try {
            emailService.sendRecordatorio24HsEmail(turno);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el recordatorio 24hs al cliente del turno {}: {}",
                    turno.getId(), e.getMessage(), e);
            return false;
        }
    }
}
