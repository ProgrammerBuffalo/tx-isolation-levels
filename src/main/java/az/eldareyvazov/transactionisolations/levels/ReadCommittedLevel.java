package az.eldareyvazov.transactionisolations.levels;

import az.eldareyvazov.transactionisolations.persistence.User;
import az.eldareyvazov.transactionisolations.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.SynchronousQueue;

@Component
@RequiredArgsConstructor
public class ReadCommittedLevel {

    private static final Logger log = LoggerFactory.getLogger(ReadUncommittedLevel.class);

    private final static int FIRST_USER_ID = 1;
    private final static int SECOND_USER_ID = 2;

    private final UserRepository repository;

    private final PlatformTransactionManager transactionManager;

    private final String FIRST_THREAD_NAME = "FIRST THREAD";
    private final String SECOND_THREAD_NAME = "SECOND THREAD";

    private final Object DUMMY = new Object();
    private final SynchronousQueue<Object> nonBufferedQueue = new SynchronousQueue<>();

    @SneakyThrows
    public void runDBOperation() {
        Thread firstOp = new Thread(() -> {
            try {
                nonBufferedQueue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
            tx.execute(status -> {
                firstOperation();
                try {
                    nonBufferedQueue.put(DUMMY);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        }, FIRST_THREAD_NAME);

        Thread secondOp = new Thread(() -> {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
            tx.execute(status -> {
                secondOperation();
                return null;
            });
        }, SECOND_THREAD_NAME);

        firstOp.start();
        secondOp.start();
    }

    @SneakyThrows
    public void firstOperation() {
        String threadName = Thread.currentThread().getName();
        log.info("START THREAD-NAME: {}", threadName);

        log.info("START FIND USER THREAD-NAME: {}", threadName);
        User user = repository.findById(FIRST_USER_ID).orElseThrow(RuntimeException::new);
        log.info("END FIND USER THREAD-NAME: {}, USER: {}", threadName, user);

        log.info("START CHANGE USER'S MONEY THREAD-NAME: {}, USER: {}", threadName, user);
        user.setMoney(user.getMoney() - 100);
        repository.saveAndFlush(user);
        log.info("END CHANGE USER'S MONEY THREAD-NAME: {}, USER: {}", threadName, user);

        log.info("END THREAD-NAME: {}", threadName);
    }

    @SneakyThrows
    public void secondOperation() {
        String threadName = Thread.currentThread().getName();
        log.info("START THREAD-NAME: {}", threadName);

        log.info("START FIND USER THREAD-NAME: {}", threadName);
        User userFrom = repository.findById(FIRST_USER_ID).orElseThrow(RuntimeException::new);
        log.info("END FIND USER THREAD-NAME: {}, USER: {}", threadName, userFrom);

        nonBufferedQueue.put(DUMMY);
        nonBufferedQueue.take();

        log.info("START FIND USER THREAD-NAME: {}", threadName);
        userFrom = repository.findById(FIRST_USER_ID).orElseThrow(RuntimeException::new);
        log.info("END FIND USER THREAD-NAME: {}, USER: {}", threadName, userFrom);


        if (userFrom.getMoney() >= 100) {
            log.info("START FIND USER THREAD-NAME: {}", threadName);
            User userTo = repository.findById(SECOND_USER_ID).orElseThrow(RuntimeException::new);
            log.info("END FIND USER THREAD-NAME: {}, USER: {}", threadName, userTo);

            userTo.setMoney(userTo.getMoney() + 100);
            userFrom.setMoney(userFrom.getMoney() - 100);
            log.info("START CHANGE USER'S MONEY THREAD-NAME: {}, USER: {}", threadName, userTo);
            repository.saveAndFlush(userTo);
            log.info("END CHANGE USER'S MONEY THREAD-NAME: {}, USER: {}", threadName, userTo);
        }

        log.info("START CHANGE USER'S MONEY THREAD-NAME: {}, USER: {}", threadName, userFrom);
        repository.saveAndFlush(userFrom);
        log.info("END CHANGE USER'S MONEY THREAD-NAME: {}, USER: {}", threadName, userFrom);
    }
}
