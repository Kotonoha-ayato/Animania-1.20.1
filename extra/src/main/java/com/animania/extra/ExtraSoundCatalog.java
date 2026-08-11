package com.animania.extra;

import java.util.ArrayList;
import java.util.List;

/** Pure, source-derived sound ID catalog; kept independent of Forge bootstrap for unit auditing. */
public final class ExtraSoundCatalog {
    public static final List<String> IDS;

    static {
        List<String> ids = new ArrayList<>();
        ids.add("hamsterdeath"); range(ids, "hamstereat", 1, 2); ids.add("hamsterhurt1"); range(ids, "hamsterliving", 1, 3);
        range(ids, "frogliving", 1, 3); ids.add("reeee"); ids.add("ooooohh"); range(ids, "toadliving", 1, 4);
        range(ids, "dartfrogliving", 1, 4); ids.add("ferrethurt1"); range(ids, "ferretliving", 1, 6);
        range(ids, "rabbit", 1, 4); range(ids, "rabbithurt", 1, 2); range(ids, "hedgehoghurt", 1, 2);
        range(ids, "hedgehogliving", 1, 5); range(ids, "peacock", 1, 10); range(ids, "peacockhurt", 1, 2);
        IDS = List.copyOf(ids);
    }

    private static void range(List<String> ids, String prefix, int first, int last) {
        for (int value = first; value <= last; value++) ids.add(prefix + value);
    }

    private ExtraSoundCatalog() { }
}
