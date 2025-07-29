package az.eldareyvazov.transactionisolations.persistence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String username;

    @NonNull
    @Column(name = "is_active")
    private Boolean isActive;

    @Override
    public String toString() {
        return String.format("id: %d, username: %s, isActive: %b", id, username, isActive);
    }
}
