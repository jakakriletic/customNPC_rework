package noppes.npcs.rework.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.util.NBTJsonUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Deterministic fault injection for every destructive boundary in SafeFileWriter. */
public class SafeFileWriterFaultInjectionTest {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private File directory;
    private File target;

    @Before
    public void createTarget() throws Exception {
        directory = Files.createTempDirectory("safe-writer-fault-").toFile();
        target = new File(directory, "data.json");
        Files.write(target.toPath(), "old".getBytes(UTF_8));
    }

    @After
    public void removeDirectory() throws Exception {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                Files.deleteIfExists(file.toPath());
            }
        }
        Files.deleteIfExists(directory.toPath());
    }

    @Test
    public void interruptedWriteBeforeReplacementPreservesTarget() throws Exception {
        try {
            SafeFileWriter.write(target, new SafeFileWriter.OutputAction() {
                @Override
                public void write(OutputStream output) throws IOException {
                    output.write("partial".getBytes(UTF_8));
                    throw new InterruptedIOException("injected interruption before replacement");
                }
            }, exactText("new"), new InjectingFiles(target.toPath()));
            fail("Expected interrupted write");
        } catch (InterruptedIOException expected) {
            assertEquals("old", read(target));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void lockedOrDeniedAtomicTargetPreservesTarget() throws Exception {
        InjectingFiles files = new InjectingFiles(target.toPath());
        files.denyAtomicMove = true;
        try {
            write("new", exactText("new"), files);
            fail("Expected access denial");
        } catch (AccessDeniedException expected) {
            assertEquals("old", read(target));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void deniedDirectoryAccessPreservesTarget() throws Exception {
        InjectingFiles files = new InjectingFiles(target.toPath());
        files.denyDirectoryAccess = true;
        try {
            write("new", exactText("new"), files);
            fail("Expected directory access denial");
        } catch (AccessDeniedException expected) {
            assertEquals("old", read(target));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void unsupportedAtomicMoveUsesVerifiedFallback() throws Exception {
        InjectingFiles files = new InjectingFiles(target.toPath());
        files.atomicUnsupported = true;

        write("new", exactText("new"), files);

        assertEquals("new", read(target));
        assertFalse(new File(directory, "data.json.bak").exists());
        assertNoTemporaryFiles();
    }

    @Test
    public void failedFallbackInstallRestoresPreviousTarget() throws Exception {
        InjectingFiles files = new InjectingFiles(target.toPath());
        files.atomicUnsupported = true;
        files.failInstall = true;
        try {
            write("new", exactText("new"), files);
            fail("Expected installation failure");
        } catch (IOException expected) {
            assertEquals("old", read(target));
            assertFalse(new File(directory, "data.json.bak").exists());
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void interruptionAfterFallbackReplacementRestoresPreviousTarget() throws Exception {
        InjectingFiles files = new InjectingFiles(target.toPath());
        files.atomicUnsupported = true;
        SafeFileWriter.Validator interruptedAfterInstall = new SafeFileWriter.Validator() {
            @Override
            public void validate(File candidate) throws IOException {
                if (candidate.getAbsoluteFile().equals(target.getAbsoluteFile())) {
                    throw new InterruptedIOException("injected interruption after replacement");
                }
                exactText("new").validate(candidate);
            }
        };
        try {
            write("new", interruptedAfterInstall, files);
            fail("Expected post-install interruption");
        } catch (InterruptedIOException expected) {
            assertEquals("old", read(target));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void failedRestoreLeavesRecoverableBackup() throws Exception {
        InjectingFiles files = new InjectingFiles(target.toPath());
        files.atomicUnsupported = true;
        files.failInstall = true;
        files.failRestore = true;
        try {
            write("new", exactText("new"), files);
            fail("Expected installation and restore failure");
        } catch (IOException expected) {
            assertFalse(target.exists());
            assertEquals("old", read(new File(directory, "data.json.bak")));
            assertEquals(1, expected.getSuppressed().length);
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void corruptJsonCandidateCannotReplaceValidJson() throws Exception {
        NBTTagCompound old = new NBTTagCompound();
        old.setString("value", "old");
        NBTJsonUtil.SaveFile(target, old);
        SafeFileWriter.Validator readableJson = new SafeFileWriter.Validator() {
            @Override
            public void validate(File candidate) throws IOException {
                try {
                    NBTJsonUtil.LoadFile(candidate);
                } catch (Exception invalid) {
                    throw new IOException("invalid JSON candidate", invalid);
                }
            }
        };

        try {
            write("{broken", readableJson, new InjectingFiles(target.toPath()));
            fail("Expected corrupt JSON rejection");
        } catch (IOException expected) {
            assertEquals(old, NBTJsonUtil.LoadFile(target));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void restartWithOrphanedBackupLeavesLastVersionRecoverable() throws Exception {
        Files.delete(target.toPath());
        File backup = new File(directory, "data.json.bak");
        Files.write(backup.toPath(), "last-valid".getBytes(UTF_8));

        try {
            write("new", exactText("new"), new InjectingFiles(target.toPath()));
            fail("Expected recovery guard");
        } catch (IOException expected) {
            assertFalse(target.exists());
            assertEquals("last-valid", read(backup));
            assertNoTemporaryFiles();
        }
    }

    private void write(final String text, SafeFileWriter.Validator validator,
            SafeFileWriter.FileOperations files) throws IOException {
        SafeFileWriter.write(target, new SafeFileWriter.OutputAction() {
            @Override
            public void write(OutputStream output) throws IOException {
                output.write(text.getBytes(UTF_8));
            }
        }, validator, files);
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

    private static final class InjectingFiles implements SafeFileWriter.FileOperations {
        private final Path destination;
        private final Path backup;
        boolean denyAtomicMove;
        boolean denyDirectoryAccess;
        boolean atomicUnsupported;
        boolean failInstall;
        boolean failRestore;

        InjectingFiles(Path destination) {
            this.destination = destination.toAbsolutePath();
            this.backup = destination.resolveSibling(destination.getFileName() + ".bak")
                    .toAbsolutePath();
        }

        @Override
        public boolean exists(Path path) {
            return Files.exists(path);
        }

        @Override
        public void createDirectories(Path path) throws IOException {
            if (denyDirectoryAccess) {
                throw new AccessDeniedException(path.toString(), null,
                        "injected directory access denial");
            }
            Files.createDirectories(path);
        }

        @Override
        public Path createTempFile(Path directory, String prefix, String suffix)
                throws IOException {
            return Files.createTempFile(directory, prefix, suffix);
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            boolean atomic = false;
            for (CopyOption option : options) {
                atomic |= option == StandardCopyOption.ATOMIC_MOVE;
            }
            if (atomic && denyAtomicMove) {
                throw new AccessDeniedException(target.toString(), null, "injected lock/denial");
            }
            if (atomic && atomicUnsupported) {
                throw new AtomicMoveNotSupportedException(source.toString(), target.toString(),
                        "injected unsupported atomic move");
            }
            Path absoluteSource = source.toAbsolutePath();
            Path absoluteTarget = target.toAbsolutePath();
            if (failInstall && absoluteTarget.equals(destination)
                    && !absoluteSource.equals(backup)) {
                failInstall = false;
                throw new IOException("injected fallback install failure");
            }
            if (failRestore && absoluteSource.equals(backup)
                    && absoluteTarget.equals(destination)) {
                throw new IOException("injected restore failure");
            }
            Files.move(source, target, options);
        }

        @Override
        public void deleteIfExists(Path path) throws IOException {
            Files.deleteIfExists(path);
        }
    }
}
