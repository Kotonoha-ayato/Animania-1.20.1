package com.animania;

import net.minecraftforge.fml.VersionChecker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimaniaUpdateNotificationTest {
    @Test
    void onlyOutdatedStatusesNotifyWhenEnabled() {
        assertTrue(AnimaniaServerEvents.shouldNotifyUpdate(true, VersionChecker.Status.OUTDATED));
        assertTrue(AnimaniaServerEvents.shouldNotifyUpdate(true, VersionChecker.Status.BETA_OUTDATED));
        assertFalse(AnimaniaServerEvents.shouldNotifyUpdate(false, VersionChecker.Status.OUTDATED));
        assertFalse(AnimaniaServerEvents.shouldNotifyUpdate(true, VersionChecker.Status.UP_TO_DATE));
        assertFalse(AnimaniaServerEvents.shouldNotifyUpdate(true, VersionChecker.Status.FAILED));
        assertFalse(AnimaniaServerEvents.shouldNotifyUpdate(true, VersionChecker.Status.PENDING));
    }
}
