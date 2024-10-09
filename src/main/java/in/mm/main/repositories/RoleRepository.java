package in.mm.main.repositories;

import in.mm.main.model.AppRole;
import in.mm.main.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleName(AppRole appRole);
}
