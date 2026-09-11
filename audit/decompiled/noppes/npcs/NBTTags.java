/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.Ingredient
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagDouble
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.NonNullList
 */
package noppes.npcs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;

public class NBTTags {
    public static void getItemStackList(NBTTagList tagList, NonNullList<ItemStack> items) {
        items.clear();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            try {
                items.set(nbttagcompound.func_74771_c("Slot") & 0xFF, (Object)new ItemStack(nbttagcompound));
                continue;
            }
            catch (ClassCastException e) {
                items.set(nbttagcompound.func_74762_e("Slot"), (Object)new ItemStack(nbttagcompound));
            }
        }
    }

    public static Map<Integer, IItemStack> getIItemStackMap(NBTTagList tagList) {
        HashMap<Integer, IItemStack> list = new HashMap<Integer, IItemStack>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            ItemStack item = new ItemStack(nbttagcompound);
            if (item.func_190926_b()) continue;
            try {
                list.put(nbttagcompound.func_74771_c("Slot") & 0xFF, NpcAPI.Instance().getIItemStack(item));
                continue;
            }
            catch (ClassCastException e) {
                list.put(nbttagcompound.func_74762_e("Slot"), NpcAPI.Instance().getIItemStack(item));
            }
        }
        return list;
    }

    public static ItemStack[] getItemStackArray(NBTTagList tagList) {
        ItemStack[] list = new ItemStack[tagList.func_74745_c()];
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list[nbttagcompound.func_74771_c((String)"Slot") & 0xFF] = new ItemStack(nbttagcompound);
        }
        return list;
    }

    public static NonNullList<Ingredient> getIngredientList(NBTTagList tagList) {
        NonNullList list = NonNullList.func_191196_a();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.add(nbttagcompound.func_74771_c("Slot") & 0xFF, (Object)Ingredient.func_193369_a((ItemStack[])new ItemStack[]{new ItemStack(nbttagcompound)}));
        }
        return list;
    }

    public static ArrayList<int[]> getIntegerArraySet(NBTTagList tagList) {
        ArrayList<int[]> set = new ArrayList<int[]>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound compound = tagList.func_150305_b(i);
            set.add(compound.func_74759_k("Array"));
        }
        return set;
    }

    public static HashMap<Integer, Boolean> getBooleanList(NBTTagList tagList) {
        HashMap<Integer, Boolean> list = new HashMap<Integer, Boolean>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.put(nbttagcompound.func_74762_e("Slot"), nbttagcompound.func_74767_n("Boolean"));
        }
        return list;
    }

    public static HashMap<Integer, Integer> getIntegerIntegerMap(NBTTagList tagList) {
        HashMap<Integer, Integer> list = new HashMap<Integer, Integer>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.put(nbttagcompound.func_74762_e("Slot"), nbttagcompound.func_74762_e("Integer"));
        }
        return list;
    }

    public static HashMap<Integer, Long> getIntegerLongMap(NBTTagList tagList) {
        HashMap<Integer, Long> list = new HashMap<Integer, Long>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.put(nbttagcompound.func_74762_e("Slot"), nbttagcompound.func_74763_f("Long"));
        }
        return list;
    }

    public static HashSet<Integer> getIntegerSet(NBTTagList tagList) {
        HashSet<Integer> list = new HashSet<Integer>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.add(nbttagcompound.func_74762_e("Integer"));
        }
        return list;
    }

    public static List<Integer> getIntegerList(NBTTagList tagList) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.add(nbttagcompound.func_74762_e("Integer"));
        }
        return list;
    }

    public static HashMap<String, String> getStringStringMap(NBTTagList tagList) {
        HashMap<String, String> list = new HashMap<String, String>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.put(nbttagcompound.func_74779_i("Slot"), nbttagcompound.func_74779_i("Value"));
        }
        return list;
    }

    public static HashMap<Integer, String> getIntegerStringMap(NBTTagList tagList) {
        HashMap<Integer, String> list = new HashMap<Integer, String>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.put(nbttagcompound.func_74762_e("Slot"), nbttagcompound.func_74779_i("Value"));
        }
        return list;
    }

    public static HashMap<String, Integer> getStringIntegerMap(NBTTagList tagList) {
        HashMap<String, Integer> list = new HashMap<String, Integer>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.put(nbttagcompound.func_74779_i("Slot"), nbttagcompound.func_74762_e("Value"));
        }
        return list;
    }

    public static HashMap<String, Vector<String>> getVectorMap(NBTTagList tagList) {
        HashMap<String, Vector<String>> map = new HashMap<String, Vector<String>>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            Vector<String> values = new Vector<String>();
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            NBTTagList list = nbttagcompound.func_150295_c("Values", 10);
            for (int j = 0; j < list.func_74745_c(); ++j) {
                NBTTagCompound value = list.func_150305_b(j);
                values.add(value.func_74779_i("Value"));
            }
            map.put(nbttagcompound.func_74779_i("Key"), values);
        }
        return map;
    }

    public static List<String> getStringList(NBTTagList tagList) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            String line = nbttagcompound.func_74779_i("Line");
            list.add(line);
        }
        return list;
    }

    public static String[] getStringArray(NBTTagList tagList, int size) {
        String[] arr = new String[size];
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            String line = nbttagcompound.func_74779_i("Value");
            int slot = nbttagcompound.func_74762_e("Slot");
            arr[slot] = line;
        }
        return arr;
    }

    public static NBTTagList nbtIntegerArraySet(List<int[]> set) {
        NBTTagList nbttaglist = new NBTTagList();
        if (set == null) {
            return nbttaglist;
        }
        for (int[] arr : set) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74783_a("Array", arr);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtItemStackList(NonNullList<ItemStack> inventory) {
        NBTTagList nbttaglist = new NBTTagList();
        for (int slot = 0; slot < inventory.size(); ++slot) {
            ItemStack item = (ItemStack)inventory.get(slot);
            if (item.func_190926_b()) continue;
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74774_a("Slot", (byte)slot);
            item.func_77955_b(nbttagcompound);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtIItemStackMap(Map<Integer, IItemStack> inventory) {
        NBTTagList nbttaglist = new NBTTagList();
        if (inventory == null) {
            return nbttaglist;
        }
        for (int slot : inventory.keySet()) {
            IItemStack item = inventory.get(slot);
            if (item == null) continue;
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74774_a("Slot", (byte)slot);
            item.getMCItemStack().func_77955_b(nbttagcompound);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtItemStackArray(ItemStack[] inventory) {
        NBTTagList nbttaglist = new NBTTagList();
        if (inventory == null) {
            return nbttaglist;
        }
        for (int slot = 0; slot < inventory.length; ++slot) {
            ItemStack item = inventory[slot];
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74774_a("Slot", (byte)slot);
            if (item != null) {
                item.func_77955_b(nbttagcompound);
            }
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtIngredientList(NonNullList<Ingredient> inventory) {
        NBTTagList nbttaglist = new NBTTagList();
        if (inventory == null) {
            return nbttaglist;
        }
        for (int slot = 0; slot < inventory.size(); ++slot) {
            Ingredient ingredient = (Ingredient)inventory.get(slot);
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74774_a("Slot", (byte)slot);
            if (ingredient != null && ingredient.func_193365_a().length > 0) {
                ingredient.func_193365_a()[0].func_77955_b(nbttagcompound);
            }
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtBooleanList(HashMap<Integer, Boolean> updatedSlots) {
        NBTTagList nbttaglist = new NBTTagList();
        if (updatedSlots == null) {
            return nbttaglist;
        }
        HashMap<Integer, Boolean> inventory2 = updatedSlots;
        for (Integer slot : inventory2.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Slot", slot.intValue());
            nbttagcompound.func_74757_a("Boolean", inventory2.get(slot).booleanValue());
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtIntegerIntegerMap(Map<Integer, Integer> lines) {
        NBTTagList nbttaglist = new NBTTagList();
        if (lines == null) {
            return nbttaglist;
        }
        for (int slot : lines.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Slot", slot);
            nbttagcompound.func_74768_a("Integer", lines.get(slot).intValue());
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtIntegerLongMap(HashMap<Integer, Long> lines) {
        NBTTagList nbttaglist = new NBTTagList();
        if (lines == null) {
            return nbttaglist;
        }
        for (int slot : lines.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Slot", slot);
            nbttagcompound.func_74772_a("Long", lines.get(slot).longValue());
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtIntegerCollection(Collection<Integer> set) {
        NBTTagList nbttaglist = new NBTTagList();
        if (set == null) {
            return nbttaglist;
        }
        for (int slot : set) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Integer", slot);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtVectorMap(HashMap<String, Vector<String>> map) {
        NBTTagList list = new NBTTagList();
        if (map == null) {
            return list;
        }
        for (String key : map.keySet()) {
            NBTTagCompound compound = new NBTTagCompound();
            compound.func_74778_a("Key", key);
            NBTTagList values = new NBTTagList();
            for (String value : map.get(key)) {
                NBTTagCompound comp = new NBTTagCompound();
                comp.func_74778_a("Value", value);
                values.func_74742_a((NBTBase)comp);
            }
            compound.func_74782_a("Values", (NBTBase)values);
            list.func_74742_a((NBTBase)compound);
        }
        return list;
    }

    public static NBTTagList nbtStringStringMap(HashMap<String, String> map) {
        NBTTagList nbttaglist = new NBTTagList();
        if (map == null) {
            return nbttaglist;
        }
        for (String slot : map.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74778_a("Slot", slot);
            nbttagcompound.func_74778_a("Value", map.get(slot));
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtStringIntegerMap(Map<String, Integer> map) {
        NBTTagList nbttaglist = new NBTTagList();
        if (map == null) {
            return nbttaglist;
        }
        for (String slot : map.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74778_a("Slot", slot);
            nbttagcompound.func_74768_a("Value", map.get(slot).intValue());
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTBase nbtIntegerStringMap(Map<Integer, String> map) {
        NBTTagList nbttaglist = new NBTTagList();
        if (map == null) {
            return nbttaglist;
        }
        for (int slot : map.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Slot", slot);
            nbttagcompound.func_74778_a("Value", map.get(slot));
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtStringArray(String[] list) {
        NBTTagList nbttaglist = new NBTTagList();
        if (list == null) {
            return nbttaglist;
        }
        for (int i = 0; i < list.length; ++i) {
            if (list[i] == null) continue;
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74778_a("Value", list[i]);
            nbttagcompound.func_74768_a("Slot", i);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtStringList(List<String> list) {
        NBTTagList nbttaglist = new NBTTagList();
        for (String s : list) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74778_a("Line", s);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }

    public static NBTTagList nbtDoubleList(double ... par1ArrayOfDouble) {
        NBTTagList nbttaglist = new NBTTagList();
        double[] adouble = par1ArrayOfDouble;
        int i = par1ArrayOfDouble.length;
        for (int j = 0; j < i; ++j) {
            double d1 = adouble[j];
            nbttaglist.func_74742_a((NBTBase)new NBTTagDouble(d1));
        }
        return nbttaglist;
    }

    public static NBTTagCompound NBTMerge(NBTTagCompound data, NBTTagCompound merge) {
        NBTTagCompound compound = data.func_74737_b();
        Set names = merge.func_150296_c();
        for (String name : names) {
            NBTBase base = merge.func_74781_a(name);
            if (base.func_74732_a() == 10) {
                base = NBTTags.NBTMerge(compound.func_74775_l(name), (NBTTagCompound)base);
            }
            compound.func_74782_a(name, base);
        }
        return compound;
    }

    public static List<ScriptContainer> GetScript(NBTTagList list, IScriptHandler handler) {
        ArrayList<ScriptContainer> scripts = new ArrayList<ScriptContainer>();
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound compoundd = list.func_150305_b(i);
            ScriptContainer script = new ScriptContainer(handler);
            script.readFromNBT(compoundd);
            scripts.add(script);
        }
        return scripts;
    }

    public static NBTTagList NBTScript(List<ScriptContainer> scripts) {
        NBTTagList list = new NBTTagList();
        for (ScriptContainer script : scripts) {
            NBTTagCompound compound = new NBTTagCompound();
            script.writeToNBT(compound);
            list.func_74742_a((NBTBase)compound);
        }
        return list;
    }

    public static TreeMap<Long, String> GetLongStringMap(NBTTagList tagList) {
        TreeMap<Long, String> list = new TreeMap<Long, String>();
        for (int i = 0; i < tagList.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = tagList.func_150305_b(i);
            list.put(nbttagcompound.func_74763_f("Long"), nbttagcompound.func_74779_i("String"));
        }
        return list;
    }

    public static NBTTagList NBTLongStringMap(Map<Long, String> map) {
        NBTTagList nbttaglist = new NBTTagList();
        if (map == null) {
            return nbttaglist;
        }
        for (long slot : map.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74772_a("Long", slot);
            nbttagcompound.func_74778_a("String", map.get(slot));
            nbttaglist.func_74742_a((NBTBase)nbttagcompound);
        }
        return nbttaglist;
    }
}

