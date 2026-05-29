package com.z33awa.redstonethrottle.client;

import com.z33awa.redstonethrottle.RedstoneThrottleMod;
import com.z33awa.redstonethrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;

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
        if (be == null) {
            RedstoneThrottleMod.LOGGER.warn("Ignored screen open request: modulator block entity was null");
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            RedstoneThrottleMod.LOGGER.warn("Ignored screen open request for {}: Minecraft instance unavailable", be.getBlockPos());
            return;
        }

        try {
            minecraft.setScreen(new ModulatorScreen(be));
        } catch (RuntimeException e) {
            RedstoneThrottleMod.LOGGER.error("Failed to open modulator screen at {}", be.getBlockPos(), e);
        }
    }
}
