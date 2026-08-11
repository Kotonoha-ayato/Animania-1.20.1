package com.animania.gametest;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Emits an unambiguous runtime selector marker from a GameTest method.  The
 * closure auditor consumes these markers from the actual Forge server log;
 * source-file presence alone is never treated as execution evidence.
 */
public final class AnimaniaGameTestEvidence {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AnimaniaGameTestEvidence() { }

    public static void mark(String selector) {
        LOGGER.info("[ANIMANIA_TEST_SELECTOR] {}", selector);
    }
}
