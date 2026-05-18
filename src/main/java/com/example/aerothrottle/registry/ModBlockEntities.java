package com.example.aerothrottle.registry;

import java.util.function.Supplier;

import com.example.aerothrottle.AeroThrottleMod;
import com.example.aerothrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AeroThrottleMod.MOD_ID);

    public static final Supplier<BlockEntityType<RedstoneSpeedModulatorBlockEntity>> REDSTONE_SPEED_MODULATOR =
        BLOCK_ENTITIES.register("redstone_speed_modulator",
            () -> BlockEntityType.Builder.of(
                (pos, state) -> new RedstoneSpeedModulatorBlockEntity(
                    ModBlockEntities.REDSTONE_SPEED_MODULATOR.get(), pos, state),
                ModBlocks.REDSTONE_SPEED_MODULATOR.get()
            ).build(null));
}
