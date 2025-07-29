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
public class NonRepeatableReadAnomaly {
    private final UserRepository repository;
    private final PlatformTransactionManager transactionManager;

    private final Object DUMMY = new Object();
    private final SynchronousQueue<Object> nonBufferedQueue = new SynchronousQueue<>();

    public void runWithAnomaly() {
        runDBOperation(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public void runWithAnomalyResolved() {
        runDBOperation(TransactionDefinition.ISOLATION_REPEATABLE_READ);
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

        Thread secondOp = new Thread(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                var tx = new TransactionTemplate(transactionManager);
                tx.setIsolationLevel(transactionIsolationLevel);

                nonBufferedQueue.take();

                tx.execute(status -> {
                    secondOperation();
                    return null;
                });

                nonBufferedQueue.put(DUMMY);
            }
        }, "SECOND THREAD");

        firstOp.start();
        secondOp.start();

        firstOp.join();
        secondOp.join();

        clearUsers();
    }

    @SneakyThrows
    public void firstOperation() {
        log.info("start");

        var activeUsersCount = repository.countAllByIsActiveIsTrue();
        log.info("fetch(first-time) count of active users: {}", activeUsersCount);

        nonBufferedQueue.put(DUMMY);
        nonBufferedQueue.take();

        activeUsersCount = repository.countAllByIsActiveIsTrue();
        log.info("fetch(second-time) count of active users: {}", activeUsersCount);

        log.info("end");
    }

    @SneakyThrows
    public void secondOperation() {
        log.info("start");

        var user = repository.findFirstByIsActiveIsFalse();
        if (user.isEmpty()) {
            return;
        }
        log.info("fetch user: {}", user.get());

        user.get().setIsActive(true);
        repository.saveAndFlush(user.get());
        log.info("update user: {}", user.get());

        log.info("end");
    }

    private void initUsers() {
        repository.saveAllAndFlush(
                List.of(
                        new User("dennis_ritchie", true),
                        new User("ken_thompson", true),
                        new User("linus_torvalds", false)
                ));
    }

    private void clearUsers() {
        repository.deleteAllInBatch();
    }
}
