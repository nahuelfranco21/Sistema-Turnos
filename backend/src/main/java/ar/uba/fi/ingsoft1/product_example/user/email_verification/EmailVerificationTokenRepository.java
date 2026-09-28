package ar.uba.fi.ingsoft1.product_example.user.email_verification;

import ar.uba.fi.ingsoft1.product_example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);
}
