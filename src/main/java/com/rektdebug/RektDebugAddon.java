package com.rektdebug;

import com.rektdebug.modules.RektAlert;
import com.rektdebug.modules.RektMovement;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class RektDebugAddon extends MeteorAddon {

    public static final RektDebugAddon INSTANCE = new RektDebugAddon();

    @Override
    public void onInitialize() {
        Modules.get().add(new RektMovement());
        Modules.get().add(new RektAlert());
    }

    @Override
    public String getPackage() {
        return "com.rektdebug";
    }
}
