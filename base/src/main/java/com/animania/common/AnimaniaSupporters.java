package com.animania.common;

import java.util.Set;
import java.util.UUID;

/** Immutable Java 17 replacement for the 1.12 PatreonHandler UUID list. */
public final class AnimaniaSupporters {
    private static final Set<UUID> IDS = Set.of(
            UUID.fromString("3507ad5c-d868-453c-90a0-3b8092999d22"),
            UUID.fromString("615d0847-0ddd-4bc9-a410-355a79cdd519"),
            UUID.fromString("04372b9e-4e31-4a69-9660-4ac1cc2dbdb4"),
            UUID.fromString("bd1a8633-8ca7-4b5d-9ef7-5d1dfde310f3"),
            UUID.fromString("9f6e90b5-cd29-49fd-8858-87e3fa4ca150"),
            UUID.fromString("9c8a434b-2adc-4385-bc58-a8f08db3ebb9"),
            UUID.fromString("1fbb6b43-af8f-40a1-9c26-c5128ff513ce"),
            UUID.fromString("fdd172ed-dcb5-49ed-93ae-7847f6e88bef"),
            UUID.fromString("b0bb3fff-ddbd-40c6-95d7-51dfcaece879"),
            UUID.fromString("1f471396-84d9-41a0-ad9b-52a722c12a6a"),
            UUID.fromString("6265251a-5dae-4eb4-a95e-dfd922ad8fd5"),
            UUID.fromString("407720e3-22a6-4a78-9e64-0f6e82e00609"),
            UUID.fromString("f865dbb5-552c-4f62-8d73-2360634f6edd"),
            UUID.fromString("2ea31456-53d4-4eee-b034-5126fc45e1c7"),
            UUID.fromString("eef69763-af9d-4ce6-bef7-cd9bc839004c"),
            UUID.fromString("de8fc838-7468-4c5f-8f48-012808e079e1"),
            UUID.fromString("0b667a5c-9c03-4b8b-8cf4-ba3b31e09934"),
            UUID.fromString("ad425147-a229-48a0-930b-ec58f9c5dd84"),
            UUID.fromString("6d84bec0-b5c4-4467-8957-8458c3c18cb0")
    );

    public static boolean contains(UUID id) {
        return IDS.contains(id);
    }

    public static int size() {
        return IDS.size();
    }

    private AnimaniaSupporters() { }
}
