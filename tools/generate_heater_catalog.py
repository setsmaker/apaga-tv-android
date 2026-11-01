#!/usr/bin/env python3
"""Make a small, offline manually-tested heater IR catalogue from CC0 Flipper-IRDB.
This is NOT an S&P HE-1800 code database and contains NO confirmed code for S&P.
"""
from pathlib import Path
import re
from collections import Counter
root=Path("/tmp/flipper-irdb/Heaters")
out=Path("app/src/main/assets");out.mkdir(parents=True,exist_ok=True)
allowed={"NEC","NECext","Samsung32","SIRC","SIRC15","SIRC20","RC5","RC5X"}
pattern=re.compile(r"^(?:power|on|off|on[\s_/-]*off|power[\s_/-]*on[\s_/-]*off|power[\s_/-]*toggle|standby|heat|heater|heat[\s_/-]*on|heat[\s_/-]*off|switch[\s_/-]*on|switch[\s_/-]*off|start|stop|on[\s_/-]*off[\s_/-]*heat)$",re.I)
count=Counter()
entries={}
for file in sorted(root.rglob("*.ir")):
    brand=file.relative_to(root).parts[0]
    try: content=file.read_text(encoding="utf-8-sig",errors="replace")
    except OSError: continue
    for chunk in re.split(r"(?=^name:\s*)",content,flags=re.M):
        if not chunk.startswith("name:"):continue
        data={}
        for line in chunk.splitlines():
            if line.startswith("#") or ":" not in line:continue
            k,v=line.split(":",1);data[k.strip().lower()]=v.strip()
        commandName=data.get("name","")
        if not pattern.fullmatch(commandName.strip()):continue
        t=data.get("type","").lower()
        if t=="parsed":
            proto=data.get("protocol","")
            if proto not in allowed:count["unsupported_protocol"]+=1;continue
            try:
                address=int.from_bytes(bytes.fromhex(data["address"]),"little")
                cmd=int.from_bytes(bytes.fromhex(data["command"]),"little")
            except (KeyError,ValueError):continue
            if address>65535 or cmd>65535:continue
            info=[proto,str(address),str(cmd),"",""]
        elif t=="raw":
            try:
                freq=int(data["frequency"])
                timings=[int(q) for q in data["data"].split()]
            except (KeyError,ValueError):continue
            if len(timings)%2:timings.append(26000)
            if not 30000<=freq<=60000 or len(timings)<4 or not all(v>0 for v in timings) or sum(timings)>1850000:
                count["invalid_raw"]+=1;continue
            info=["RAW","","",str(freq),",".join(map(str,timings))]
        else:continue
        fields=[brand,file.stem,commandName]+info
        # Deduplicate on waveform semantics and manufacturer.
        key=(brand,*info)
        entries.setdefault(key,fields)
items=sorted(entries.values(),key=lambda a:(a[0].lower(),a[1].lower(),a[2].lower()))
header="# brand|model|button|protocol|address|command|frequency|raw_data"
(out/"heater_scan_catalog.psv").write_text(header+"\n"+"\n".join("|".join(str(x).replace("|"," ").replace("\n"," ") for x in row) for row in items)+"\n",encoding="utf-8")
Path("heater-scan-report.txt").write_text(f"Calefactores: {len(items)} señales candidatas de {len({a[0] for a in items})} fabricantes. Ninguna identificada como código S&P HE-1800.\n"+"\n".join(f"{x[0]} / {x[1]} / {x[2]} ({x[3]})" for x in items)+"\n",encoding="utf-8")
print(f"Scanner calefactores: {len(items)} señales candidatas, {len({a[0] for a in items})} fabricantes.")
