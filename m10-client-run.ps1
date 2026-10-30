# M10.1d. Controller owns this foreground scenario and all child JVMs.
# Dev MCP client/server (explicitly not packaged-client Q22 proof).
# Same independent diagnostic mod in all cases, never a rework harness in O.
param([ValidateSet('integrated','dedicated','original-client','original-server')][string]$Case='integrated')
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
$stamp=Get-Date -Format 'yyyy-MM-dd-HHmmss'
$base=Join-Path $root "audit/m101d-$Case-$stamp"
$work=Join-Path $root "dev/run-m101d/$Case-$stamp"
$world='m101d-world'
$clientArm=if($Case -eq 'original-client'){'O'}else{'K'}
$serverArm=if($Case -eq 'original-server'){'O'}else{'K'}
$integrated=$Case -eq 'integrated'
$server=$null; $client=$null; $lock=$null; $exitCode=1
$hashes=@{}; $logs=@(); $proofs=@(); $owned=@{}
$oldJava=$env:JAVA_HOME; $oldPath=$env:PATH
function Guard {
    $roots=@(); if($null -ne $server){$roots+=$server.Id}; if($null -ne $client){$roots+=$client.Id}
    # Remember all descendants, including Gradle's child JVM after its parent exits.
    $snapshot=@(Get-CimInstance Win32_Process)
    $tree=@{}; foreach($id in @($roots)+@($owned.Keys)){$tree[[int]$id]=$true}
    $grew=$true
    while($grew){$grew=$false; foreach($p in $snapshot){if($tree.ContainsKey([int]$p.ParentProcessId)-and -not $tree.ContainsKey([int]$p.ProcessId)){$tree[[int]$p.ProcessId]=$true;$grew=$true}}}
    foreach($p in $snapshot){if($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)){$owned[[int]$p.ProcessId]=$true}}
    $foreign=@(Get-NavForeignJava -Roots (@($roots)+@($owned.Keys)))
    if($foreign.Count){throw ('Foreign Java workload: '+($foreign -join '; '))}
}
function Send([string]$Text){$server.StandardInput.WriteLine($Text);$server.StandardInput.Flush()}
function Wait-Marker($Proc,[string]$Log,[string]$Marker,[int]$Seconds=900){
    $until=(Get-Date).AddSeconds($Seconds)
    do {
        $text=Read-JavaPrototypeLog $Log
        if($text.Contains($Marker)){return}
        if($Proc.HasExited -or $text -cmatch 'M101D FAILED|M101D CLIENT DONE success=false|Encountered an unexpected exception|Exception ticking|NoSuchMethodError|NoSuchFieldError|AbstractMethodError'){throw "Process failed before $Marker; $Log"}
        Guard; Start-Sleep -Milliseconds 500
    }while((Get-Date)-lt $until)
    throw "Timeout waiting for $Marker; $Log"
}
function Wait-Exit($Proc,[string]$Log,[int]$Seconds=1200){
    $until=(Get-Date).AddSeconds($Seconds)
    while(-not $Proc.HasExited -and (Get-Date)-lt $until){Guard;Start-Sleep -Milliseconds 500}
    Assert-JavaPrototype $Proc.HasExited "completed before timeout: $Log"
    [M61ProcessLog]::Finish($Proc)
    Assert-JavaPrototype ($Proc.ExitCode -eq 0) "clean process exit: $Log"
}
function Job([string]$Pass){
    $path="$base-$Pass.job"
    $lines=@("case=$Case","world=$world","pass=$Pass","client=$clientArm","server=$serverArm",('base='+$base.Replace('\','/')))
    if(-not $integrated){$lines+="connect=$port"}
    [IO.File]::WriteAllLines($path,$lines,(New-Object Text.UTF8Encoding $false)); return $path
}
function Start-Gradle([string]$Task,[string]$Arm,[string]$Job,[string]$Label,[string]$WorldArg=''){
    $log="$base-$Label.log"; $script:logs+= $log
    $gradleArgs=@($Task,'--offline','--no-daemon','--console=plain',"-PrwRunDir=$work","-Pm10Job=$Job",'-PrwPrivzeto=1')
    if($Arm -eq 'O'){$gradleArgs+='-Pm10Original'}
    if($WorldArg){$gradleArgs+="-PrwWorld=$WorldArg"}
    $p=Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') $gradleArgs (Join-Path $root 'dev') $log
    return @{proc=$p;log=$log}
}
function Start-Server([string]$Pass,[string]$WorldArg){
    $r=Start-Gradle 'runServer' $serverArm (Job $Pass) "$Pass-server" $WorldArg
    $script:server=$r.proc; $script:serverLog=$r.log
    Wait-Marker $server $serverLog 'M101D FIXTURES READY'
    Wait-Marker $server $serverLog 'Done ('
}
function Stop-Server {
    Send 'save-all'; Send 'stop'; Wait-Exit $server $serverLog 180; $script:server=$null
}
function Read-Checks([string]$Path,[string[]]$Keys){
    Assert-JavaPrototype (Test-Path -LiteralPath $Path) "proof exists: $Path"
    $rows=@([IO.File]::ReadAllText($Path)|ConvertFrom-Json)
    Assert-JavaPrototype ($rows.Count -eq $Keys.Count) "exactly $($Keys.Count) assertions: $Path"
    Assert-JavaPrototype (@($rows|Where-Object {$_.ok -ne $true}).Count -eq 0) "all assertions passed: $Path"
    Assert-JavaPrototype ((@($rows.key|Sort-Object)-join '|') -ceq (@($Keys|Sort-Object)-join '|')) "exact assertion key set: $Path"
    return $rows
}
try{
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo "m101d-$Case"
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no existing Java workload'
    $env:JAVA_HOME=Join-Path $root '.tools/jdk8';$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
    $run=Join-Path $root 'dev/run'
    $original=Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'
    $mapped=Join-Path $root 'dev/libs/customnpcs-mapped-01Oct19.jar'
    $protected=@($original,$mapped,(Join-Path $root 'environment-lock.json'),(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'))
    $protected+=@(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File|Select-Object -ExpandProperty FullName)
    $options=Join-Path $run 'options.txt';$optionsExisted=Test-Path -LiteralPath $options
    if($optionsExisted){$protected+=$options}
    foreach($path in $protected){$hashes[$path]=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash}
    Assert-JavaPrototype ($hashes[$original] -ceq 'CAFACADE45FB2AA6AC52956487889D0F4A9D4F6A1682EA28A1F7ACA1BA100FA1') 'pinned distribution original'
    Assert-JavaPrototype ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match '(?m)^eula=true') 'existing real EULA acceptance'
    Assert-JavaPrototype (-not (Test-Path -LiteralPath $work)) 'fresh isolated work directory'
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'eula.txt') -Destination $work
    # Copy the same measured scene settings; only isolated path/port are changed.
    $props=[IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert-JavaPrototype ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener=New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,0)
    $listener.Start();$port=$listener.LocalEndpoint.Port;$listener.Stop()
    $props=$props -replace '(?m)^server-port=.*$',"server-port=$port"
    $props=$props -replace '(?m)^level-name=.*$',"level-name=$world"
    # Keep the established offline local test login; this is no authentication proof.
    Assert-JavaPrototype ($props -match '(?m)^online-mode=false\s*$') 'existing local offline test mode'
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'),$props,(New-Object Text.UTF8Encoding $false))
    # All main run options/configs remain untouched. The isolated config copy preserves
    # non-Rw settings; rwPrivzeto=1 restores only the code defaults under test.
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if($optionsExisted){Copy-Item -LiteralPath $options -Destination $work}
    foreach($pass in @('edit','verify')){
        if(-not $integrated){Start-Server $pass $world}
        $r=Start-Gradle 'runClient' $clientArm (Job $pass) "$pass-client"
        $client=$r.proc; $clientLog=$r.log
        Wait-Exit $client $clientLog 1500; $client=$null
        $proof=[IO.File]::ReadAllText("$base-$pass-client.json")|ConvertFrom-Json
        Assert-JavaPrototype ($proof.success -eq $true -and $proof.case -ceq $Case -and $proof.pass -ceq $pass) "$pass proof identity/success"
        $clientKeys=@('login-sync-dialogs','login-sync-quests','login-sync-factions','connection-mode')
        if($pass -eq 'edit'){$clientKeys+=@('locked-dialog-no-gui','offer-dialog-id','progress-dialog-id','trader-window','bank-window','skin-download-failed','render-continues','reload-texture-keys-stable','reload-render-alive')}
        else{$clientKeys+='mail-gui-delivered'}
        $tmp="$base-$pass-client-checks.json"; $proof.assertions|ConvertTo-Json -Depth 5|Set-Content -LiteralPath $tmp -Encoding UTF8
        $null=Read-Checks $tmp $clientKeys
        $expectedImages=if($pass -eq 'edit'){7}else{1}
        Assert-JavaPrototype (@($proof.screenshots).Count -eq $expectedImages) "$pass screenshot count"
        foreach($name in $proof.screenshots){$image=Join-Path $root "audit/screenshots/$name";Assert-JavaPrototype ((Get-Item -LiteralPath $image).Length -gt 1000) "screenshot $name"}
        $serverKeys=if($pass -eq 'edit'){@('fixture-categories','dialog-denied','quest-accepted','quest-incomplete','quest-progress','quest-finished','quest-reward','trader-buy','trader-sell','bank-deposit','bank-withdraw','final-items','post-malformed-alive')}else{@('fixture-categories','offline-mail-delivered','playerdata-equal','items-relogin','bank-relogin')}
        $null=Read-Checks "$base-$pass-server.json" $serverKeys
        $proofs+= $proof
        if($pass -eq 'edit'){
            if($integrated){Start-Server 'offline' "saves/$world"}
            $name=[IO.File]::ReadAllText("$base-player.txt")
            Assert-JavaPrototype ($name -cmatch '^[A-Za-z0-9_]+$') 'fixture player name'
            Send "m10probe offline $name"
            Wait-Marker $server $serverLog 'M101D OFFLINE MAIL QUEUED' 60
            $null=Read-Checks "$base-offline-server.json" @('mail-player-offline')
            # Wait for the actual offline mailbox on disk, not a guessed scheduler delay.
            $worldPath=if($integrated){Join-Path $work "saves/$world"}else{Join-Path $work $world}
            $until=(Get-Date).AddSeconds(30);$mailSaved=$false
            do{
                $files=@(Get-ChildItem -LiteralPath (Join-Path $worldPath 'customnpcs/playerdata') -Filter '*.json' -File)
                foreach($f in $files){try{if([IO.File]::ReadAllText($f.FullName).Contains('M101D_Offline')){$mailSaved=$true}}catch{}}
                if(-not $mailSaved){Guard;Start-Sleep -Milliseconds 200}
            }while(-not $mailSaved -and (Get-Date)-lt $until)
            Assert-JavaPrototype $mailSaved 'offline mail persisted before restart'
            Stop-Server
        }elseif(-not $integrated){Stop-Server}
    }
    $combined=($logs|ForEach-Object {Read-JavaPrototypeLog $_}) -join "`n"
    Assert-JavaPrototype ($combined.Contains('Error with EnumPacketServer.null')) 'malformed packet actually reached CNPC handler'
    Assert-JavaPrototype ($combined -cnotmatch 'M101D FAILED|NoSuchMethodError|NoSuchFieldError|AbstractMethodError|Exception ticking|Encountered an unexpected exception') 'no fixture/linkage/ticking failure'
    # The one deliberately malformed packet is expected to log a handled exception.
    $unexpected=@($combined -split "`n" | Where-Object {
        $_ -cmatch '\tat noppes\.' -and $_ -cnotmatch '\tat noppes\.npcs\.PacketHandlerServer\.lambda\$onServerPacket\$0\(' -and
        $_ -cnotmatch '\tat noppes\.npcs\.client\.ImageDownloadAlt\$1\.run\('
    })
    Assert-JavaPrototype ($unexpected.Count -eq 0) ('no unexpected CNPC stack: '+($unexpected -join '; '))
    foreach($path in $hashes.Keys){Assert-JavaPrototype ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ceq $hashes[$path]) "protected input unchanged: $path"}
    Assert-JavaPrototype ((Test-Path -LiteralPath $options) -eq $optionsExisted) 'main options presence unchanged'
    @{success=$true;package='M10.1d';case=$Case;server=$serverArm;client=$clientArm;inputs=$hashes;proofs=$proofs;logs=$logs;work=$work;limits=@('MCP dev runtime, not packaged-client Q22','local offline player, no real authentication','no user modpack','texture registry stability and heap samples, not a heap leak proof')} |
        ConvertTo-Json -Depth 12|Set-Content -LiteralPath "$base.json" -Encoding UTF8
    Write-Host "M10.1d evidence: $base.json"; $exitCode=0
}catch{Write-Host "FAIL $_"}
finally{
    if($null -ne $client){try{Stop-JavaPrototypeProcess $client}catch{Write-Host "Cleanup failure: $_";$exitCode=1}}
    if($null -ne $server){try{if(-not $server.HasExited){Send 'stop';if(-not $server.WaitForExit(30000)){Stop-JavaPrototypeProcess $server}};Stop-JavaPrototypeProcess $server}catch{Write-Host "Cleanup failure: $_";$exitCode=1}}
    # Only retained owned descendants; never kill another scenario or the owner's JVM.
    foreach($id in @($owned.Keys)){if(Get-Process -Id $id -ErrorAction SilentlyContinue){& taskkill.exe /PID $id /T /F 2>&1|Out-Null;$exitCode=1}}
    if($null -ne $lock){$lock.Dispose()}
    $env:JAVA_HOME=$oldJava;$env:PATH=$oldPath
}
exit $exitCode
