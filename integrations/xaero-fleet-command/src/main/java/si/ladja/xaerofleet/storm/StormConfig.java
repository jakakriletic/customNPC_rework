package si.ladja.xaerofleet.storm;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class StormConfig {
    public static boolean enabled=true, allowAllPlayers=false, windPush=true;
    public static int coldDelayTicks=300;
    public static float coldDamage=2, windStrength=0.11f;
    private StormConfig() { }
    public static void load(File file) {
        Configuration c = new Configuration(file); c.load();
        enabled=c.getBoolean("enabled","winterStorms",true,"Enable local winter storms selected on the map.");
        allowAllPlayers=c.getBoolean("allowAllPlayers","winterStorms",false,"Allow non-operators to create/remove storms. Single-player owner can always manage storms.");
        windPush=c.getBoolean("windPush","winterStorms",true,"Wind gusts push exposed survival players. Sneaking reduces the force.");
        coldDelayTicks=c.getInt("coldDelaySeconds","winterStorms",15,1,120,"Outdoor exposure before freezing damage begins without armour.")*20;
        coldDamage=c.getFloat("coldDamage","winterStorms",2,0,20,"Damage every two seconds after cold builds up. Two damage = one heart. Armour reduces it; four pieces prevent it.");
        windStrength=c.getFloat("windStrength","winterStorms",0.11f,0,0.4f,"Horizontal gust impulse every half second.");
        if(c.hasChanged())c.save();
    }
}
