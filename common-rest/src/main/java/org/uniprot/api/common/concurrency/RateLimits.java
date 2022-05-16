package org.uniprot.api.common.concurrency;

import dev.failsafe.RateLimiter;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

/**
 * Created 13/05/2022
 *
 * @author Edd
 */
@Builder(toBuilder = true)
@Getter
public class RateLimits {
    // By default set really high -- so that effectively there is no limit,
    // but tests will work because they hit things at a high rate
    public static final int SEARCH_RATE_LIMIT_PER_MINUTE = 50000;
    public static final int GET_ALL_RATE_LIMIT_PER_MINUTE = 50000;

    @Builder.Default
    private RateLimiter<Object> searchRateLimiter =
            RateLimiter.burstyBuilder(SEARCH_RATE_LIMIT_PER_MINUTE, Duration.ofMinutes(1)).build();

    @Builder.Default
    private RateLimiter<Object> getAllRateLimiter =
            RateLimiter.burstyBuilder(GET_ALL_RATE_LIMIT_PER_MINUTE, Duration.ofMinutes(1)).build();
}
