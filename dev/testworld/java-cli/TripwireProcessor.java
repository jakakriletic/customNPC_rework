import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

public class TripwireProcessor extends AbstractProcessor {
    static {
        try { java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("rw.m67.marker")), new byte[] { 2 }); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    @Override public boolean process(Set<? extends TypeElement> types, RoundEnvironment round) { return false; }
}
