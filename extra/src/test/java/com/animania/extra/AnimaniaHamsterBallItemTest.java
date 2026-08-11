package com.animania.extra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AnimaniaHamsterBallItemTest {
    @Test
    void legacyMetadataOrderIsPreserved() {
        assertEquals(15, AnimaniaHamsterBallColors.modernDyeId(0));
        assertEquals(14, AnimaniaHamsterBallColors.modernDyeId(1));
        assertEquals(8, AnimaniaHamsterBallColors.modernDyeId(7));
        assertEquals(0, AnimaniaHamsterBallColors.modernDyeId(15));
    }
}
