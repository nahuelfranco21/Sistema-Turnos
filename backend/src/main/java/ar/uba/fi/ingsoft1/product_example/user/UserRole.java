package ar.uba.fi.ingsoft1.product_example.user;

import lombok.experimental.FieldNameConstants;

import java.util.Set;

@FieldNameConstants(onlyExplicitlyIncluded = true)
public enum UserRole {
    @FieldNameConstants.Include CLIENTE,
    @FieldNameConstants.Include PROFESIONAL,
    @FieldNameConstants.Include SUPER_ADMIN;

    public String toStringWithPrefix() {
        return "ROLE_" + this;
    }

    public Set<UserRole> getImpliedRoles() {
        return switch (this) {
            case CLIENTE -> Set.of(CLIENTE);
            case PROFESIONAL -> Set.of(PROFESIONAL, CLIENTE);
            case SUPER_ADMIN -> Set.of(SUPER_ADMIN, PROFESIONAL, CLIENTE);
        };
    }
}
