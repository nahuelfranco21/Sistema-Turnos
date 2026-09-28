package ar.uba.fi.ingsoft1.product_example.user.password_recovery;

import ar.uba.fi.ingsoft1.product_example.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PasswordRecoveryTokenService {

    private final PasswordRecoveryTokenRepository repository;

    @Autowired
    public PasswordRecoveryTokenService(
            PasswordRecoveryTokenRepository repository
    ) {
        this.repository = repository;
    }

    public PasswordRecoveryToken createFor(User user) {
        PasswordRecoveryToken token =
                new PasswordRecoveryToken(user);

        return repository.save(token);
    }

    public Optional<PasswordRecoveryToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    public void save(PasswordRecoveryToken token) {
        repository.save(token);
    }

    public void deleteAllByUser(User user) {
        repository.deleteByUser(user);
    }
}