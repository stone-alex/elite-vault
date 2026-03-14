package elite.vault.api.jobs;

/**
 * Shared response DTOs for the async job pattern.
 * All endpoints return JobAcceptedResponse on POST,
 * and JobErrorResponse on failure.
 */
public class JobDtos {

    /**
     * Returned with HTTP 202 on POST — client uses job ID to poll.
     */
    public record JobAcceptedResponse(String job) {
    }

    /**
     * Returned with HTTP 200 when job failed internally.
     */
    public record JobErrorResponse(String error) {
    }
}