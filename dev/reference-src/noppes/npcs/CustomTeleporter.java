/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.world.Teleporter
 *  net.minecraft.world.WorldServer
 */
package noppes.npcs;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class CustomTeleporter
extends Teleporter {
    public CustomTeleporter(WorldServer par1WorldServer) {
        super(par1WorldServer);
    }

    public void placeInPortal(Entity entityIn, float rotationYaw) {
    }
}

