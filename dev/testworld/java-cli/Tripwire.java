package corpus;
public class Tripwire {
    public static void touch() {
        try { java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("rw.m67.marker")), new byte[] { 1 }); }
        catch (Exception e) { throw new AssertionError(e); }
    }
}
