package ar.uba.fi.ingsoft1.product_example.user.email_verification;

import ar.uba.fi.ingsoft1.product_example.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmailVerificationTokenService {

    private final EmailVerificationTokenRepository repository;

    @Autowired
    public EmailVerificationTokenService(EmailVerificationTokenRepository repository) {
        this.repository = repository;
    }

    public EmailVerificationToken createFor(User user) {
        return repository.save(new EmailVerificationToken(user));
    }

    public Optional<EmailVerificationToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    public void save(EmailVerificationToken token) {
        repository.save(token);
    }

    public void deleteAllByUser(User user) {
        repository.deleteByUser(user);
    }
}
