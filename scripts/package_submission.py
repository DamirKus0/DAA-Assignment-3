"""Package tracked deliverables without Git metadata, caches or build output."""
import argparse
import re
import subprocess
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def package(name, surname, group):
    parts = [re.sub(r"[^\w-]", "_", value) for value in (name, surname, group)]
    if any(not part for part in parts):
        raise ValueError("Name, surname and group must not be empty")
    label = "DAA_Assignment3_" + "_".join(parts)
    output = ROOT / "dist" / (label + ".zip")
    output.parent.mkdir(exist_ok=True)
    files = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
    excluded = {".git", "target", "tmp", "dist", ".idea", ".venv", "__pycache__"}
    count = 0
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for relative in sorted(files):
            if not relative:
                continue
            file = Path(relative)
            if any(part in excluded for part in file.parts) or file.name == ".DS_Store":
                continue
            archive.write(ROOT / file, arcname=label + "/" + file.as_posix())
            count += 1
    with zipfile.ZipFile(output) as archive:
        if archive.testzip() is not None:
            raise RuntimeError("ZIP integrity check failed")
    print(f"Packaged {count} files: {output}")
    return output


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", default="NAME")
    parser.add_argument("--surname", default="SURNAME")
    parser.add_argument("--group", default="GROUP")
    args = parser.parse_args()
    package(args.name, args.surname, args.group)
