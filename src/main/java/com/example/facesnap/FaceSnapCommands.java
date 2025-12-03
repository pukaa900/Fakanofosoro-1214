package com.example.facesnap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class FaceSnapCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("facesnap")
                    .then(literal("toggle")
                            .executes(ctx -> {
                                ServerCommandSource source = ctx.getSource();
                                ServerPlayerEntity player = source.getPlayer();
                                FaceSnapHandler.toggleMode(player.getUuid());
                                source.sendFeedback(() -> "FaceSnap mode toggled to: " + FaceSnapHandler.getMode(player.getUuid()), false);
                                return 1;
                            }))
                    .then(literal("mode")
                            .then(CommandManager.argument("mode", StringArgumentType.word())
                                    .executes(ctx -> {
                                        ServerCommandSource source = ctx.getSource();
                                        ServerPlayerEntity player = source.getPlayer();
                                        String m = StringArgumentType.getString(ctx, "mode");
                                        if (m.equalsIgnoreCase("face")) FaceSnapHandler.setMode(player.getUuid(), FaceSnapHandler.Mode.FACE);
                                        else if (m.equalsIgnoreCase("chaos")) FaceSnapHandler.setMode(player.getUuid(), FaceSnapHandler.Mode.CHAOS);
                                        else { source.sendError(new net.minecraft.text.LiteralText("Unknown mode: use face or chaos")); return 0; }
                                        source.sendFeedback(() -> "FaceSnap mode set to: " + FaceSnapHandler.getMode(player.getUuid()), false);
                                        return 1;
                                    })))
                    .then(literal("cooldown")
                            .then(CommandManager.argument("ms", IntegerArgumentType.integer(0))
                                    .executes(ctx -> {
                                        ServerCommandSource source = ctx.getSource();
                                        ServerPlayerEntity player = source.getPlayer();
                                        int ms = IntegerArgumentType.getInteger(ctx, "ms");
                                        FaceSnapHandler.setCooldown(player.getUuid(), ms);
                                        source.sendFeedback(() -> "FaceSnap cooldown set to: " + ms + " ms", false);
                                        return 1;
                                    })))
                    .then(literal("status")
                            .executes(ctx -> {
                                ServerCommandSource source = ctx.getSource();
                                ServerPlayerEntity player = source.getPlayer();
                                source.sendFeedback(() -> "Mode=" + FaceSnapHandler.getMode(player.getUuid()) + ", cooldown=" + FaceSnapHandler.getCooldown(player.getUuid()) + "ms", false);
                                return 1;
                            }))
            );
        });
    }
}
