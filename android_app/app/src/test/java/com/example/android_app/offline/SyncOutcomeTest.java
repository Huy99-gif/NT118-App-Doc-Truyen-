package com.example.android_app.offline;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SyncOutcomeTest {
    @Test
    public void success_isDone() {
        assertEquals(SyncOutcome.DONE, SyncOutcome.fromHttpCode(200));
        assertEquals(SyncOutcome.DONE, SyncOutcome.fromHttpCode(204));
    }

    @Test
    public void unauthorized_stopsAuth() {
        assertEquals(SyncOutcome.STOP_AUTH, SyncOutcome.fromHttpCode(401));
    }

    @Test
    public void knownClientErrors_dropRecord() {
        assertEquals(SyncOutcome.DROP, SyncOutcome.fromHttpCode(400));
        assertEquals(SyncOutcome.DROP, SyncOutcome.fromHttpCode(404));
        assertEquals(SyncOutcome.DROP, SyncOutcome.fromHttpCode(409));
    }

    @Test
    public void other4xx_dropsRecord() {
        assertEquals(SyncOutcome.DROP, SyncOutcome.fromHttpCode(403));
        assertEquals(SyncOutcome.DROP, SyncOutcome.fromHttpCode(422));
    }

    @Test
    public void retry_givesUpAfterMaxAttempts_soOneRowCannotBlockQueue() {
        assertEquals(SyncOutcome.RETRY,
                SyncOutcome.withAttempts(SyncOutcome.RETRY, SyncOutcome.MAX_RETRIES - 1));
        assertEquals(SyncOutcome.DROP,
                SyncOutcome.withAttempts(SyncOutcome.RETRY, SyncOutcome.MAX_RETRIES));
        assertEquals(SyncOutcome.DONE, SyncOutcome.withAttempts(SyncOutcome.DONE, 50));
    }

    @Test
    public void serverErrors_retry() {
        assertEquals(SyncOutcome.RETRY, SyncOutcome.fromHttpCode(500));
        assertEquals(SyncOutcome.RETRY, SyncOutcome.fromHttpCode(503));
    }
}
