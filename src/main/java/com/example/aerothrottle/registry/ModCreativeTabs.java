package com.example.aerothrottle.registry;

import java.util.function.Supplier;

import com.example.aerothrottle.AeroThrottleMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AeroThrottleMod.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aero_throttle"))
            .icon(() -> new ItemStack(ModItems.REDSTONE_SPEED_MODULATOR.get()))
            .displayItems((params, output) -> output.accept(ModItems.REDSTONE_SPEED_MODULATOR.get()))
            .build());
}
