package com.example.facesnap;

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

import java.util.HashMap;
import java.util.UUID;

public class FaceSnapHandler {

    private static final HashMap<UUID, Boolean> enabled = new HashMap<>();
    private static final HashMap<UUID, String> mode = new HashMap<>();
    private static final HashMap<UUID, Integer> cooldown = new HashMap<>();

    public static boolean toggleEnabled(UUID id) {
        boolean v = !enabled.getOrDefault(id, false);
        enabled.put(id, v);
        return v;
    }

    public static boolean setMode(UUID id, String m) {
        if (!m.equals("face") && !m.equals("chaos")) return false;
        mode.put(id, m);
        return true;
    }

    public static String getMode(UUID id) {
        return mode.getOrDefault(id, "face");
    }

    public static void setCooldown(UUID id, int ms) {
        cooldown.put(id, ms);
    }

    public static int getCooldown(UUID id) {
        return cooldown.getOrDefault(id, 0);
    }

    // === MAIN BLOCK HANDLER ===
    public static ActionResult onBlockUse(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {

        if (world.isClient) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) return ActionResult.PASS;

        ServerPlayerEntity spe = (ServerPlayerEntity) player;
        UUID id = player.getUuid();

        if (!enabled.getOrDefault(id, false)) return ActionResult.PASS;

        BlockPos targetPos = hitResult.getBlockPos();
        Direction side = hitResult.getSide();
        BlockPos placePos = targetPos.offset(side);

        // FIXED: no more getMaterial()
        BlockState state = world.getBlockState(placePos);

        if (!state.isAir() && !state.isReplaceable()) {
            return ActionResult.SUCCESS;
        }

        boolean placed = placeBlockAt(spe, stack, placePos, hand);

        if (placed) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }

    private static boolean placeBlockAt(ServerPlayerEntity player, ItemStack stack, BlockPos pos, Hand hand) {
        World world = player.getWorld();

        BlockItem bitem = (BlockItem) stack.getItem();
        Block block = bitem.getBlock();
        BlockState state = block.getDefaultState();

        world.setBlockState(pos, state);

        world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE,
                SoundCategory.BLOCKS, 1.0F, 1.0F);

        return true;
    }
}
