package com.zerokg2004.gravitygun.gravitygun.client.sound;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class HeldSoundInstance extends AbstractSoundInstance {
    private final LocalPlayer player;
    private boolean stopped = false;

    public HeldSoundInstance(LocalPlayer player, SoundEvent sound) {
        super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    public void tick() {
        if (player.isRemoved() || !player.isAlive()) {
            this.stopped = true;
        } else {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stopManually() {
        this.stopped = true;
    }
}