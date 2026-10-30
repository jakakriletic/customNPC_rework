// Compile only: never evaluate the world fixture or launch Minecraft.
var path = String(arguments[0]);
var text = String(new java.lang.String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)), 'UTF-8'));
text = text.replace(/@@OUTPUT@@/g, '"syntax-output"').replace(/@@REQUEST@@/g, '"syntax-request"');
var engine = new javax.script.ScriptEngineManager().getEngineByName('nashorn');
if (engine === null) { throw new Error('No Nashorn compiler'); }
engine.compile(text);
print('M104 Nashorn syntax PASS');
