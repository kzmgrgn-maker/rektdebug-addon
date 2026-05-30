package com.rektdebug.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class RektAlert extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> soundAlert = sgGeneral.add(new BoolSetting.Builder()
        .name("sound-alert")
        .description("Tespit sesli uyarı.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> chatAlert = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-alert")
        .description("Tespit chat uyarısı.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> soundVolume = sgGeneral.add(new DoubleSetting.Builder()
        .name("volume")
        .description("Ses seviyesi.")
        .defaultValue(0.5)
        .min(0.1)
        .sliderMax(1.0)
        .build()
    );

    public final Setting<Integer> cooldownTicks = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Uyarılar arası minimum tick.")
        .defaultValue(20)
        .min(5)
        .sliderMax(100)
        .build()
    );

    private int lastAlertTick = 0;
    private int currentTick   = 0;
    private int lastCount     = 0;

    public RektAlert() {
        super(RektDebugCategory.CATEGORY, "rekt-alert",
              "RektMovement tespitlerinde sesli/chat uyarısı verir.");
    }

    @Override
    public void onActivate() {
        lastAlertTick = 0;
        currentTick   = 0;
        lastCount     = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        currentTick++;

        RektMovement mov = Modules.get().get(RektMovement.class);
        if (mov == null || !mov.isActive()) return;

        int cur = mov.detections.size();
        if (cur > lastCount && (currentTick - lastAlertTick) >= cooldownTicks.get()) {

            DetectedMovement latest = mov.detections.isEmpty()
                ? null : mov.detections.get(mov.detections.size() - 1);

            if (latest != null) {
                lastAlertTick = currentTick;

                if (soundAlert.get()) {
                    // MC 1.21.1: SoundEvents returns RegistryEntry, use .value()
                    RegistryEntry<SoundEvent> entry = SoundEvents.BLOCK_NOTE_BLOCK_PLING;
                    mc.getSoundManager().play(PositionedSoundInstance.master(
                        entry.value(),
                        1.0f,
                        soundVolume.get().floatValue()
                    ));
                }

                if (chatAlert.get()) {
                    String msg = String.format(
                        "\u00a7c[RektAlert]\u00a7r %s (%d,%d,%d): %s",
                        latest.type,
                        latest.pos.getX(), latest.pos.getY(), latest.pos.getZ(),
                        latest.description
                    );
                    mc.player.sendMessage(Text.literal(msg), false);
                }
            }
        }
        lastCount = cur;
    }
}
