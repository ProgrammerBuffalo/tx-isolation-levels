package az.eldareyvazov.transactionisolations.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private Integer money;

    @Override
    public String toString() {
        return String.format("id: %d, username: %s, money: %d", id, username, money);
    }
}
