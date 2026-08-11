package com.animania.farm;

import java.util.ArrayList;
import java.util.List;

/** Pure, source-derived sound ID catalog; kept independent of Forge bootstrap for unit auditing. */
public final class FarmSoundCatalog {
    public static final List<String> IDS;

    static {
        List<String> ids = new ArrayList<>();
        range(ids, "sheepliving", 1, 7); range(ids, "lambliving", 1, 2); ids.add("sheephurt1");
        range(ids, "goatliving", 1, 5); range(ids, "kidliving", 1, 3); range(ids, "goathurt", 1, 2); range(ids, "kidhurt", 1, 2);
        range(ids, "horsehurt", 1, 3); range(ids, "horseliving", 1, 6);
        range(ids, "crow", 1, 3); range(ids, "cluck", 1, 6); range(ids, "hurt", 1, 2); range(ids, "death", 1, 2);
        range(ids, "angrybull", 1, 3); range(ids, "bullmoo", 1, 8); range(ids, "cowdeath", 1, 2);
        range(ids, "coweat", 1, 2); range(ids, "hurtcalf", 1, 2); range(ids, "cowhurt", 1, 2);
        range(ids, "moocalf", 1, 3); range(ids, "moo", 1, 8);
        range(ids, "hog", 1, 5); range(ids, "pig", 1, 7); range(ids, "pighurt", 1, 2);
        range(ids, "piglethurt", 1, 3); range(ids, "piglet", 1, 3); ids.add("hitch"); ids.add("unhitch");
        IDS = List.copyOf(ids);
    }

    private static void range(List<String> ids, String prefix, int first, int last) {
        for (int value = first; value <= last; value++) ids.add(prefix + value);
    }

    private FarmSoundCatalog() { }
}
