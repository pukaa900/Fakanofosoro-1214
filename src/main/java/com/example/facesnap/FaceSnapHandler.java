package com.example.facesnap;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FaceSnapHandler {
    // per-player mode and cooldown
    public enum Mode { FACE, CHAOS }

    private static final Map<UUID, Mode> modes = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastPlace = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> cooldownMs = new ConcurrentHashMap<>();
    private static final Random RAND = new Random();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> handleUseBlock(player, world, hand, hitResult));
    }

    private static ActionResult handleUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) return ActionResult.PASS;

        ServerPlayerEntity spe = (ServerPlayerEntity) player;
        UUID id = spe.getUuid();

        int cd = cooldownMs.getOrDefault(id, 100); // default 100ms
        long now = System.currentTimeMillis();
        long last = lastPlace.getOrDefault(id, 0L);
        if (now - last < cd) {
            return ActionResult.SUCCESS; // cancel vanilla placement
        }

        Mode mode = modes.getOrDefault(id, Mode.FACE);

        // determine placement position
        BlockPos targetPos = hitResult.getBlockPos();
        Direction side = hitResult.getSide();
        BlockPos placePos = targetPos.offset(side);

        if (mode == Mode.CHAOS) {
            // random face selection
            Direction[] dirs = Direction.values();
            side = dirs[RAND.nextInt(dirs.length)];
            placePos = targetPos.offset(side);

            // random 0-1 additional offset outward
            int extra = RAND.nextInt(2); // 0 or 1
            if (extra > 0) placePos = placePos.offset(side, extra);

            // random skip chance
            double skip = 0.25;
            if (RAND.nextDouble() < skip) {
                lastPlace.put(id, now);
                return ActionResult.SUCCESS;
            }

            // random timing jitter (0-200ms)
            int jitter = RAND.nextInt(200);
            schedulePlacementWithDelay(spe.getServer(), jitter, spe, stack.copy(), placePos, hand);
            lastPlace.put(id, now + jitter);
            return ActionResult.SUCCESS;
        }

        // FACE mode: place exactly at target + side
        boolean placed = placeBlockAt((ServerPlayerEntity) player, stack, placePos, hand);
        if (placed) {
            lastPlace.put(id, now);
            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS; // cancel vanilla placement in all handled cases
    }

    private static void schedulePlacementWithDelay(MinecraftServer server, int delayMs, ServerPlayerEntity player, ItemStack stack, BlockPos pos, Hand hand) {
        new Thread(() -> {
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            server.execute(() -> placeBlockAtSync(player, stack, pos, hand));
        }).start();
    }

    private static void placeBlockAtSync(ServerPlayerEntity player, ItemStack stack, BlockPos pos, Hand hand) {
        placeBlockAt(player, stack, pos, hand);
    }

    private static boolean placeBlockAt(ServerPlayerEntity player, ItemStack stack, BlockPos pos, Hand hand) {
        World world = player.getWorld();
        if (!world.getWorldBorder().contains(pos)) return false;

        if (!world.isAir(pos) && !world.getBlockState(pos).getMaterial().isReplaceable()) {
            // don't overwrite non-replaceable blocks
            return false;
        }

        BlockItem bitem = (BlockItem) stack.getItem();
        Block block = bitem.getBlock();
        BlockState state = block.getDefaultState();

        world.setBlockState(pos, state, 3);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);

        if (!player.isCreative()) {
            player.getInventory().removeOne(stack);
        }

        return true;
    }

    // command-accessible utilities
    public static void toggleMode(UUID playerId) {
        modes.put(playerId, modes.getOrDefault(playerId, Mode.FACE) == Mode.FACE ? Mode.CHAOS : Mode.FACE);
    }

    public static Mode getMode(UUID playerId) { return modes.getOrDefault(playerId, Mode.FACE); }

    public static void setMode(UUID playerId, Mode m) { modes.put(playerId, m); }

    public static void setCooldown(UUID playerId, int ms) { cooldownMs.put(playerId, ms); }

    public static int getCooldown(UUID playerId) { return cooldownMs.getOrDefault(playerId, 100); }
}
