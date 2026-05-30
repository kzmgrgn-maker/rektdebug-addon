package com.rektdebug.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class RektMovement extends Module {

    private static final int MAX_BLOCKS = 1500;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDisplay  = settings.createGroup("Display");

    public final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Tarama yarıçapı (blok).")
        .defaultValue(6)
        .min(2)
        .sliderMax(12)
        .build()
    );

    public final Setting<Integer> scanInterval = sgGeneral.add(new IntSetting.Builder()
        .name("scan-interval")
        .description("Kaç tickte bir taransın.")
        .defaultValue(10)
        .min(5)
        .sliderMax(40)
        .build()
    );

    public final Setting<Integer> displayDuration = sgGeneral.add(new IntSetting.Builder()
        .name("display-duration")
        .description("Tespiti kaç ms göster.")
        .defaultValue(4000)
        .min(500)
        .sliderMax(10000)
        .build()
    );

    public final Setting<Boolean> showCoords = sgDisplay.add(new BoolSetting.Builder()
        .name("show-coords")
        .description("Koordinatları göster.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> chatLog = sgDisplay.add(new BoolSetting.Builder()
        .name("chat-log")
        .description("Tespitleri chate yaz.")
        .defaultValue(false)
        .build()
    );

    // CopyOnWriteArrayList prevents ConcurrentModificationException
    public final List<DetectedMovement> detections = new CopyOnWriteArrayList<>();

    private final Map<BlockPos, BlockState> prevBlocks = new HashMap<>();
    private int tickCounter = 0;

    public RektMovement() {
        super(RektDebugCategory.CATEGORY, "rekt-movement",
              "Yakın blok değişikliklerini tespit eder.");
    }

    @Override
    public void onActivate() {
        prevBlocks.clear();
        detections.clear();
        tickCounter = 0;
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

        // Remove expired detections safely (CopyOnWriteArrayList allows this)
        detections.removeIf(DetectedMovement::isExpired);

        if (tickCounter % scanInterval.get() == 0) {
            scanForChanges();
            snapshotBlocks();
        }
    }

    private void snapshotBlocks() {
        if (mc.world == null || mc.player == null) return;

        int px = (int) mc.player.getX();
        int py = (int) mc.player.getY();
        int pz = (int) mc.player.getZ();
        int r  = radius.get();

        prevBlocks.clear();
        int count = 0;

        outer:
        for (int x = px - r; x <= px + r; x++) {
            for (int y = Math.max(mc.world.getBottomY(), py - r);
                     y <= Math.min(mc.world.getTopY() - 1, py + r); y++) {
                for (int z = pz - r; z <= pz + r; z++) {
                    if (count >= MAX_BLOCKS) break outer;
                    BlockPos pos = new BlockPos(x, y, z);
                    prevBlocks.put(pos, mc.world.getBlockState(pos));
                    count++;
                }
            }
        }
    }

    private void scanForChanges() {
        if (mc.world == null || mc.player == null) return;

        List<DetectedMovement> newDetections = new ArrayList<>();

        for (Map.Entry<BlockPos, BlockState> entry : prevBlocks.entrySet()) {
            BlockPos   pos      = entry.getKey();
            BlockState oldState = entry.getValue();
            BlockState newState = mc.world.getBlockState(pos);

            if (!newState.equals(oldState)) {
                String type = "Block Change";
                String desc = oldState.getBlock().getName().getString()
                    + " -> " + newState.getBlock().getName().getString();

                newDetections.add(new DetectedMovement(
                    pos, type, desc, System.currentTimeMillis(), displayDuration.get()
                ));

                if (chatLog.get() && mc.player != null) {
                    String msg = showCoords.get()
                        ? String.format("[RektMovement] %s (%d,%d,%d): %s",
                            type, pos.getX(), pos.getY(), pos.getZ(), desc)
                        : "[RektMovement] " + type + ": " + desc;
                    mc.player.sendMessage(net.minecraft.text.Text.literal(msg), false);
                }
            }
        }

        detections.addAll(newDetections);
    }
}
