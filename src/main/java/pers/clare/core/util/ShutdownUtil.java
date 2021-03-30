package pers.clare.core.util;


import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ShutdownUtil {
    private static final int DEFAULT_AWAIT_SECONDS = 3 * 60;

    private ShutdownUtil() {
    }

    public static void await(ExecutorService executor) {
        await("", executor, DEFAULT_AWAIT_SECONDS);
    }

    public static void await(String name, ExecutorService executor) {
        await(name, executor, DEFAULT_AWAIT_SECONDS);
    }

    public static void await(ExecutorService executor, int awaitSeconds) {
        await("", executor, awaitSeconds);
    }

    public static void await(String name, ExecutorService executor, int awaitSeconds) {
        executor.shutdown();
        log.info("{} executor shutdown...", name);
        try {
            if (!executor.awaitTermination(awaitSeconds, TimeUnit.SECONDS)) {
                throw new InterruptedException(
                        String.format("%s executor did not shut down gracefully within %s seconds. Proceeding with forceful shutdown"
                                , name
                                , awaitSeconds
                        )
                );
            }
            log.info("{} executor shutdown complete", name);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
