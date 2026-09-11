/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.resources.I18n
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.util.ChatAllowedCharacters
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  org.lwjgl.input.Keyboard
 */
package noppes.npcs.client.gui.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiButtonNextPage;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiClose;
import noppes.npcs.client.gui.util.IGuiError;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.containers.ContainerMail;
import noppes.npcs.controllers.data.PlayerMail;
import org.lwjgl.input.Keyboard;

@SideOnly(value=Side.CLIENT)
public class GuiMailmanWrite
extends GuiContainerNPCInterface
implements ITextfieldListener,
IGuiError,
IGuiClose,
GuiYesNoCallback {
    private static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
    private static final ResourceLocation bookWidgets = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation bookInventory = new ResourceLocation("textures/gui/container/inventory.png");
    private int updateCount;
    private int bookImageWidth = 192;
    private int bookImageHeight = 192;
    private int bookTotalPages = 1;
    private int currPage;
    private NBTTagList bookPages;
    private GuiButtonNextPage buttonNextPage;
    private GuiButtonNextPage buttonPreviousPage;
    private boolean canEdit;
    private boolean canSend;
    private boolean hasSend = false;
    public static GuiScreen parent;
    public static PlayerMail mail;
    private Minecraft mc = Minecraft.func_71410_x();
    private String username = "";
    private GuiNpcLabel error;

    public GuiMailmanWrite(ContainerMail container, boolean canEdit, boolean canSend) {
        super(null, container);
        this.title = "";
        this.canEdit = canEdit;
        this.canSend = canSend;
        if (GuiMailmanWrite.mail.message.func_74764_b("pages")) {
            this.bookPages = GuiMailmanWrite.mail.message.func_150295_c("pages", 8);
        }
        if (this.bookPages != null) {
            this.bookPages = this.bookPages.func_74737_b();
            this.bookTotalPages = this.bookPages.func_74745_c();
            if (this.bookTotalPages < 1) {
                this.bookTotalPages = 1;
            }
        } else {
            this.bookPages = new NBTTagList();
            this.bookPages.func_74742_a((NBTBase)new NBTTagString(""));
            this.bookTotalPages = 1;
        }
        this.field_146999_f = 360;
        this.field_147000_g = 260;
        this.drawDefaultBackground = false;
        this.closeOnEsc = true;
    }

    @Override
    public void func_73876_c() {
        super.func_73876_c();
        ++this.updateCount;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.field_146292_n.clear();
        Keyboard.enableRepeatEvents((boolean)true);
        if (this.canEdit && !this.canSend) {
            this.addLabel(new GuiNpcLabel(0, "mailbox.sender", this.field_147003_i + 170, this.field_147009_r + 32, 0));
        } else {
            this.addLabel(new GuiNpcLabel(0, "mailbox.username", this.field_147003_i + 170, this.field_147009_r + 32, 0));
        }
        if (this.canEdit && !this.canSend) {
            this.addTextField(new GuiNpcTextField(2, (GuiScreen)this, this.field_146289_q, this.field_147003_i + 170, this.field_147009_r + 42, 114, 20, GuiMailmanWrite.mail.sender));
        } else if (this.canEdit) {
            this.addTextField(new GuiNpcTextField(0, (GuiScreen)this, this.field_146289_q, this.field_147003_i + 170, this.field_147009_r + 42, 114, 20, this.username));
        } else {
            this.addLabel(new GuiNpcLabel(10, GuiMailmanWrite.mail.sender, this.field_147003_i + 170, this.field_147009_r + 42, 0));
        }
        this.addLabel(new GuiNpcLabel(1, "mailbox.subject", this.field_147003_i + 170, this.field_147009_r + 72, 0));
        if (this.canEdit) {
            this.addTextField(new GuiNpcTextField(1, (GuiScreen)this, this.field_146289_q, this.field_147003_i + 170, this.field_147009_r + 82, 114, 20, GuiMailmanWrite.mail.subject));
        } else {
            this.addLabel(new GuiNpcLabel(11, GuiMailmanWrite.mail.subject, this.field_147003_i + 170, this.field_147009_r + 82, 0));
        }
        this.error = new GuiNpcLabel(2, "", this.field_147003_i + 170, this.field_147009_r + 114, 0xFF0000);
        this.addLabel(this.error);
        if (this.canEdit && !this.canSend) {
            this.addButton(new GuiNpcButton(0, this.field_147003_i + 200, this.field_147009_r + 171, 60, 20, "gui.done"));
        } else if (this.canEdit) {
            this.addButton(new GuiNpcButton(0, this.field_147003_i + 200, this.field_147009_r + 171, 60, 20, "mailbox.send"));
        }
        if (!this.canEdit && !this.canSend) {
            this.addButton(new GuiNpcButton(4, this.field_147003_i + 200, this.field_147009_r + 171, 60, 20, "selectWorld.deleteButton"));
        }
        if (!this.canEdit || this.canSend) {
            this.addButton(new GuiNpcButton(3, this.field_147003_i + 200, this.field_147009_r + 194, 60, 20, "gui.cancel"));
        }
        this.buttonNextPage = new GuiButtonNextPage(1, this.field_147003_i + 120, this.field_147009_r + 156, true);
        this.field_146292_n.add(this.buttonNextPage);
        this.buttonPreviousPage = new GuiButtonNextPage(2, this.field_147003_i + 38, this.field_147009_r + 156, false);
        this.field_146292_n.add(this.buttonPreviousPage);
        this.updateButtons();
    }

    public void func_146281_b() {
        Keyboard.enableRepeatEvents((boolean)false);
    }

    private void updateButtons() {
        this.buttonNextPage.setVisible(this.currPage < this.bookTotalPages - 1 || this.canEdit);
        this.buttonPreviousPage.setVisible(this.currPage > 0);
    }

    public void func_73878_a(boolean flag, int i) {
        if (flag) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.MailDelete, GuiMailmanWrite.mail.time, GuiMailmanWrite.mail.sender);
            this.close();
        } else {
            NoppesUtil.openGUI((EntityPlayer)this.player, this);
        }
    }

    @Override
    protected void func_146284_a(GuiButton par1GuiButton) {
        if (par1GuiButton.field_146124_l) {
            int id = par1GuiButton.field_146127_k;
            if (id == 0) {
                GuiMailmanWrite.mail.message.func_74782_a("pages", (NBTBase)this.bookPages);
                if (this.canSend) {
                    if (!this.hasSend) {
                        this.hasSend = true;
                        NoppesUtilPlayer.sendData(EnumPlayerPacket.MailSend, this.username, mail.writeNBT());
                    }
                } else {
                    this.close();
                }
            }
            if (id == 3) {
                this.close();
            }
            if (id == 4) {
                GuiYesNo guiyesno = new GuiYesNo((GuiYesNoCallback)this, "", I18n.func_74838_a((String)"gui.deleteMessage"), 0);
                this.displayGuiScreen((GuiScreen)guiyesno);
            } else if (id == 1) {
                if (this.currPage < this.bookTotalPages - 1) {
                    ++this.currPage;
                } else if (this.canEdit) {
                    this.addNewPage();
                    if (this.currPage < this.bookTotalPages - 1) {
                        ++this.currPage;
                    }
                }
            } else if (id == 2 && this.currPage > 0) {
                --this.currPage;
            }
            this.updateButtons();
        }
    }

    private void addNewPage() {
        if (this.bookPages != null && this.bookPages.func_74745_c() < 50) {
            this.bookPages.func_74742_a((NBTBase)new NBTTagString(""));
            ++this.bookTotalPages;
        }
    }

    @Override
    public void func_73869_a(char par1, int par2) {
        if (!GuiNpcTextField.isActive() && this.canEdit) {
            this.keyTypedInBook(par1, par2);
        } else {
            super.func_73869_a(par1, par2);
        }
    }

    private void keyTypedInBook(char par1, int par2) {
        switch (par1) {
            case '\u0016': {
                this.func_74160_b(GuiScreen.func_146277_j());
                return;
            }
        }
        switch (par2) {
            case 14: {
                String s = this.func_74158_i();
                if (s.length() > 0) {
                    this.func_74159_a(s.substring(0, s.length() - 1));
                }
                return;
            }
            case 28: 
            case 156: {
                this.func_74160_b("\n");
                return;
            }
        }
        if (ChatAllowedCharacters.func_71566_a((char)par1)) {
            this.func_74160_b(Character.toString(par1));
        }
    }

    private String func_74158_i() {
        if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.func_74745_c()) {
            return this.bookPages.func_150307_f(this.currPage);
        }
        return "";
    }

    private void func_74159_a(String par1Str) {
        if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.func_74745_c()) {
            this.bookPages.func_150304_a(this.currPage, (NBTBase)new NBTTagString(par1Str));
        }
    }

    private void func_74160_b(String par1Str) {
        String s1 = this.func_74158_i();
        String s2 = s1 + par1Str;
        int i = this.mc.field_71466_p.func_78267_b(s2 + "" + TextFormatting.BLACK + "_", 118);
        if (i <= 118 && s2.length() < 256) {
            this.func_74159_a(s2);
        }
    }

    @Override
    public void func_73863_a(int par1, int par2, float par3) {
        this.func_146270_b(0);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.func_110434_K().func_110577_a(bookGuiTextures);
        this.func_73729_b(this.field_147003_i + 130, this.field_147009_r + 22, 0, 0, this.bookImageWidth, this.bookImageHeight / 3);
        this.func_73729_b(this.field_147003_i + 130, this.field_147009_r + 22 + this.bookImageHeight / 3, 0, this.bookImageHeight / 2, this.bookImageWidth, this.bookImageHeight / 2);
        this.func_73729_b(this.field_147003_i, this.field_147009_r + 2, 0, 0, this.bookImageWidth, this.bookImageHeight);
        this.mc.func_110434_K().func_110577_a(bookInventory);
        this.func_73729_b(this.field_147003_i + 20, this.field_147009_r + 173, 0, 82, 180, 55);
        this.func_73729_b(this.field_147003_i + 20, this.field_147009_r + 228, 0, 140, 180, 28);
        String s = net.minecraft.client.resources.I18n.func_135052_a((String)"book.pageIndicator", (Object[])new Object[]{this.currPage + 1, this.bookTotalPages});
        String s1 = "";
        if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.func_74745_c()) {
            s1 = this.bookPages.func_150307_f(this.currPage);
        }
        if (this.canEdit) {
            s1 = this.mc.field_71466_p.func_78260_a() ? s1 + "_" : (this.updateCount / 6 % 2 == 0 ? s1 + "" + TextFormatting.BLACK + "_" : s1 + "" + TextFormatting.GRAY + "_");
        }
        int l = this.mc.field_71466_p.func_78256_a(s);
        this.mc.field_71466_p.func_78276_b(s, this.field_147003_i - l + this.bookImageWidth - 44, this.field_147009_r + 18, 0);
        this.mc.field_71466_p.func_78279_b(s1, this.field_147003_i + 36, this.field_147009_r + 18 + 16, 116, 0);
        this.func_73733_a(this.field_147003_i + 175, this.field_147009_r + 136, this.field_147003_i + 269, this.field_147009_r + 154, -1072689136, -804253680);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.func_110434_K().func_110577_a(bookWidgets);
        for (int i = 0; i < 4; ++i) {
            this.func_73729_b(this.field_147003_i + 175 + i * 24, this.field_147009_r + 134, 0, 22, 24, 24);
        }
        super.func_73863_a(par1, par2, par3);
    }

    @Override
    public void close() {
        this.mc.func_147108_a(parent);
        parent = null;
        mail = new PlayerMail();
    }

    @Override
    public void unFocused(GuiNpcTextField textField) {
        if (textField.field_175208_g == 0) {
            this.username = textField.func_146179_b();
        }
        if (textField.field_175208_g == 1) {
            GuiMailmanWrite.mail.subject = textField.func_146179_b();
        }
        if (textField.field_175208_g == 2) {
            GuiMailmanWrite.mail.sender = textField.func_146179_b();
        }
    }

    @Override
    public void setError(int i, NBTTagCompound data) {
        if (i == 0) {
            this.error.label = I18n.func_74838_a((String)"mailbox.errorUsername");
        }
        if (i == 1) {
            this.error.label = I18n.func_74838_a((String)"mailbox.errorSubject");
        }
        this.hasSend = false;
    }

    @Override
    public void setClose(int i, NBTTagCompound data) {
        this.player.func_145747_a((ITextComponent)new TextComponentTranslation("mailbox.succes", new Object[]{data.func_74779_i("username")}));
    }

    @Override
    public void save() {
    }

    static {
        mail = new PlayerMail();
    }
}

