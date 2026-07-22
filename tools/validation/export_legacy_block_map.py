"""Export Forge 1.12's saved numeric block map as a release migration resource."""

import argparse
import json
from pathlib import Path

import nbtlib


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("level_dat", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    level = nbtlib.load(args.level_dat)
    entries = level["FML"]["Registries"]["minecraft:blocks"]["ids"]
    ids = {str(int(entry["V"])): str(entry["K"]) for entry in entries}
    result = {
        "format": 1,
        "minecraft": "1.12.2",
        "forge": "14.23.5.2847",
        "base_metals_artifact": "BaseMetals-1.12-2.5.0-rc2.332.jar",
        "base_metals_sha256": "034FD791A9E77345C19F6098C01D4B5B66F90EF4A953FB0831089935EAD9DFFB",
        "mmdlib_artifact": "MMDLib-1.12-1.0.0-rc2.36.jar",
        "mmdlib_sha256": "FE2229E6755A5FD306C33CC22DCCF055378D371339133E4912C5DC9213E360AF",
        "ids": dict(sorted(ids.items(), key=lambda entry: int(entry[0]))),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
