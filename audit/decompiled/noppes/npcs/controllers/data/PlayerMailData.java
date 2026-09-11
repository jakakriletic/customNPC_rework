/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.ArrayList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.controllers.data.PlayerMail;

public class PlayerMailData {
    public ArrayList<PlayerMail> playermail = new ArrayList();

    public void loadNBTData(NBTTagCompound compound) {
        ArrayList<PlayerMail> newmail = new ArrayList<PlayerMail>();
        NBTTagList list = compound.func_150295_c("MailData", 10);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.func_74745_c(); ++i) {
            PlayerMail mail = new PlayerMail();
            mail.readNBT(list.func_150305_b(i));
            newmail.add(mail);
        }
        this.playermail = newmail;
    }

    public NBTTagCompound saveNBTData(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (PlayerMail mail : this.playermail) {
            list.func_74742_a((NBTBase)mail.writeNBT());
        }
        compound.func_74782_a("MailData", (NBTBase)list);
        return compound;
    }

    public boolean hasMail() {
        for (PlayerMail mail : this.playermail) {
            if (mail.beenRead) continue;
            return true;
        }
        return false;
    }
}

