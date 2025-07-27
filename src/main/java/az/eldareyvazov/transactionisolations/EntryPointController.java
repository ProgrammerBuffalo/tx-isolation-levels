package az.eldareyvazov.transactionisolations;

import az.eldareyvazov.transactionisolations.levels.ReadCommittedLevel;
import az.eldareyvazov.transactionisolations.levels.ReadUncommittedLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EntryPointController {

    private final ReadUncommittedLevel readUncommittedLevel;

    private final ReadCommittedLevel readCommittedLevel;

    @PostMapping("start-read-uncommitted")
    public ResponseEntity<?> startUncommittedLevel() {
        readUncommittedLevel.runDBOperation();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("start-read-committed")
    public ResponseEntity<?> startCommittedLevel() {
        readCommittedLevel.runDBOperation();
        return ResponseEntity.accepted().build();
    }
}
