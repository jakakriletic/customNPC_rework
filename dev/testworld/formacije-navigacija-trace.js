// Appended only by -TraceNavigator, after the unchanged recovery overlay.
// Read-only PathNavigate state: no method that changes a path, timer or entity.
// These snapshots can identify a timeout-reset signature, not a clearPath caller.
// D-075: a failed read is logged as FORM-NAVERR with the field name and marks the
// record navError=<field>; it must not disable the controller script (fail closed).
// D-076: Vec3d is read only through reflective Field objects. A direct value.x makes
// Nashorn introspect every Vec3d method; fromPitchYawVector(Vec2f) then fails on the
// dedicated server, where the @SideOnly(CLIENT) Vec2f class is stripped.
var formNavClass = Java.type('net.minecraft.pathfinding.PathNavigate').class;
var formNavClock = Java.type('java.lang.System');
var formNavOriginalRecoveryState = formTraceRecoveryState;
var formNavFailures = 0;
formTraceRecoveryState = function (squad, i, path) {
    var text = formNavOriginalRecoveryState(squad, i, path);
    var step = 'navigator';
    try {
        var nav = formTraceRead(squad, 'members')[i].getNavigator();
        var read = function (name) { step = name; return formTraceField(formNavClass, name, nav); };
        var xyz = function (value) {
            var vec = formNavClass.getDeclaredField('lastPosCheck').getType();
            return formTraceXYZ(formTraceField(vec, 'x', value), formTraceField(vec, 'y', value),
                formTraceField(vec, 'z', value));
        };
        var target = read('targetPos');
        step = 'format';
        return text + ' sampleMs=' + formNavClock.currentTimeMillis()
            + ' navClass=' + nav.getClass().getName()
            + ' navTicks=' + read('totalTicks') + ' navCheckTick=' + read('ticksAtLastPos')
            + ' navCheckPos=' + xyz(read('lastPosCheck'))
            + ' navTimeoutNode=' + xyz(read('timeoutCachedNode'))
            + ' navTimeoutMs=' + read('timeoutTimer')
            + ' navTimeoutLimit=' + formTraceNumber(read('timeoutLimit'))
            + ' navTimeoutLastMs=' + read('lastTimeoutCheck')
            + ' navUpdate=' + read('tryUpdatePath')
            + ' navUpdateWorldTick=' + read('lastTimeUpdated')
            + ' navTarget=' + (target ? formTraceXYZ(target.getX(), target.getY(), target.getZ()) : 'none');
    } catch (error) {
        formNavFailures++;
        if (formNavFailures <= 3) {
            formTraceLog('FORM-NAVERR step=' + step + ' count=' + formNavFailures
                + ' error=' + String(error).replace(/\s+/g, '_'));
        }
        return text + ' navError=' + step;
    }
};
