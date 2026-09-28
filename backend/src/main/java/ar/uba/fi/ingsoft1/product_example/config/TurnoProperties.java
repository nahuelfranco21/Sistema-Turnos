package ar.uba.fi.ingsoft1.product_example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.turno")
public class TurnoProperties {

    private int expirationHours = 24;
    private int reminderHours = 23;
    private int reminder24hWindowMinutes = 5;

    public int getExpirationHours() {
        return expirationHours;
    }

    public void setExpirationHours(int expirationHours) {
        this.expirationHours = expirationHours;
    }

    public int getReminderHours() {
        return reminderHours;
    }

    public void setReminderHours(int reminderHours) {
        this.reminderHours = reminderHours;
    }

    public int getReminder24hWindowMinutes() {
        return reminder24hWindowMinutes;
    }

    public void setReminder24hWindowMinutes(int reminder24hWindowMinutes) {
        this.reminder24hWindowMinutes = reminder24hWindowMinutes;
    }
}
