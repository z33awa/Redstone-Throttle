package com.example.aerothrottle.client;

import com.example.aerothrottle.AeroThrottleMod;
import com.example.aerothrottle.registry.ModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AeroThrottleMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            ModBlockEntities.REDSTONE_SPEED_MODULATOR.get(),
            RedstoneSpeedModulatorRenderer::new);
    }
}
