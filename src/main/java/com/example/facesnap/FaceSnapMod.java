package com.example.facesnap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class FaceSnapMod implements ModInitializer {

    @Override
    public void onInitialize() {

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FaceSnapCommands.register(dispatcher);
        });

        // No handler register() exists — remove that call entirely.
        System.out.println("FaceSnap mod loaded.");
    }
}
