package com.rektdebug.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class RektAlert extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> soundAlert = sgGeneral.add(new BoolSetting.Builder()
        .name("sound-alert")
        .description("Play a sound when movement is detected.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> chatAlert = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-alert")
        .description("Send a chat message when movement is detected.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> soundVolume = sgGeneral.add(new DoubleSetting.Builder()
        .name("sound-volume")
        .description("Volume of the alert sound.")
        .defaultValue(0.5)
        .min(0.1)
        .sliderMax(1.0)
        .build()
    );

    public final Setting<Integer> cooldownTicks = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Minimum ticks between alerts.")
        .defaultValue(20)
        .min(5)
        .sliderMax(100)
        .build()
    );

    private int lastAlertTick = 0;
    private int currentTick = 0;
    private int lastDetectionCount = 0;

    public RektAlert() {
        super(RektDebugCategory.CATEGORY, "rekt-alert", "Sends sound and chat alerts when RektMovement detects changes.");
    }

    @Override
    public void onActivate() {
        lastAlertTick = 0;
        currentTick = 0;
        lastDetectionCount = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        currentTick++;

        RektMovement movement = Modules.get().get(RektMovement.class);
        if (movement == null || !movement.isActive()) return;

        int currentCount = movement.detections.size();

        if (currentCount > lastDetectionCount && (currentTick - lastAlertTick) >= cooldownTicks.get()) {
            DetectedMovement latest = movement.detections.isEmpty()
                ? null
                : movement.detections.get(movement.detections.size() - 1);

            if (latest != null) {
                lastAlertTick = currentTick;

                if (soundAlert.get()) {
                    mc.getSoundManager().play(PositionedSoundInstance.master(
                        SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                        1.0f,
                        soundVolume.get().floatValue()
                    ));
                }

                if (chatAlert.get()) {
                    String msg = String.format(
                        "\u00a7c[RektAlert]\u00a7r %s at (%d, %d, %d): %s",
                        latest.type,
                        latest.pos.getX(),
                        latest.pos.getY(),
                        latest.pos.getZ(),
                        latest.description
                    );
                    mc.player.sendMessage(Text.literal(msg), false);
                }
            }
        }

        lastDetectionCount = currentCount;
    }
}
