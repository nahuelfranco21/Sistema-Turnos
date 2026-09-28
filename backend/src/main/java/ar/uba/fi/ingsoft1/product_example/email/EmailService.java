package ar.uba.fi.ingsoft1.product_example.email;

import ar.uba.fi.ingsoft1.product_example.turno.Turno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String fromAddress;

    @Autowired
    public EmailService(JavaMailSender mailSender,
                        @Value("${app.frontend-url}") String frontendUrl,
                        @Value("${app.email.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordRecoveryEmail(String toEmail, String token, String headerFrontendUrl) {
        String baseUrl = headerFrontendUrl != null ? headerFrontendUrl : frontendUrl;
        String recoveryLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Recuperación de Contraseña";
        String body = "Haz clic en el siguiente enlace para restablecer tu contraseña: " + recoveryLink;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendVerificationEmail(String toEmail, String token, String headerFrontendUrl) {
        String baseUrl = headerFrontendUrl != null ? headerFrontendUrl : frontendUrl;
        String verificationLink = baseUrl + "/verify-email?token=" + token;
        String subject = "Verificá tu cuenta en Sistema Turnos";
        String body = "¡Bienvenido! Hacé clic en el siguiente enlace para verificar tu cuenta:\n\n"
                + verificationLink
                + "\n\nEste enlace expira en 24 horas.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendTurnoCancelledByProfesionalEmail(Turno turno) {
        var cliente = turno.getCliente();
        var profesional = turno.getAgenda().getProfesional();
        String rebookLink = frontendUrl + "/reprogramar/" + turno.getId();

        String subject = "Tu profesional reprogramó tu turno";
        String body = "Hola " + cliente.getNombre() + " " + cliente.getApellido() + ",\n\n"
                + "Te informamos que tu turno con " + profesional.getNombre() + " " + profesional.getApellido()
                + " programado para el día " + turno.getFecha() + " a las " + turno.getBloqueHorario()
                + " hs fue cancelado por el profesional. Como ya tenías la seña registrada, no es necesario que vuelvas a pagar.\n\n"
                + "Podés elegir un nuevo horario desde el siguiente enlace:\n"
                + rebookLink + "\n\n"
                + "¡Gracias!";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(cliente.getUsername());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email de cancelación enviado al cliente {} por turno {}",
                cliente.getUsername(), turno.getId());
    }

    public void sendTurnoExpiredEmail(Turno turno) {
        var cliente = turno.getCliente();
        var profesional = turno.getAgenda().getProfesional();
        String rebookLink = frontendUrl + "/profesional/" + profesional.getId();

        String subject = "Tu reserva venció por falta de pago";
        String body = "Hola " + cliente.getNombre() + " " + cliente.getApellido() + ",\n\n"
                + "Tu turno con " + profesional.getNombre() + " " + profesional.getApellido()
                + " programado para el día " + turno.getFecha() + " a las " + turno.getBloqueHorario()
                + " hs fue liberado porque no registramos el pago dentro de las 24 horas posteriores a la reserva.\n\n"
                + "Si querés sacar un nuevo turno, podés hacerlo desde el siguiente enlace:\n"
                + rebookLink + "\n\n"
                + "¡Gracias!";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(cliente.getUsername());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email de vencimiento enviado al cliente {} por turno {}",
                cliente.getUsername(), turno.getId());
    }

    public void sendTurnoPaymentReminderEmail(Turno turno) {
        var cliente = turno.getCliente();
        var profesional = turno.getAgenda().getProfesional();
        String rebookLink = frontendUrl + "/profesional/" + profesional.getId();

        String subject = "Te queda poco tiempo para confirmar el pago de tu turno";
        String body = "Hola " + cliente.getNombre() + " " + cliente.getApellido() + ",\n\n"
                + "Te recordamos que tu turno con " + profesional.getNombre() + " " + profesional.getApellido()
                + " programado para el día " + turno.getFecha() + " a las " + turno.getBloqueHorario()
                + " hs todavía no tiene el pago registrado.\n\n"
                + "Si no confirmás el pago en las próximas horas, vamos a liberar el horario automáticamente.\n\n"
                + "Podés volver al sistema y confirmar el pago desde tu cuenta:\n"
                + frontendUrl + "\n\n"
                + "¡Gracias!";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(cliente.getUsername());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Recordatorio de pago enviado al cliente {} por turno {}",
                cliente.getUsername(), turno.getId());
    }

    public void sendRecordatorio24HsEmail(Turno turno) {
        var cliente = turno.getCliente();
        var profesional = turno.getAgenda().getProfesional();

        String subject = "Recordatorio: tenés un turno mañana";
        String body = "Hola " + cliente.getNombre() + " " + cliente.getApellido() + ",\n\n"
                + "Te recordamos que mañana tenés un turno con "
                + profesional.getNombre() + " " + profesional.getApellido()
                + " a las " + turno.getBloqueHorario() + " hs.\n\n"
                + "Si necesitás cancelarlo, podés hacerlo desde el sistema:\n"
                + frontendUrl + "\n\n"
                + "¡Hasta mañana!";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(cliente.getUsername());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Recordatorio 24hs enviado al cliente {} por turno {}",
                cliente.getUsername(), turno.getId());
    }
}
