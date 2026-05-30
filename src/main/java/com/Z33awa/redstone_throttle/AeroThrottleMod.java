package com.Z33awa.redstone_throttle;

import com.Z33awa.redstone_throttle.registry.ModBlockEntities;
import com.Z33awa.redstone_throttle.registry.ModBlocks;
import com.Z33awa.redstone_throttle.registry.ModCreativeTabs;
import com.Z33awa.redstone_throttle.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AeroThrottleMod.MOD_ID)
public class AeroThrottleMod {

    public static final String MOD_ID = "redstone_throttle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroThrottleMod(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }
}
