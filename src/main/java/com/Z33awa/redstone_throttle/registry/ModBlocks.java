package com.Z33awa.redstone_throttle.registry;

import com.Z33awa.redstone_throttle.AeroThrottleMod;
import com.Z33awa.redstone_throttle.content.modulator.RedstoneSpeedModulatorBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AeroThrottleMod.MOD_ID);

    public static final DeferredBlock<RedstoneSpeedModulatorBlock> REDSTONE_SPEED_MODULATOR =
        BLOCKS.register("redstone_speed_modulator",
            () -> new RedstoneSpeedModulatorBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f, 6.0f)
                .sound(SoundType.METAL)
                .noOcclusion()));
}
