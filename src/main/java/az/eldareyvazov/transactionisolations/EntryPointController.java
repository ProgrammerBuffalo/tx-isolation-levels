package az.eldareyvazov.transactionisolations;

import az.eldareyvazov.transactionisolations.anomaly.NonRepeatableReadAnomaly;
import az.eldareyvazov.transactionisolations.anomaly.DirtyReadAnomaly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EntryPointController {

    private final DirtyReadAnomaly dirtyReadAnomaly;

    private final NonRepeatableReadAnomaly nonRepeatableReadAnomaly;

    @PostMapping("dirty-read")
    public ResponseEntity<?> startDirtyRead() {
        dirtyReadAnomaly.runWithAnomaly();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("dirty-read-resolved")
    public ResponseEntity<?> startDirtyReadResolved() {
        dirtyReadAnomaly.runWithAnomalyResolved();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("non-repeatable-read")
    public ResponseEntity<?> startNonRepeatableRead() {
        nonRepeatableReadAnomaly.runWithAnomaly();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("non-repeatable-read-resolved")
    public ResponseEntity<?> startNonRepeatableReadResolved() {
        nonRepeatableReadAnomaly.runWithAnomalyResolved();
        return ResponseEntity.accepted().build();
    }

}
