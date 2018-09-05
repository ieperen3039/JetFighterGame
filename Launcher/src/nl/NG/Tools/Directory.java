package nl.NG.Tools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Geert van Ieperen created on 10-2-2018.
 */
public enum Directory {
    music(Paths.get("res", "music")),
    soundEffects(Paths.get("res", "sounds")),
    fonts(Paths.get("res", "fonts")),
    shaders(Paths.get("res", "shaders")),
    meshes(Paths.get("res", "models")),
    backdrops(Paths.get("res", "pictures")),
    recordings(Paths.get("Recordings")),
    screenShots(Paths.get("ScreenShots")),
    settings(Paths.get("res")),
    gameJar(Paths.get("jar")),
    launcher(Paths.get("jar")),
    tables(currentDirectory());

    private final Path directory;

    Directory(Path directory) {
        this.directory = directory;
    }

    public File getFile(String... path) {
        return currentDirectory()
                .resolve(getPath(path))
                .toFile();
    }

    public Path getPath(String... path) {
        Path dir = this.directory;
        for (String p : path) {
            dir = dir.resolve(p);
        }
        return dir;
    }

    public File[] getFiles() {
        return getFile("").listFiles();
    }

    public static Path currentDirectory() {
        return Paths.get("").toAbsolutePath();
    }

}
