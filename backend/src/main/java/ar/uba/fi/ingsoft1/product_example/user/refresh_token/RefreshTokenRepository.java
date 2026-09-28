package ar.uba.fi.ingsoft1.product_example.user.refresh_token;

import ar.uba.fi.ingsoft1.product_example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    void deleteByUser(User user);
}
