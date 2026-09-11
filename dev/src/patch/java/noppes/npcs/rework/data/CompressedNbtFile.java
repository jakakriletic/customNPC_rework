package noppes.npcs.rework.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

/** Writes and validates gzip-compressed NBT through the crash-safe replacement path. */
public final class CompressedNbtFile {
    private CompressedNbtFile() {}

    public static void save(File target, NBTTagCompound compound) throws IOException {
        final NBTTagCompound expected = compound.copy();
        SafeFileWriter.write(target, new SafeFileWriter.OutputAction() {
            @Override
            public void write(OutputStream output) throws IOException {
                // CompressedStreamTools closes its argument. Keep the underlying file stream
                // open so SafeFileWriter can flush and fsync it after compression completes.
                OutputStream nonClosing = new FilterOutputStream(output) {
                    @Override
                    public void close() throws IOException {
                        flush();
                    }
                };
                CompressedStreamTools.writeCompressed(expected, nonClosing);
            }
        }, new SafeFileWriter.Validator() {
            @Override
            public void validate(File candidate) throws IOException {
                InputStream input = new FileInputStream(candidate);
                NBTTagCompound actual;
                try {
                    actual = CompressedStreamTools.readCompressed(input);
                } finally {
                    input.close();
                }
                if (!expected.equals(actual)) {
                    throw new IOException("Compressed NBT verification failed for " + candidate);
                }
            }
        });
    }
}
