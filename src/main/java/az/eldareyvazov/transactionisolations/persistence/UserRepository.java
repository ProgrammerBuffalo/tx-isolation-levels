package az.eldareyvazov.transactionisolations.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    int countAllByIsActiveIsTrue();

    Optional<User> findFirstByIsActiveIsFalse();
}
