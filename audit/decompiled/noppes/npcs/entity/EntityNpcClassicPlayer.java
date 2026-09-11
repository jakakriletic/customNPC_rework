/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.world.World;
import noppes.npcs.entity.EntityCustomNpc;

public class EntityNpcClassicPlayer
extends EntityCustomNpc {
    public EntityNpcClassicPlayer(World world) {
        super(world);
        this.display.setSkinTexture("customnpcs:textures/entity/humanmale/steve.png");
    }
}

