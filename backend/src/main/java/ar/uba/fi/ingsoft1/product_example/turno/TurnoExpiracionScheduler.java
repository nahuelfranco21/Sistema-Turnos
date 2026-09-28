package ar.uba.fi.ingsoft1.product_example.turno;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TurnoExpiracionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TurnoExpiracionScheduler.class);

    private final TurnoJobHandler jobHandler;

    public TurnoExpiracionScheduler(TurnoJobHandler jobHandler) {
        this.jobHandler = jobHandler;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay-ms}", initialDelayString = "${app.scheduler.initial-delay-ms}")
    public void correrJobs() {
        try {
            int recordatorios = jobHandler.recordatorioPagoPendiente();
            if (recordatorios > 0) {
                log.info("Se enviaron {} recordatorio(s) de pago", recordatorios);
            }
        } catch (Exception e) {
            log.error("Error al ejecutar el recordatorio de pago: {}", e.getMessage(), e);
        }
        try {
            int cantidad = jobHandler.expirarTurnosSinPago();
            if (cantidad > 0) {
                log.info("Se vencieron {} turno(s) sin pago", cantidad);
            }
        } catch (Exception e) {
            log.error("Error al ejecutar la expiración de turnos sin pago: {}", e.getMessage(), e);
        }
        try {
            int recordatorios24h = jobHandler.enviarRecordatorios24Hs();
            if (recordatorios24h > 0) {
                log.info("Se enviaron {} recordatorio(s) de turno para mañana", recordatorios24h);
            }
        } catch (Exception e) {
            log.error("Error al ejecutar los recordatorios 24hs: {}", e.getMessage(), e);
        }
    }
}
