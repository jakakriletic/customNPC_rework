package noppes.npcs.rework.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
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

    interface FileOperations {
        boolean exists(Path path);
        void createDirectories(Path path) throws IOException;
        Path createTempFile(Path directory, String prefix, String suffix) throws IOException;
        void move(Path source, Path target, CopyOption... options) throws IOException;
        void deleteIfExists(Path path) throws IOException;
    }

    private static final FileOperations REAL_FILES = new FileOperations() {
        @Override
        public boolean exists(Path path) {
            return Files.exists(path);
        }

        @Override
        public void createDirectories(Path path) throws IOException {
            Files.createDirectories(path);
        }

        @Override
        public Path createTempFile(Path directory, String prefix, String suffix)
                throws IOException {
            return Files.createTempFile(directory, prefix, suffix);
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            Files.move(source, target, options);
        }

        @Override
        public void deleteIfExists(Path path) throws IOException {
            Files.deleteIfExists(path);
        }
    };

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
        write(target, action, validator, REAL_FILES);
    }

    static void write(File target, OutputAction action, Validator validator,
            FileOperations files) throws IOException {
        if (target == null || action == null || validator == null) {
            throw new NullPointerException("target, action and validator are required");
        }

        File absoluteTarget = target.getAbsoluteFile();
        File parentFile = absoluteTarget.getParentFile();
        if (parentFile == null) {
            throw new IOException("Target has no parent directory: " + target);
        }
        Path parent = parentFile.toPath();
        files.createDirectories(parent);

        Path destination = absoluteTarget.toPath();
        Path backup = parent.resolve(absoluteTarget.getName() + ".bak");
        if (!files.exists(destination) && files.exists(backup)) {
            throw new IOException("Recovery backup exists while target is missing: " + backup);
        }

        Path temporary = files.createTempFile(parent, absoluteTarget.getName() + ".", ".tmp");
        boolean installed = false;
        try {
            writeAndSync(temporary, action);
            validator.validate(temporary.toFile());

            try {
                files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                installed = true;
            } catch (AtomicMoveNotSupportedException unsupported) {
                replaceWithBackup(temporary, destination, backup, validator, files);
                installed = true;
            }

            validator.validate(destination.toFile());
        } finally {
            if (!installed) {
                files.deleteIfExists(temporary);
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
            Validator validator, FileOperations files) throws IOException {
        boolean hadTarget = files.exists(destination);
        if (hadTarget) {
            files.move(destination, backup, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            validator.validate(destination.toFile());
            files.deleteIfExists(backup);
        } catch (IOException failure) {
            if (hadTarget && files.exists(backup)) {
                files.deleteIfExists(destination);
                try {
                    files.move(backup, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
    }
}
