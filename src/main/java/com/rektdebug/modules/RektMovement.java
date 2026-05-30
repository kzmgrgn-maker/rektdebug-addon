package com.rektdebug.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RektMovement extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDisplay = settings.createGroup("Display");

    public final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Block radius to scan for movement.")
        .defaultValue(16)
        .min(4)
        .sliderMax(32)
        .build()
    );

    public final Setting<Integer> displayDuration = sgGeneral.add(new IntSetting.Builder()
        .name("display-duration")
        .description("How long (ms) to show a detected movement.")
        .defaultValue(3000)
        .min(500)
        .sliderMax(10000)
        .build()
    );

    public final Setting<Boolean> showCoords = sgDisplay.add(new BoolSetting.Builder()
        .name("show-coords")
        .description("Show coordinates of detected movement.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> chatLog = sgDisplay.add(new BoolSetting.Builder()
        .name("chat-log")
        .description("Print detections to chat.")
        .defaultValue(false)
        .build()
    );

    private final Map<BlockPos, BlockState> prevBlocks = new HashMap<>();
    public final List<DetectedMovement> detections = new ArrayList<>();
    private int tickCounter = 0;

    public RektMovement() {
        super(RektDebugCategory.CATEGORY, "rekt-movement", "Detects block state changes and entity movements in your radius.");
    }

    @Override
    public void onActivate() {
        prevBlocks.clear();
        detections.clear();
        snapshotBlocks();
    }

    @Override
    public void onDeactivate() {
        prevBlocks.clear();
        detections.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        tickCounter++;

        Iterator<DetectedMovement> it = detections.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired()) it.remove();
        }

        if (tickCounter % 2 == 0) {
            scanForChanges();
            snapshotBlocks();
        }
    }

    private void snapshotBlocks() {
        if (mc.world == null || mc.player == null) return;
        World world = mc.world;
        Vec3d center = mc.player.getPos();
        int r = radius.get();
        prevBlocks.clear();

        for (int x = (int) center.x - r; x <= (int) center.x + r; x++) {
            for (int y = (int) center.y - r; y <= (int) center.y + r; y++) {
                for (int z = (int) center.z - r; z <= (int) center.z + r; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    prevBlocks.put(pos, world.getBlockState(pos));
                }
            }
        }
    }

    private void scanForChanges() {
        if (mc.world == null || mc.player == null) return;
        World world = mc.world;

        for (Map.Entry<BlockPos, BlockState> entry : prevBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState oldState = entry.getValue();
            BlockState newState = world.getBlockState(pos);

            if (!newState.equals(oldState)) {
                String type = "Block Change";
                String desc = oldState.getBlock().getName().getString()
                    + " -> " + newState.getBlock().getName().getString();

                DetectedMovement det = new DetectedMovement(pos, type, desc,
                    System.currentTimeMillis(), displayDuration.get());
                detections.add(det);

                if (chatLog.get()) {
                    String msg = showCoords.get()
                        ? String.format("[RektMovement] %s at (%d, %d, %d): %s", type, pos.getX(), pos.getY(), pos.getZ(), desc)
                        : String.format("[RektMovement] %s: %s", type, desc);
                    mc.player.sendMessage(net.minecraft.text.Text.literal(msg), false);
                }
            }
        }
    }
}
