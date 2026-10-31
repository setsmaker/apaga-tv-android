#!/usr/bin/env python3
"""Generate TV IR codes from the CC0 Flipper-IRDB collection."""
import os,re
from pathlib import Path
from collections import defaultdict,Counter
root=Path("/tmp/flipper-irdb/TVs")
out=Path("app/src/main/assets")
out.mkdir(parents=True,exist_ok=True)
valid={"NEC","NECext","Samsung32","SIRC","SIRC15","SIRC20"}
off=re.compile(r"^(?:power[ _-]*off|off[ _-]*power|off|turn[ _-]*off|power[ _-]*down)(?: *\\(discrete\\))?$",re.I)
toggle=re.compile(r"^(?:power|pwr|on[ /_-]*off|power[ /_-]*on[ /_-]*off|power[ _-]*toggle|standby|lg[ _-]*on[ /_-]*off|tv[ _-]*power)$",re.I)
rows={}; skip=Counter(); files=0
for path in sorted(root.rglob("*.ir")):
    files+=1
    brand=path.relative_to(root).parts[0].replace("|"," ")
    source=path.stem.replace("|"," ").replace("\n"," ")
    try: content=path.read_text(encoding="utf-8-sig",errors="replace")
    except OSError: continue
    for block in re.split(r"(?=^name: *)",content,flags=re.M):
        if not block.startswith("name:"): continue
        data={}
        for line in block.splitlines():
            if not line.startswith("#") and ":" in line:
                k,v=line.split(":",1);data[k.strip().lower()]=v.strip()
        name=data.get("name","").strip()
        mode="OFF" if off.fullmatch(name) else ("TOGGLE" if toggle.fullmatch(name) else None)
        if not mode: continue
        typ=data.get("type","").lower()
        if typ=="parsed":
            proto=data.get("protocol","")
            if proto not in valid:
                skip[(mode,proto)]+=1;continue
            try:
                address=int.from_bytes(bytes.fromhex(data["address"]),"little")
                command=int.from_bytes(bytes.fromhex(data["command"]),"little")
            except (KeyError, ValueError):
                skip[(mode,"bad parsed")]+=1;continue
            if address>65535 or command>65535:
                skip[(mode,"range")]+=1;continue
            fields=(proto,str(address),str(command),"","")
        elif typ=="raw":
            try:
                frequency=int(data["frequency"])
                timings=list(map(int,data["data"].split()))
            except (KeyError,ValueError):
                skip[(mode,"bad raw")]+=1;continue
            if not 30000<=frequency<=60000 or len(timings)<4 or len(timings)%2 or any(x<=0 for x in timings) or sum(timings)>1800000:
                skip[(mode,"invalid raw")]+=1;continue
            fields=("RAW","","",str(frequency),",".join(map(str,timings)))
        else:
            skip[(mode,typ)]+=1;continue
        key=(brand,mode)+fields
        rows.setdefault(key,(brand,mode)+fields+(source,))
for key in list(rows):
    if key[1]=="OFF" and (key[0],"TOGGLE")+key[2:] in rows:
        skip[("OFF","matches toggle")]+=1;del rows[key]
ordered=sorted(rows.values(),key=lambda r:(r[0].lower(),r[1],r[2],r[3],r[4]))
(out/"flipper_tv_catalog.psv").write_text("# brand|mode|protocol|address|command|frequency|data|model\n"+"\n".join("|".join(x) for x in ordered)+"\n",encoding="utf-8")
counts=defaultdict(lambda:[0,0])
for x in ordered:counts[x[0]][0 if x[1]=="OFF" else 1]+=1
summary=[f"TV files: {files}",f"Brands: {len(counts)}",f"OFF: {sum(v[0] for v in counts.values())}",f"TOGGLE: {sum(v[1] for v in counts.values())}",""]
summary +=[f"{b}: {v[0]} OFF, {v[1]} TOGGLE" for b,v in sorted(counts.items())]
summary+=["","Excluded:"]+[f"{x}: {y}" for x,y in sorted(skip.items())]
Path("ir-catalog-report.txt").write_text("\n".join(summary)+"\n",encoding="utf-8")
print("\n".join(summary),flush=True)
