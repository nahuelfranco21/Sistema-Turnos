package ar.uba.fi.ingsoft1.product_example.user.password_recovery;

import ar.uba.fi.ingsoft1.product_example.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
public class PasswordRecoveryToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Getter
    private String token;

    @ManyToOne
    @JoinColumn(nullable = false)
    @Getter
    private User user;

    @Column(nullable = false)
    @Getter
    private LocalDateTime expiration;

    @Column(nullable = false)
    @Getter
    private boolean used;

    public PasswordRecoveryToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        this.expiration = LocalDateTime.now().plusMinutes(30);
        this.used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiration);
    }

    public void markAsUsed() {
        this.used = true;
    }
}