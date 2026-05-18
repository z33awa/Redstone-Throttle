package com.example.aerothrottle.network;

import com.example.aerothrottle.AeroThrottleMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AeroThrottleMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkRegistry {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
            UpdateModulatorPacket.TYPE,
            UpdateModulatorPacket.STREAM_CODEC,
            UpdateModulatorPacket::handle);
    }
}
