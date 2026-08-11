import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
AUDITOR = ROOT / "tools" / "audit_production_bytecode.py"


class ProductionBytecodeAuditTest(unittest.TestCase):
    def run_audit(self, contents: dict[str, bytes]) -> subprocess.CompletedProcess[str]:
        with tempfile.TemporaryDirectory() as directory:
            jar = Path(directory) / "test.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("META-INF/mods.toml", "modLoader=javafml")
                for name, data in contents.items():
                    archive.writestr(name, data)
            return subprocess.run([sys.executable, str(AUDITOR), "--jar", str(jar)],
                                  text=True, capture_output=True)

    def test_rejects_mapped_runtime_symbols(self) -> None:
        result = self.run_audit({
            "com/animania/common/entity/AnimaniaVehicleEntity.class": b"OPTIONAL_UUID defineId"
        })
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("retains mapped symbol", result.stderr)

    def test_accepts_srg_runtime_symbols(self) -> None:
        result = self.run_audit({
            "com/animania/common/entity/AnimaniaVehicleEntity.class": b"f_135041_ m_135353_"
        })
        self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main()
