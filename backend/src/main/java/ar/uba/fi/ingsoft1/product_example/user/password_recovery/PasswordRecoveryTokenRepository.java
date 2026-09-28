package ar.uba.fi.ingsoft1.product_example.user.password_recovery;

import ar.uba.fi.ingsoft1.product_example.user.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PasswordRecoveryTokenRepository
        extends CrudRepository<PasswordRecoveryToken, Long> {

    Optional<PasswordRecoveryToken> findByToken(String token);

    void deleteByUser(User user);
}