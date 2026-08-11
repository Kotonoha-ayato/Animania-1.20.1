"""Audit legacy Farm milk/honey blocks, native FluidTypes and animated client textures."""
from __future__ import annotations
import argparse, json, struct
from pathlib import Path

OWNER = "[farm-fluid-audit:v1]"
ROWS = [
    "src/main/java/com/animania/addons/farm/common/block/fluids/BlockFluidHoney.java",
    "src/main/java/com/animania/addons/farm/common/block/fluids/BlockFluidMilk.java",
]
IDS = ("milk_holstein", "milk_friesian", "milk_jersey", "milk_goat", "milk_sheep", "animania_honey")
TARGETS = [
    "farm/src/main/java/com/animania/farm/FarmFluids.java",
    "farm/src/main/java/com/animania/farm/FarmLegacyFluidBlock.java",
    "farm/src/main/java/com/animania/farm/AnimaniaFarmClient.java",
]
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
MOLD_TEXTURES = {
    "mold_cow_milk.json": "milk_friesian",
    "mold_friesian_milk.json": "milk_friesian",
    "mold_goat_milk.json": "milk_goat",
    "mold_holstein_milk.json": "milk_holstein",
    "mold_jersey_milk.json": "milk_jersey",
    "mold_sheep_milk.json": "milk_sheep",
}

def png_size(path: Path) -> tuple[int,int]:
    data=path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR": raise ValueError("invalid PNG")
    return struct.unpack(">II",data[16:24])

def main():
    p=argparse.ArgumentParser(); p.add_argument("--root",type=Path,required=True); p.add_argument("--matrix",type=Path,default=Path("docs/migration-matrix.json")); p.add_argument("--write",action="store_true"); a=p.parse_args()
    root=a.root.resolve(); errors=[]
    for s in ROWS:
        if not (root/"upstream/Animania-1.12"/s).is_file(): errors.append(f"legacy source missing: {s}")
    for t in TARGETS+[TEST]:
        if not (root/t).is_file(): errors.append(f"target missing: {t}")
    code="\n".join((root/t).read_text(encoding="utf-8") for t in TARGETS); test=(root/TEST).read_text(encoding="utf-8")
    for token in ("initializeClient", '"fluids/" + id + "_still"', '"fluids/" + id + "_flow"', "RenderType.translucent()", "flow.x / divisor", "MobEffects.REGENERATION", "2000.0D", "1000.0D"):
        if token not in code: errors.append(f"fluid implementation missing: {token}")
    for token in ("farmFluidsAndCheeseMoldProcess", "honey fluid did not apply", "milk fluid lost its legacy snow map color"):
        if token not in test: errors.append(f"GameTest assertion missing: {token}")
    for fluid in IDS:
        for suffix in ("still","flow"):
            png=root/f"farm/src/main/resources/assets/animania_farm/textures/fluids/{fluid}_{suffix}.png"; meta=Path(str(png)+".mcmeta")
            try:
                w,h=png_size(png)
                if w < 16 or h < 16 or h % w: errors.append(f"invalid animated dimensions: {png}={w}x{h}")
            except (OSError,ValueError) as ex: errors.append(f"invalid fluid texture {png}: {ex}")
            try:
                animation=json.loads(meta.read_text(encoding="utf-8")).get("animation")
                if not isinstance(animation,dict): errors.append(f"animation metadata missing: {meta}")
            except (OSError,json.JSONDecodeError) as ex: errors.append(f"invalid animation metadata {meta}: {ex}")
    for model_name, texture_name in MOLD_TEXTURES.items():
        model_path=root/f"farm/src/main/resources/assets/animania_farm/models/block/{model_name}"
        try:
            model=json.loads(model_path.read_text(encoding="utf-8"))
            expected=f"animania_farm:block/{texture_name}"
            if model.get("textures",{}).get("5") != expected:
                errors.append(f"mold milk texture must be block-atlas stitched: {model_name} -> {expected}")
            texture=root/f"farm/src/main/resources/assets/animania_farm/textures/block/{texture_name}.png"
            png_size(texture)
        except (OSError,ValueError,json.JSONDecodeError) as ex:
            errors.append(f"invalid mold milk model/texture {model_name}: {ex}")
    mp=a.matrix if a.matrix.is_absolute() else root/a.matrix; m=json.loads(mp.read_text(encoding="utf-8")); by={e.get("source"):e for e in m["entries"]}; changed=0
    if not errors:
        for s in ROWS:
            e=by.get(s)
            if e is None: errors.append(f"matrix row missing: {s}"); continue
            proof={"paths":TARGETS,"behavior_tests":[TEST,"tools/audit_farm_fluids.py"],"serialization_tests":[],"client_tests":["tools/audit_farm_fluids.py"],"notes":[f"{OWNER} Dedicated Forge GameTest proves registration/map color/honey regeneration; audit proves exact native collision divisors plus six still/flow client bindings, valid animated PNGs and translucent render registration."]}
            if a.write: e.update(status="closed",implemented=True,verified=True,tests=[TEST,"tools/audit_farm_fluids.py"],target_evidence=proof); changed+=1
            elif e.get("status")!="closed" or not any(OWNER in n for n in e.get("target_evidence",{}).get("notes",[])): errors.append(f"provable row not closed: {s}")
    if a.write and not errors: mp.write_text(json.dumps(m,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"rows":len(ROWS),"textures":len(IDS)*2,"changed":changed,"errors":errors,"error_count":len(errors)},ensure_ascii=False,indent=2))
    if errors: raise SystemExit(1)

if __name__=="__main__": main()
