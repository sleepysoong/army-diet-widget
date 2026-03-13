#!/usr/bin/env python3

from pathlib import Path


VERSION_FILE = Path(__file__).resolve().parents[1] / "version.properties"


def read_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = raw_line.partition("=")
        if not separator:
            raise ValueError(f"Invalid property line: {raw_line}")
        properties[key.strip()] = value.strip()
    return properties


def bump_version_name(version_name: str) -> str:
    parts = version_name.split(".")
    for index in range(len(parts) - 1, -1, -1):
        if parts[index].isdigit():
            parts[index] = str(int(parts[index]) + 1)
            return ".".join(parts)
    return f"{version_name}.1"


def main() -> None:
    properties = read_properties(VERSION_FILE)
    current_code = int(properties["VERSION_CODE"])
    current_name = properties["VERSION_NAME"]

    properties["VERSION_CODE"] = str(current_code + 1)
    properties["VERSION_NAME"] = bump_version_name(current_name)

    VERSION_FILE.write_text(
        "\n".join(
            [
                f"VERSION_CODE={properties['VERSION_CODE']}",
                f"VERSION_NAME={properties['VERSION_NAME']}",
            ]
        )
        + "\n",
        encoding="utf-8",
    )

    print(f"VERSION_CODE={properties['VERSION_CODE']}")
    print(f"VERSION_NAME={properties['VERSION_NAME']}")


if __name__ == "__main__":
    main()
