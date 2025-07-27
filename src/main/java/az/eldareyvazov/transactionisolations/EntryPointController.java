package az.eldareyvazov.transactionisolations;

import az.eldareyvazov.transactionisolations.levels.ReadUncommittedLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EntryPointController {

    private final ReadUncommittedLevel uncommittedLevel;

    @PostMapping("start-uncommitted")
    public ResponseEntity<?> startUncommittedLevel() {
        uncommittedLevel.runDBOperation();
        return ResponseEntity.accepted().build();
    }
}
