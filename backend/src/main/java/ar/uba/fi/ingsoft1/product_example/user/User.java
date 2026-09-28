package ar.uba.fi.ingsoft1.product_example.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "users")
@NoArgsConstructor
public class User implements UserDetails, UserCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    @Getter
    private String username;

    @Column(nullable = false)
    @Getter
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    private UserRole role;

    @Column(nullable = false)
    @Getter
    private String nombre;

    @Column(nullable = false)
    @Getter
    private String apellido;

    @Column(nullable = false)
    @Getter
    private LocalDate fechaNacimiento;

    @Column(nullable = true)
    @Getter
    private String profesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @Getter
    private SectorTrabajo sector;

    @Column(nullable = true)
    @Getter
    private String ubicacion;

    @Column(nullable = true, columnDefinition = "TEXT")
    @Getter
    private String fotoPerfil;

    @Column(nullable = true, columnDefinition = "TEXT")
    @Getter
    private String descripcion;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Getter
    private boolean verified = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Getter
    private boolean active = true;

    public User(String username, String password, UserRole role, String nombre,
                String apellido, LocalDate fechaNacimiento, String profesion, SectorTrabajo sector) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.profesion = profesion;
        this.sector = sector;
    }

    public void setProfesion(String profesion, SectorTrabajo sector) {
        this.profesion = profesion;
        this.sector = sector;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void verify() {
        this.verified = true;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public void setUsername(String username) {
        this.username = username;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Set<UserRole> getRoles() {
        return role.getImpliedRoles();
    }

    @Override
    public String username() {
        return this.username;
    }

    @Override
    public String password() {
        return this.password;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getImpliedRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.toStringWithPrefix()))
                .collect(Collectors.toSet());
    }
}
