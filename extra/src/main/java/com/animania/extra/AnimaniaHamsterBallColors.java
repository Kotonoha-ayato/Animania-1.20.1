package com.animania.extra;

/** Pure conversion between 1.12 dye-damage order and modern dye IDs. */
final class AnimaniaHamsterBallColors {
    private AnimaniaHamsterBallColors() { }

    static int modernDyeId(int legacyColor) {
        return 15 - Math.max(0, Math.min(15, legacyColor));
    }
}
