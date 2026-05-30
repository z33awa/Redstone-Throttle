package com.example.aerothrottle.client;

import com.example.aerothrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Indirection so Block code (which runs on both sides) can request a screen open
 * without statically referencing client-only classes.
 */
public class ScreenOpener {
    public static void openModulatorScreen(RedstoneSpeedModulatorBlockEntity be) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        Minecraft.getInstance().setScreen(new ModulatorScreen(be));
    }
}
