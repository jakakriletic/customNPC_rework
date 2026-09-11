/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.projectile.EntityThrowable
 */
package noppes.npcs.api.wrapper;

import net.minecraft.entity.projectile.EntityThrowable;
import noppes.npcs.api.entity.IThrowable;
import noppes.npcs.api.wrapper.EntityWrapper;

public class ThrowableWrapper<T extends EntityThrowable>
extends EntityWrapper<T>
implements IThrowable {
    public ThrowableWrapper(T entity) {
        super(entity);
    }

    @Override
    public int getType() {
        return 11;
    }

    @Override
    public boolean typeOf(int type) {
        return type == 11 ? true : super.typeOf(type);
    }
}

