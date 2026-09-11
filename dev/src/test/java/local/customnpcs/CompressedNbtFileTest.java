package local.customnpcs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.rework.data.CompressedNbtFile;

import org.junit.Test;

public class CompressedNbtFileTest {
    @Test
    public void createsAndReplacesAValidatedCompressedNbtFile() throws Exception {
        File directory = Files.createTempDirectory("compressed-nbt-test").toFile();
        File target = new File(directory, "data.dat");

        NBTTagCompound first = new NBTTagCompound();
        first.setString("value", "first");
        CompressedNbtFile.save(target, first);

        NBTTagCompound second = new NBTTagCompound();
        second.setString("value", "second");
        second.setInteger("count", 42);
        CompressedNbtFile.save(target, second);

        InputStream input = new FileInputStream(target);
        try {
            assertEquals(second, CompressedStreamTools.readCompressed(input));
        } finally {
            input.close();
        }
    }
}
