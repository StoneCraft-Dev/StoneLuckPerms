package me.lucko.luckperms.forge.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public enum FMLPaths {
    GAMEDIR(), MODSDIR("mods"), CONFIGDIR("config"), FMLCONFIG(false, CONFIGDIR, "fml.toml");
    private final Path relativePath;
    private final boolean isDirectory;
    private Path absolutePath;

    FMLPaths() {
        this("");
    }

    FMLPaths(final String... path) {
        this.relativePath = this.computePath(path);
        this.isDirectory = true;
    }

    FMLPaths(final boolean isDir, final FMLPaths parent, final String... path) {
        this.relativePath = parent.relativePath.resolve(this.computePath(path));
        this.isDirectory = isDir;
    }

    public static void loadAbsolutePaths() {
        for (final FMLPaths path : FMLPaths.values()) {
            path.absolutePath = path.relativePath.toAbsolutePath().normalize();

            if (path.isDirectory && !Files.isDirectory(path.absolutePath)) {
                try {
                    Files.createDirectories(path.absolutePath);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static Path getOrCreateGameRelativePath(final Path path) {
        final Path gameFolderPath = FMLPaths.GAMEDIR.get().resolve(path);

        if (!Files.isDirectory(gameFolderPath)) {
            try {
                Files.createDirectories(gameFolderPath);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return gameFolderPath;
    }

    private Path computePath(final String... path) {
        return Paths.get(path[0], Arrays.copyOfRange(path, 1, path.length));
    }

    public Path relative() {
        return this.relativePath;
    }

    public Path get() {
        if (this.absolutePath == null) {
            loadAbsolutePaths();
        }

        return this.absolutePath;
    }
}
