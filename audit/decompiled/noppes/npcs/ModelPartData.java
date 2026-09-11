/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ModelPartData {
    private static Map<String, ResourceLocation> resources = new HashMap<String, ResourceLocation>();
    public int color = 0xFFFFFF;
    public int colorPattern = 0xFFFFFF;
    public byte type = 0;
    public byte pattern = 0;
    public boolean playerTexture = false;
    public String name;
    private ResourceLocation location;

    public ModelPartData(String name) {
        this.name = name;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74774_a("Type", this.type);
        compound.func_74768_a("Color", this.color);
        compound.func_74757_a("PlayerTexture", this.playerTexture);
        compound.func_74774_a("Pattern", this.pattern);
        return compound;
    }

    public void readFromNBT(NBTTagCompound compound) {
        if (!compound.func_74764_b("Type")) {
            this.type = (byte)-1;
            return;
        }
        this.type = compound.func_74771_c("Type");
        this.color = compound.func_74762_e("Color");
        this.playerTexture = compound.func_74767_n("PlayerTexture");
        this.pattern = compound.func_74771_c("Pattern");
        this.location = null;
    }

    public ResourceLocation getResource() {
        if (this.location != null) {
            return this.location;
        }
        String texture = this.name + "/" + this.type;
        this.location = resources.get(texture);
        if (this.location != null) {
            return this.location;
        }
        this.location = new ResourceLocation("moreplayermodels:textures/" + texture + ".png");
        resources.put(texture, this.location);
        return this.location;
    }

    public void setType(int type) {
        this.type = (byte)type;
        this.location = null;
    }

    public String toString() {
        return "Color: " + this.color + " Type: " + this.type;
    }

    public String getColor() {
        String str = Integer.toHexString(this.color);
        while (str.length() < 6) {
            str = "0" + str;
        }
        return str;
    }
}

