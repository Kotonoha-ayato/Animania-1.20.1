package com.animania.common.helper;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Checked error used when a legacy or modern config value cannot be interpreted. */
public class InvalidConfigException extends Exception {
    private static final Logger LOGGER = LogUtils.getLogger();

    public InvalidConfigException(String cause) {
        super(cause);
    }

    /** Preserve the legacy diagnostic entry point while using the modern logger. */
    public void printException() {
        LOGGER.error(getMessage(), this);
    }
}
