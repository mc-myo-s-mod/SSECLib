"""Check the artifacts a downstream build receives from the local Maven repository."""
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET
from zipfile import ZipFile

module = Path.cwd().name
minecraft = {"forge-1-20-1": "1.20.1", "neoforge-1-21-1": "1.21.1", "neoforge-26-1-2": "26.1.2"}[module]
artifact = f"ssec-commands-{minecraft}"
version = os.environ.get("ORG_GRADLE_PROJECT_ssec_commands_version", "1.0.0")
repo = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / ".m2" / "repository"
directory = repo / "me" / "myogoo" / artifact / version
base = f"{artifact}-{version}"
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
pom = ET.parse(directory / f"{base}.pom").getroot()
assert pom.findtext("m:groupId", namespaces=ns) == "me.myogoo"
assert pom.findtext("m:artifactId", namespaces=ns) == artifact
assert pom.findtext("m:version", namespaces=ns) == version
assert not pom.findall(".//m:dependency", ns), "Host dependencies leaked into the POM"
assert pom.findtext("m:url", namespaces=ns) == "https://github.com/mc-myo-s-mod/SSECLib"
assert pom.findtext("m:developers/m:developer/m:id", namespaces=ns) == "myogoo"
assert "lgpl-3.0" in pom.findtext("m:licenses/m:license/m:url", namespaces=ns)
for tag in ("m:name", "m:description", "m:scm/m:connection", "m:scm/m:developerConnection", "m:scm/m:url"):
    assert pom.findtext(tag, namespaces=ns), tag

metadata = directory / f"{base}.module"
if metadata.exists():
    for variant in json.loads(metadata.read_text(encoding="utf-8"))["variants"]:
        assert not variant.get("dependencies"), variant["name"]
        if variant["name"] in {"apiElements", "runtimeElements"}:
            assert [f["name"] for f in variant["files"]] == [f"{base}.jar"]

registrar = "me/myogoo/ssec/command/CommandRegistrar"
for suffix, entry in ((".jar", registrar + ".class"), ("-sources.jar", registrar + ".java"),
                      ("-javadoc.jar", registrar + ".html")):
    with ZipFile(directory / (base + suffix)) as jar:
        assert jar.getinfo(entry).file_size > 0
        if suffix == ".jar":
            assert all(name.startswith(("META-INF/", "me/")) for name in jar.namelist())

if minecraft == "1.20.1":
    for suffix, method in ((".jar", "m_6761_"), ("-dev.jar", "hasPermission")):
        bytecode = subprocess.check_output(
            ["javap", "-c", "-classpath", str(directory / (base + suffix)),
             "me.myogoo.ssec.api.command.permission.PermissionLevel"], text=True
        )
        assert f"CommandSourceStack.{method}:(I)Z" in bytecode, suffix
print(f"Verified me.myogoo:{artifact}:{version}")
