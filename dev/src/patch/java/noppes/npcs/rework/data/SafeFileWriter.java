package noppes.npcs.rework.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes a complete, validated replacement without exposing a partially written target file.
 * Temporary and backup files always live beside the target so a move cannot cross file systems.
 */
public final class SafeFileWriter {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private SafeFileWriter() {}

    public interface OutputAction {
        void write(OutputStream output) throws IOException;
    }

    public interface Validator {
        void validate(File candidate) throws IOException;
    }

    public static void writeUtf8(File target, final String text, Validator validator)
            throws IOException {
        write(target, new OutputAction() {
            @Override
            public void write(OutputStream output) throws IOException {
                output.write(text.getBytes(UTF_8));
            }
        }, validator);
    }

    public static void write(File target, OutputAction action, Validator validator)
            throws IOException {
        if (target == null || action == null || validator == null) {
            throw new NullPointerException("target, action and validator are required");
        }

        File absoluteTarget = target.getAbsoluteFile();
        File parentFile = absoluteTarget.getParentFile();
        if (parentFile == null) {
            throw new IOException("Target has no parent directory: " + target);
        }
        Path parent = parentFile.toPath();
        Files.createDirectories(parent);

        Path destination = absoluteTarget.toPath();
        Path backup = parent.resolve(absoluteTarget.getName() + ".bak");
        if (!Files.exists(destination) && Files.exists(backup)) {
            throw new IOException("Recovery backup exists while target is missing: " + backup);
        }

        Path temporary = Files.createTempFile(parent, absoluteTarget.getName() + ".", ".tmp");
        boolean installed = false;
        try {
            writeAndSync(temporary, action);
            validator.validate(temporary.toFile());

            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                installed = true;
            } catch (AtomicMoveNotSupportedException unsupported) {
                replaceWithBackup(temporary, destination, backup, validator);
                installed = true;
            }

            validator.validate(destination.toFile());
        } finally {
            if (!installed) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void writeAndSync(Path temporary, OutputAction action) throws IOException {
        FileOutputStream output = new FileOutputStream(temporary.toFile());
        try {
            action.write(output);
            output.flush();
            output.getFD().sync();
        } finally {
            output.close();
        }
    }

    private static void replaceWithBackup(Path temporary, Path destination, Path backup,
            Validator validator) throws IOException {
        boolean hadTarget = Files.exists(destination);
        if (hadTarget) {
            Files.move(destination, backup, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            validator.validate(destination.toFile());
            Files.deleteIfExists(backup);
        } catch (IOException failure) {
            if (hadTarget && Files.exists(backup)) {
                Files.deleteIfExists(destination);
                try {
                    Files.move(backup, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
    }
}
