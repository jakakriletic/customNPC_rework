/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.passive.EntityTameable
 */
package noppes.npcs.api.entity;

import net.minecraft.entity.passive.EntityTameable;
import noppes.npcs.api.entity.IAnimal;

public interface IPixelmon<T extends EntityTameable>
extends IAnimal<T> {
    public Object getPokemonData();
}

