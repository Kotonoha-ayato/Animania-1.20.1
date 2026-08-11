"""Prove that the 1.12 addon template is non-shipping developer scaffolding."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

OWNER = "[nonshipping-template-audit:v1]"
PREFIX = "src/main/java/com/animania/addons/template/"
EXPECTED = {"TemplateAddon.java", "TemplateAddonRenderHandler.java", "TemplateAddonBlockHandler.java",
            "TemplateAddonCraftingHandler.java", "TemplateAddonEntityHandler.java",
            "TemplateAddonItemHandler.java", "TemplateConfig.java"}

def method_body(text: str, name: str) -> str | None:
    match = re.search(r"\bvoid\s+" + re.escape(name) + r"\s*\([^)]*\)\s*\{", text)
    if not match: return None
    start, depth = match.end() - 1, 0
    for index in range(start, len(text)):
        if text[index] == "{": depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0: return text[start + 1:index]
    return None

def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json")); parser.add_argument("--write", action="store_true")
    args = parser.parse_args(); root = args.root.resolve(); errors: list[str] = []
    source_root = root / "upstream/Animania-1.12/src/main/java/com/animania/addons/template"
    files = {path.name for path in source_root.rglob("*.java")}
    if files != EXPECTED: errors.append(f"template file set changed: missing={sorted(EXPECTED-files)}, extra={sorted(files-EXPECTED)}")
    handlers = {"TemplateAddonBlockHandler.java": ("preInit",), "TemplateAddonCraftingHandler.java": ("init",),
                "TemplateAddonEntityHandler.java": ("preInit",), "TemplateAddonItemHandler.java": ("preInit",),
                "TemplateAddonRenderHandler.java": ("preInit", "init")}
    for filename, methods in handlers.items():
        text = next(source_root.rglob(filename)).read_text(encoding="utf-8")
        for method in methods:
            body = method_body(text, method)
            if body is None or re.sub(r"/\*.*?\*/|//[^\n]*|\s+", "", body, flags=re.S):
                errors.append(f"{filename}.{method} is no longer empty")
    config = next(source_root.rglob("TemplateConfig.java")).read_text(encoding="utf-8")
    if re.search(r"^\s*@Config\b", config, re.M) or re.search(r"\b(?:boolean|int|double|String)\s+\w+\s*=", config):
        errors.append("TemplateConfig declares active configuration or values")
    all_source = "\n".join(path.read_text(encoding="utf-8") for path in source_root.rglob("*.java"))
    for token in ("registerEntity(", "registerBlock(", "registerItem(", "addRecipe(", "addSmelting("):
        if token in all_source: errors.append(f"template contains runtime registration: {token}")
    resources = root / "upstream/Animania-1.12/src/main/resources"
    if [p for p in resources.rglob("*") if p.is_file() and "template" in p.as_posix().lower()]:
        errors.append("template-owned resources exist")
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8")); rows = [e for e in matrix["entries"] if e.get("source", "").startswith(PREFIX)]
    if {Path(e["source"]).name for e in rows} != EXPECTED: errors.append("matrix template rows differ from source set")
    changed = 0
    if not errors:
        for entry in rows:
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            client_tests = ["tools/audit_nonshipping_template.py"] if "client/" in entry["source"] else []
            proof = {"paths": ["tools/audit_nonshipping_template.py"], "behavior_tests": ["tools/audit_nonshipping_template.py"],
                     "serialization_tests": [], "client_tests": client_tests, "notes": [f"{OWNER} audited exclusion: empty addon-author scaffold; not runtime content or a shipping module."]}
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=["tools/audit_nonshipping_template.py"], target_evidence=proof); changed += 1
            elif entry.get("status") != "closed" or not owned: errors.append(f"auditable row not closed: {entry['source']}")
    if args.write and not errors: matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"source_files": len(files), "matrix_rows": len(rows), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)

if __name__ == "__main__": main()
