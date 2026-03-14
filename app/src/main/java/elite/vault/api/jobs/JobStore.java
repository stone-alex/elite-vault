package elite.vault.api.jobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.*;

/**
 * Generic async job store for Elite Vault API endpoints.
 * <p>
 * All endpoints follow the same contract:
 * POST  → returns {"job": "<uuid>"} with HTTP 202 immediately
 * GET   /{job} → 202 while computing, 200 + result on completion (flushes job on delivery)
 * GET   /{job} → 404 if job unknown or TTL expired
 * <p>
 * Jobs are flushed from the map on first successful delivery.
 * Unclaimed jobs (client crash etc.) are reaped after JOB_TTL_MS.
 *
 * @param <T> The result type for this store's jobs (e.g. API_TradeRouteDto)
 */
public class JobStore<T> {

    private static final long JOB_TTL_MS = 10 * 60 * 1000L; // 10 minutes
    private static final Logger log = LogManager.getLogger(JobStore.class);

    private final ConcurrentHashMap<String, Job<T>> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final String name;

    /**
     * @param name    Human-readable name for logging (e.g. "trade-route")
     * @param threads Number of worker threads for this store's jobs
     */
    public JobStore(String name, int threads) {
        this.name = name;
        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, name + "-worker-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });

        // TTL reaper — daemon thread, runs every minute
        ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, name + "-reaper");
            t.setDaemon(true);
            return t;
        });
        reaper.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            int before = jobs.size();
            jobs.entrySet().removeIf(e -> (now - e.getValue().createdAt) > JOB_TTL_MS);
            int removed = before - jobs.size();
            if (removed > 0) log.info("[{}] reaped {} expired job(s)", name, removed);
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Submit a computation. Returns the job ID immediately.
     * The callable is executed on this store's thread pool.
     */
    public String submit(Callable<T> computation) {
        String jobId = UUID.randomUUID().toString();
        Job<T> job = new Job<>(jobId);
        jobs.put(jobId, job);

        executor.submit(() -> {
            try {
                T result = computation.call();
                job.complete(result);
                log.info("[{}] job {} completed", name, jobId);
            } catch (Exception e) {
                log.error("[{}] job {} failed", name, jobId, e);
                job.fail(e.getMessage());
            }
        });

        return jobId;
    }

    /**
     * Poll for a job result.
     * Returns null if job not found (404 case).
     * Returns the Job object — caller checks job.isDone().
     */
    public Job<T> poll(String jobId) {
        return jobs.get(jobId);
    }

    /**
     * Remove a job — called after successful delivery to client.
     */
    public void flush(String jobId) {
        jobs.remove(jobId);
    }

    // -------------------------------------------------------------------------
    // Job state container
    // -------------------------------------------------------------------------
    public static class Job<T> {
        public final String jobId;
        public final long createdAt = System.currentTimeMillis();

        private volatile boolean done = false;
        private volatile boolean failed = false;
        private volatile T result = null;
        private volatile String error = null;

        Job(String jobId) {
            this.jobId = jobId;
        }

        void complete(T result) {
            this.result = result;
            this.done = true;
        }

        void fail(String error) {
            this.error = error;
            this.failed = true;
            this.done = true;
        }

        public boolean isDone() {
            return done;
        }

        public boolean isFailed() {
            return failed;
        }

        public T getResult() {
            return result;
        }

        public String getError() {
            return error;
        }
    }
}