package az.eldareyvazov.transactionisolations.anomaly;

import az.eldareyvazov.transactionisolations.persistence.User;
import az.eldareyvazov.transactionisolations.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.SynchronousQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class DirtyReadAnomaly {
    private final UserRepository repository;
    private final PlatformTransactionManager transactionManager;

    private final Object DUMMY = new Object();
    private final SynchronousQueue<Object> nonBufferedQueue = new SynchronousQueue<>();

    public void runWithAnomaly() {
        runDBOperation(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
    }

    public void runWithAnomalyResolved() {
        runDBOperation(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @SneakyThrows
    private void runDBOperation(int transactionIsolationLevel) {
        initUsers();

        Thread firstOp = new Thread(() -> {
            var tx = new TransactionTemplate(transactionManager);
            tx.setIsolationLevel(transactionIsolationLevel);
            tx.execute(status -> {
                firstOperation();
                return null;
            });
        }, "FIRST THREAD");

        Thread secondOp = new Thread(() -> {
            var tx = new TransactionTemplate(transactionManager);
            tx.setIsolationLevel(transactionIsolationLevel);
            tx.execute(status -> {
                secondOperation();
                return null;
            });
        }, "SECOND THREAD");

        firstOp.start();
        secondOp.start();

        firstOp.join();
        secondOp.join();

        clearUsers();
    }

    @SneakyThrows
    private void firstOperation() {
        nonBufferedQueue.take();
        log.info("start");

        var user = repository.findFirstByIsActiveIsFalse();
        if (user.isEmpty()) {
            return;
        }
        log.info("fetch user: {}", user.get());

        user.get().setIsActive(true);
        repository.saveAndFlush(user.get());
        log.info("update user: {}", user.get());

        nonBufferedQueue.put(DUMMY);
        nonBufferedQueue.take();

        log.warn("rollback update");

        throw new RuntimeException("Forcing rollback manually");
    }

    @SneakyThrows
    private void secondOperation() {
        log.info("start");

        var activeUsersCount = repository.countAllByIsActiveIsTrue();
        log.info("first fetch count of active users: {}", activeUsersCount);

        nonBufferedQueue.put(DUMMY);
        nonBufferedQueue.take();

        activeUsersCount = repository.countAllByIsActiveIsTrue();
        log.info("second fetch count of active users: {}", activeUsersCount);

        nonBufferedQueue.put(DUMMY);

        log.info("end");
    }

    private void initUsers() {
        repository.saveAllAndFlush(
                List.of(
                        new User("dennis_ritchie", true),
                        new User("ken_thompson", false)
                ));
    }

    private void clearUsers() {
        repository.deleteAllInBatch();
    }
}
