package com.example.facesnap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;


public class FaceSnapCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
            CommandManager.literal("facesnap")
                // /facesnap toggle
                .then(CommandManager.literal("toggle")
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        ServerPlayerEntity player = source.getPlayer();
                        FaceSnapHandler.toggleEnabled(player.getUuid());

                        source.sendFeedback(
                                () -> Text.literal("FaceSnap mode toggled to: " +
                                        FaceSnapHandler.getMode(player.getUuid())),
                                false
                        );
                        return 1;
                    })
                )

                // /facesnap mode <face|chaos>
                .then(CommandManager.literal("mode")
                    .then(CommandManager.argument("mode", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            String mode = StringArgumentType.getString(ctx, "mode");

                            if (!FaceSnapHandler.setMode(player.getUuid(), mode)) {
                                source.sendError(Text.literal("Unknown mode: use face or chaos"));
                                return 0;
                            }

                            source.sendFeedback(
                                    () -> Text.literal("FaceSnap mode set to: " +
                                            FaceSnapHandler.getMode(player.getUuid())),
                                    false
                            );
                            return 1;
                        })
                    )
                )

                // /facesnap cooldown <ms>
                .then(CommandManager.literal("cooldown")
                    .then(CommandManager.argument("ms", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            int ms = IntegerArgumentType.getInteger(ctx, "ms");

                            FaceSnapHandler.setCooldown(player.getUuid(), ms);

                            source.sendFeedback(
                                    () -> Text.literal("FaceSnap cooldown set to: " + ms + " ms"),
                                    false
                            );
                            return 1;
                        })
                    )
                )

                // /facesnap status
                .then(CommandManager.literal("status")
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        ServerPlayerEntity player = source.getPlayer();

                        source.sendFeedback(
                                () -> Text.literal(
                                        "Mode=" + FaceSnapHandler.getMode(player.getUuid()) +
                                        ", cooldown=" + FaceSnapHandler.getCooldown(player.getUuid()) + "ms"
                                ),
                                false
                        );
                        return 1;
                    })
                )
        );
    }
}
