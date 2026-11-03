package si.ladja.xaerofleet.storm;

public final class ColdModel {
    private ColdModel() { }
    public static int advance(int cold,int ticks,float severity,int armourPieces,boolean sheltered,boolean heat,int threshold) {
        int cap = Math.max(20,threshold)*2;
        if (sheltered || heat || armourPieces >= 4 || severity <= 0) return Math.max(0,cold-ticks*3);
        float protection = Math.max(0,Math.min(4,armourPieces))/4f;
        return Math.min(cap,Math.max(0,cold)+Math.max(1,Math.round(ticks*severity*(1-protection))));
    }
    public static float damage(int cold,int threshold,int armourPieces,float baseDamage) {
        return cold < threshold ? 0 : Math.max(0,baseDamage*(1-Math.max(0,Math.min(4,armourPieces))/4f));
    }
}
