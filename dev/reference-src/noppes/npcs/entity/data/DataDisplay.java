/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.Property
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTUtil
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.StringUtils
 *  net.minecraft.world.BossInfo$Color
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.registry.EntityEntry
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 */
package noppes.npcs.entity.data;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.ModelData;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.data.INPCDisplay;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class DataDisplay
implements INPCDisplay {
    EntityNPCInterface npc;
    private String name;
    private String title = "";
    public byte skinType = 0;
    private String url = "";
    public GameProfile playerProfile;
    private String texture = "customnpcs:textures/entity/humanmale/steve.png";
    private String cloakTexture = "";
    private String glowTexture = "";
    private int visible = 0;
    private int modelSize = 5;
    private int showName = 0;
    private int skinColor = 0xFFFFFF;
    private boolean disableLivingAnimation = false;
    private boolean noHitbox = false;
    private byte showBossBar = 0;
    private BossInfo.Color bossColor = BossInfo.Color.PINK;

    public DataDisplay(EntityNPCInterface npc) {
        this.npc = npc;
        String[] names = new String[]{"Noppes", "Noppes", "Noppes", "Noppes", "Atesson", "Rothcersul", "Achdranys", "Pegato", "Chald", "Gareld", "Nalworche", "Ineald", "Tia'kim", "Torerod", "Turturdar", "Ranler", "Dyntan", "Oldrake", "Gharis", "Elmn", "Tanal", "Waran-ess", "Ach-aldhat", "Athi", "Itageray", "Tasr", "Ightech", "Gakih", "Adkal", "Qua'an", "Sieq", "Urnp", "Rods", "Vorbani", "Smaik", "Fian", "Hir", "Ristai", "Kineth", "Naif", "Issraya", "Arisotura", "Honf", "Rilfom", "Estz", "Ghatroth", "Yosil", "Darage", "Aldny", "Tyltran", "Armos", "Loxiku", "Burhat", "Tinlt", "Ightyd", "Mia", "Ken", "Karla", "Lily", "Carina", "SubPai", "Daniel", "Slater", "Zidane", "Valentine", "Eirina", "Carnow", "Grave", "Shadow", "Drakken", "Kaoz", "Silk", "Drake", "Oldam", "Lynxx", "Lenyx", "Winter", "Seth", "Apolitho", "Amethyst", "Ankin", "Seinkan", "Ayumu", "Sakamoto", "Divina", "Div", "Magia", "Magnus", "Tiakono", "Ruin", "Hailinx", "Ethan", "Wate", "Carter", "William", "Brion", "Sparrow", "Basrrelen", "Gyaku", "Claire", "Crowfeather", "Blackwell", "Raven", "Farcri", "Lucas", "Bangheart", "Kamoku", "Kyoukan", "Blaze", "Benjamin", "Larianne", "Kakaragon", "Melancholy", "Epodyno", "Thanato", "Mika", "Dacks", "Ylander", "Neve", "Meadow", "Cuero", "Embrera", "Eldamore", "Faolan", "Chim", "Nasu", "Kathrine", "Ariel", "Arei", "Demytrix", "Kora", "Ava", "Larson", "Leonardo", "Wyrl", "Sakiama", "Lambton", "Kederath", "Malus", "Riplette", "Andern", "Ezall", "Lucien", "Droco", "Cray", "Tymen", "Zenix", "Entranger", "Saenorath", "Chris", "Christine", "Marble", "Mable", "Ross", "Rose", "Xalgan ", "Kennet", "Aphmau"};
        this.name = names[new Random().nextInt(names.length)];
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.setString("Name", this.name);
        nbttagcompound.setString("Title", this.title);
        nbttagcompound.setString("SkinUrl", this.url);
        nbttagcompound.setString("Texture", this.texture);
        nbttagcompound.setString("CloakTexture", this.cloakTexture);
        nbttagcompound.setString("GlowTexture", this.glowTexture);
        nbttagcompound.setByte("UsingSkinUrl", this.skinType);
        if (this.playerProfile != null) {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            NBTUtil.writeGameProfile((NBTTagCompound)nbttagcompound1, (GameProfile)this.playerProfile);
            nbttagcompound.setTag("SkinUsername", (NBTBase)nbttagcompound1);
        }
        nbttagcompound.setInteger("Size", this.modelSize);
        nbttagcompound.setInteger("ShowName", this.showName);
        nbttagcompound.setInteger("SkinColor", this.skinColor);
        nbttagcompound.setInteger("NpcVisible", this.visible);
        nbttagcompound.setBoolean("NoLivingAnimation", this.disableLivingAnimation);
        nbttagcompound.setBoolean("IsStatue", this.noHitbox);
        nbttagcompound.setByte("BossBar", this.showBossBar);
        nbttagcompound.setInteger("BossColor", this.bossColor.ordinal());
        return nbttagcompound;
    }

    public void readToNBT(NBTTagCompound nbttagcompound) {
        this.setName(nbttagcompound.getString("Name"));
        this.title = nbttagcompound.getString("Title");
        byte prevSkinType = this.skinType;
        String prevTexture = this.texture;
        String prevUrl = this.url;
        String prevPlayer = this.getSkinPlayer();
        this.url = nbttagcompound.getString("SkinUrl");
        this.skinType = nbttagcompound.getByte("UsingSkinUrl");
        this.texture = nbttagcompound.getString("Texture");
        this.cloakTexture = nbttagcompound.getString("CloakTexture");
        this.glowTexture = nbttagcompound.getString("GlowTexture");
        this.playerProfile = null;
        if (this.skinType == 1) {
            if (nbttagcompound.hasKey("SkinUsername", 10)) {
                this.playerProfile = NBTUtil.readGameProfileFromNBT((NBTTagCompound)nbttagcompound.getCompoundTag("SkinUsername"));
            } else if (nbttagcompound.hasKey("SkinUsername", 8) && !StringUtils.isNullOrEmpty((String)nbttagcompound.getString("SkinUsername"))) {
                this.playerProfile = new GameProfile(null, nbttagcompound.getString("SkinUsername"));
            }
            this.loadProfile();
        }
        this.modelSize = ValueUtil.CorrectInt(nbttagcompound.getInteger("Size"), 1, 30);
        this.showName = nbttagcompound.getInteger("ShowName");
        if (nbttagcompound.hasKey("SkinColor")) {
            this.skinColor = nbttagcompound.getInteger("SkinColor");
        }
        this.visible = nbttagcompound.getInteger("NpcVisible");
        this.disableLivingAnimation = nbttagcompound.getBoolean("NoLivingAnimation");
        this.noHitbox = nbttagcompound.getBoolean("IsStatue");
        this.setBossbar(nbttagcompound.getByte("BossBar"));
        this.setBossColor(nbttagcompound.getInteger("BossColor"));
        if (!(prevSkinType == this.skinType && this.texture.equals(prevTexture) && this.url.equals(prevUrl) && this.getSkinPlayer().equals(prevPlayer))) {
            this.npc.textureLocation = null;
        }
        this.npc.textureGlowLocation = null;
        this.npc.textureCloakLocation = null;
        this.npc.updateHitbox();
    }

    public void loadProfile() {
        GameProfile gameprofile;
        if (!(this.playerProfile == null || StringUtils.isNullOrEmpty((String)this.playerProfile.getName()) || this.npc.getServer() == null || this.playerProfile.isComplete() && this.playerProfile.getProperties().containsKey((Object)"textures") || (gameprofile = this.npc.getServer().getPlayerProfileCache().getGameProfileForUsername(this.playerProfile.getName())) == null)) {
            Property property = (Property)Iterables.getFirst((Iterable)gameprofile.getProperties().get((Object)"textures"), null);
            if (property == null) {
                gameprofile = this.npc.getServer().getMinecraftSessionService().fillProfileProperties(gameprofile, true);
            }
            this.playerProfile = gameprofile;
        }
    }

    public boolean showName() {
        if (this.npc.isKilled()) {
            return false;
        }
        return this.showName == 0 || this.showName == 2 && this.npc.isAttacking();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        if (this.name.equals(name)) {
            return;
        }
        this.name = name;
        this.npc.bossInfo.setName(this.npc.getDisplayName());
        this.npc.updateClient = true;
    }

    @Override
    public int getShowName() {
        return this.showName;
    }

    @Override
    public void setShowName(int type) {
        if (type == this.showName) {
            return;
        }
        this.showName = ValueUtil.CorrectInt(type, 0, 2);
        this.npc.updateClient = true;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(String title) {
        if (this.title.equals(title)) {
            return;
        }
        this.title = title;
        this.npc.updateClient = true;
    }

    @Override
    public String getSkinUrl() {
        return this.url;
    }

    @Override
    public void setSkinUrl(String url) {
        if (this.url.equals(url)) {
            return;
        }
        this.url = url;
        this.skinType = url.isEmpty() ? (byte)0 : (byte)2;
        this.npc.updateClient = true;
    }

    @Override
    public String getSkinPlayer() {
        return this.playerProfile == null ? "" : this.playerProfile.getName();
    }

    @Override
    public void setSkinPlayer(String name) {
        if (name == null || name.isEmpty()) {
            this.playerProfile = null;
            this.skinType = 0;
        } else {
            this.playerProfile = new GameProfile(null, name);
            this.skinType = 1;
        }
        this.npc.updateClient = true;
    }

    @Override
    public String getSkinTexture() {
        return this.texture;
    }

    @Override
    public void setSkinTexture(String texture) {
        if (texture == null || this.texture.equals(texture)) {
            return;
        }
        this.texture = texture.toLowerCase();
        this.npc.textureLocation = null;
        this.skinType = 0;
        this.npc.updateClient = true;
    }

    @Override
    public String getOverlayTexture() {
        return this.glowTexture;
    }

    @Override
    public void setOverlayTexture(String texture) {
        if (this.glowTexture.equals(texture)) {
            return;
        }
        this.glowTexture = texture;
        this.npc.textureGlowLocation = null;
        this.npc.updateClient = true;
    }

    @Override
    public String getCapeTexture() {
        return this.cloakTexture;
    }

    @Override
    public void setCapeTexture(String texture) {
        if (this.cloakTexture.equals(texture)) {
            return;
        }
        this.cloakTexture = texture.toLowerCase();
        this.npc.textureCloakLocation = null;
        this.npc.updateClient = true;
    }

    @Override
    public boolean getHasLivingAnimation() {
        return !this.disableLivingAnimation;
    }

    @Override
    public void setHasLivingAnimation(boolean enabled) {
        this.disableLivingAnimation = !enabled;
        this.npc.updateClient = true;
    }

    @Override
    public int getBossbar() {
        return this.showBossBar;
    }

    @Override
    public void setBossbar(int type) {
        if (type == this.showBossBar) {
            return;
        }
        this.showBossBar = (byte)ValueUtil.CorrectInt(type, 0, 2);
        this.npc.bossInfo.setVisible(this.showBossBar == 1);
        this.npc.updateClient = true;
    }

    @Override
    public int getBossColor() {
        return this.bossColor.ordinal();
    }

    @Override
    public void setBossColor(int color) {
        if (color < 0 || color >= BossInfo.Color.values().length) {
            throw new CustomNPCsException("Invalid Boss Color: " + color, new Object[0]);
        }
        this.bossColor = BossInfo.Color.values()[color];
        this.npc.bossInfo.setColor(this.bossColor);
    }

    @Override
    public int getVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(int type) {
        if (type == this.visible) {
            return;
        }
        this.visible = ValueUtil.CorrectInt(type, 0, 2);
        this.npc.updateClient = true;
    }

    @Override
    public int getSize() {
        return this.modelSize;
    }

    @Override
    public void setSize(int size) {
        if (this.modelSize == size) {
            return;
        }
        this.modelSize = ValueUtil.CorrectInt(size, 1, 30);
        this.npc.updateClient = true;
    }

    @Override
    public void setModelScale(int part, float x, float y, float z) {
        ModelData modeldata = ((EntityCustomNpc)this.npc).modelData;
        ModelPartConfig model = null;
        if (part == 0) {
            model = modeldata.getPartConfig(EnumParts.HEAD);
        } else if (part == 1) {
            model = modeldata.getPartConfig(EnumParts.BODY);
        } else if (part == 2) {
            model = modeldata.getPartConfig(EnumParts.ARM_LEFT);
        } else if (part == 3) {
            model = modeldata.getPartConfig(EnumParts.ARM_RIGHT);
        } else if (part == 4) {
            model = modeldata.getPartConfig(EnumParts.LEG_LEFT);
        } else if (part == 5) {
            model = modeldata.getPartConfig(EnumParts.LEG_RIGHT);
        }
        if (model == null) {
            throw new CustomNPCsException("Unknown part: " + part, new Object[0]);
        }
        model.setScale(x, y, z);
        this.npc.updateClient = true;
    }

    @Override
    public float[] getModelScale(int part) {
        ModelData modeldata = ((EntityCustomNpc)this.npc).modelData;
        ModelPartConfig model = null;
        if (part == 0) {
            model = modeldata.getPartConfig(EnumParts.HEAD);
        } else if (part == 1) {
            model = modeldata.getPartConfig(EnumParts.BODY);
        } else if (part == 2) {
            model = modeldata.getPartConfig(EnumParts.ARM_LEFT);
        } else if (part == 3) {
            model = modeldata.getPartConfig(EnumParts.ARM_RIGHT);
        } else if (part == 4) {
            model = modeldata.getPartConfig(EnumParts.LEG_LEFT);
        } else if (part == 5) {
            model = modeldata.getPartConfig(EnumParts.LEG_RIGHT);
        }
        if (model == null) {
            throw new CustomNPCsException("Unknown part: " + part, new Object[0]);
        }
        return new float[]{model.scaleX, model.scaleY, model.scaleZ};
    }

    @Override
    public int getTint() {
        return this.skinColor;
    }

    @Override
    public void setTint(int color) {
        if (color == this.skinColor) {
            return;
        }
        this.skinColor = color;
        this.npc.updateClient = true;
    }

    @Override
    public void setModel(String id) {
        ModelData modeldata = ((EntityCustomNpc)this.npc).modelData;
        if (id == null) {
            if (modeldata.entityClass == null) {
                return;
            }
            modeldata.entityClass = null;
            this.npc.updateClient = true;
        } else {
            ResourceLocation resource = new ResourceLocation(id);
            Entity entity = EntityList.createEntityByIDFromName((ResourceLocation)resource, (World)this.npc.world);
            if (entity == null) {
                throw new CustomNPCsException("Failed to create an entity from given id: " + id, new Object[0]);
            }
            modeldata.setEntityName(entity.getClass().getCanonicalName());
            this.npc.updateClient = true;
        }
    }

    @Override
    public String getModel() {
        ModelData modeldata = ((EntityCustomNpc)this.npc).modelData;
        if (modeldata.entityClass == null) {
            return null;
        }
        String name = modeldata.entityClass.getCanonicalName();
        for (EntityEntry ent : ForgeRegistries.ENTITIES.getValues()) {
            Class c = ent.getEntityClass();
            if (!c.getCanonicalName().equals(name) || !EntityLivingBase.class.isAssignableFrom(c)) continue;
            return ent.getRegistryName().toString();
        }
        return null;
    }

    @Override
    public boolean getHasHitbox() {
        return !this.noHitbox;
    }

    @Override
    public void setHasHitbox(boolean bo) {
        if (this.noHitbox != bo) {
            return;
        }
        this.noHitbox = !bo;
        this.npc.updateClient = true;
    }
}

