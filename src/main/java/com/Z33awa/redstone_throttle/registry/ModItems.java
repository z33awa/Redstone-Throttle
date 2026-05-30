package com.Z33awa.redstone_throttle.registry;

import com.Z33awa.redstone_throttle.AeroThrottleMod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AeroThrottleMod.MOD_ID);

    public static final DeferredItem<BlockItem> REDSTONE_SPEED_MODULATOR =
        ITEMS.registerSimpleBlockItem(ModBlocks.REDSTONE_SPEED_MODULATOR, new Item.Properties());
}
