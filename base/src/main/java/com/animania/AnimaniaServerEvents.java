package com.animania;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Server-authoritative hooks shared by every addon. */
public final class AnimaniaServerEvents {
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AnimaniaAnimalEntity animal) {
            animal.ensureValidState();
        }
    }
}

