# Java 8 NPC script template

This is an offline IDE project for the **existing** CustomNPC rework Java NPC API.
Use a prepared checkout and its pinned `.tools/jdk8`; no API stubs, new dependencies,
Minecraft launch, or world access is needed to compile. Java is for trusted authors
with process permissions, without a sandbox. Review AI-generated code before enabling it.

From the repository root, export the current compiled API, then check the template:

```powershell
.\dev.ps1 exportJavaTemplateClasspath --offline
.\scripts\java\template\check.ps1 -Ide
```

The export builds the repository classes if needed. `-Ide` compiles `src/main/java` and
generates IntelliJ IDEA/Eclipse project files, then runs `compileScripts`. Open this
folder's `build.gradle` in IntelliJ (or import the generated Eclipse project); set
**both Gradle JVM and project SDK to JDK 8** and language level 8. Local classpath files
and generated IDE files are ignored. API symbols and autocomplete come from the actual
patched classes first, then the repository's mapped dependencies, never copied wrappers.
Use the public [v1 API](../../../docs/api/API.md) and [hook signatures](../../../docs/api/HOOKS.md);
Minecraft implementation types visible on the dependency path are not supported script API.

`check.ps1` without `-Ide` runs only the shared runtime compiler in a plain JVM:

```powershell
.\scripts\java\template\check.ps1
.\scripts\java\template\check.ps1 -Manifest examples/invalid/scripts.json -Report .local/invalid.json
```

The second command must fail: wrong `@Override` event, wrong API value type, and a broken
include. Read JSON `tabs[].problems[]`: `file`, `origin`, `line`, `column`, `code`, `message`.
The errors point to line 5 in each invalid file; these files are outside the IDE source
set. Fix the source, then check again. Exit is successful only if every tab compiles.
The Gradle task uses the same `CompileScripts`, `JavaSources`, `JdkScriptCompiler`,
Java 8 options and explicit API classpath as the repository CLI. It writes no script
bytecode and does not load/execute scripts or annotation processors. IDE compilation
writes classes in `build/` for inspection, but never invokes script methods.

`scripts.json` describes separate tabs; the **first file** of each tab is its public
entry class, later files are ordered includes. Filenames match public types; packages
are allowed. One Java source unit per file. To use the starter in an NPC, choose Java,
paste all of `StarterNpc.java` (including package/imports) in one tab, place
`CounterText.java` in the server's scripts library, and select it in that tab's ScriptList.
Enable scripting using the existing trusted-author permissions. Runtime selection/GUI
and automatic modpack classpath setup are completed in M6.12; currently the administrator
must supply the environment's `RwJavaClasspath` (including Forge universal in packaged
Forge), as documented in [M6.7](../../../docs/meritve/2026-10-29-M6.7-cli.md).
Do not install this IDE project's classes/JAR as a mod. There is no `main` or game runner.

The example overrides init/tick/interact/timer. Runtime needs a public no-argument
constructor (the implicit one here suffices); use `@Override` and exact event types to
catch mistakes. Each NPC/tab/generation has its own instance and static state. Successful
reload resets fields; use the API's storeddata explicitly for persistence. A rejected
reload keeps the last good generation and reports the error. CLI acceptance is compile-only;
it does not prove runtime hook execution, permissions, save/restart, or plain-class binding.

You can copy this directory, including its local classpath file, to a path with spaces.
Set `$env:CNPC_REWORK_ROOT` to the original prepared checkout for `check.ps1`, or use its
pinned `dev/gradlew.bat -p <copy> ...` with `JAVA_HOME` set to its JDK 8. To export directly
to a copy: `dev.ps1 exportJavaTemplateClasspath --offline -PjavaTemplateOutput=C:/path/to/copy/.local/api-classpath.txt`.
Paths in manifests are relative to the manifest; Gradle `-P` paths are relative to this
template. Re-export after changing/restoring the checkout/API or moving dependencies.
This development classpath is not a portable classpath for a different modpack.
