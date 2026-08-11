package com.animania.farm;

import com.animania.farm.client.render.FarmHiveItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Uses the exact native hive mesh for inventory, hand and dropped-item rendering. */
public final class FarmHiveItem extends BlockItem {
    private final boolean wild;

    public FarmHiveItem(Block block, boolean wild, Properties properties) {
        super(block, properties);
        this.wild = wild;
    }

    public boolean isWild() {
        return wild;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new FarmHiveItemRenderer(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
