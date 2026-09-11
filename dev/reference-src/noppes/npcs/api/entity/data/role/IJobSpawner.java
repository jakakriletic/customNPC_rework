/*
 * Decompiled with CFR 0.152.
 */
package noppes.npcs.api.entity.data.role;

import noppes.npcs.api.entity.IEntityLivingBase;

public interface IJobSpawner {
    public IEntityLivingBase spawnEntity(int var1);

    public void removeAllSpawned();
}

