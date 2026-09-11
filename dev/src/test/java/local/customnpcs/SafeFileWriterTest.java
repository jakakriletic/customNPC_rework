package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import noppes.npcs.rework.data.SafeFileWriter;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class SafeFileWriterTest {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private File directory;

    @Before
    public void createDirectory() throws Exception {
        try {
            Class.forName("noppes.npcs.rework.data.SafeFileWriter");
        } catch (ClassNotFoundException originalBuild) {
            Assume.assumeNoException(originalBuild);
        }
        directory = Files.createTempDirectory("cnpc-safe-writer-").toFile();
    }

    @After
    public void removeDirectory() throws Exception {
        if (directory != null) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    Files.deleteIfExists(file.toPath());
                }
            }
            Files.deleteIfExists(directory.toPath());
        }
    }

    @Test
    public void createsAndValidatesAFile() throws Exception {
        File target = new File(directory, "clone.json");
        SafeFileWriter.writeUtf8(target, "new", exactText("new"));
        assertEquals("new", read(target));
        assertNoTemporaryFiles();
    }

    @Test
    public void atomicallyReplacesAnExistingFile() throws Exception {
        File target = new File(directory, "clone.json");
        Files.write(target.toPath(), "old".getBytes(UTF_8));
        SafeFileWriter.writeUtf8(target, "new", exactText("new"));
        assertEquals("new", read(target));
        assertFalse(new File(directory, "clone.json.bak").exists());
        assertNoTemporaryFiles();
    }

    @Test
    public void failedValidationPreservesThePreviousFile() throws Exception {
        File target = new File(directory, "clone.json");
        Files.write(target.toPath(), "old".getBytes(UTF_8));
        try {
            SafeFileWriter.writeUtf8(target, "bad", exactText("expected"));
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("validation"));
        }
        assertEquals("old", read(target));
        assertNoTemporaryFiles();
    }

    @Test
    public void refusesToOverwriteAnOrphanedRecoveryBackup() throws Exception {
        File target = new File(directory, "clone.json");
        File backup = new File(directory, "clone.json.bak");
        Files.write(backup.toPath(), "recoverable".getBytes(UTF_8));
        try {
            SafeFileWriter.writeUtf8(target, "new", exactText("new"));
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Recovery backup"));
        }
        assertFalse(target.exists());
        assertEquals("recoverable", read(backup));
    }

    private SafeFileWriter.Validator exactText(final String expected) {
        return new SafeFileWriter.Validator() {
            @Override
            public void validate(File candidate) throws IOException {
                if (!expected.equals(read(candidate))) {
                    throw new IOException("validation rejected candidate");
                }
            }
        };
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), UTF_8);
    }

    private void assertNoTemporaryFiles() {
        File[] leftovers = directory.listFiles((dir, name) -> name.endsWith(".tmp"));
        assertTrue(leftovers == null || leftovers.length == 0);
    }
}
