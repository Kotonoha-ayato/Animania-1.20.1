package com.animania.extra;

import com.animania.extra.client.render.ExtraHamsterWheelItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Renders the wheel's native mesh instead of the invisible block's placeholder cube. */
public final class ExtraHamsterWheelItem extends BlockItem {
    public ExtraHamsterWheelItem(Properties properties) {
        super(ExtraContent.HAMSTER_WHEEL.get(), properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new ExtraHamsterWheelItemRenderer(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
