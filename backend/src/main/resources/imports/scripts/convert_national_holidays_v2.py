import argparse
import csv
import io
from datetime import datetime
from pathlib import Path

import requests


DATE_HEADERS = ("国民の祝日・休日月日", "月日", "date")
NAME_HEADERS = ("国民の祝日・休日名称", "名称", "name")


def download(url: str, path: Path):
    if not url:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    response = requests.get(url, timeout=60)
    response.raise_for_status()
    path.write_bytes(response.content)


def decode_source(content: bytes) -> str:
    for encoding in ("utf-8-sig", "cp932", "shift_jis"):
        try:
            return content.decode(encoding)
        except UnicodeDecodeError:
            continue
    raise RuntimeError("内閣府の祝日CSVの文字コードを判定できませんでした。")


def find_value(row: dict[str, str], candidates: tuple[str, ...]) -> str:
    for candidate in candidates:
        value = row.get(candidate)
        if value and value.strip():
            return value.strip()
    return ""


def parse_date(value: str) -> str:
    normalized = value.strip().replace("年", "/").replace("月", "/").replace("日", "")
    for pattern in ("%Y/%m/%d", "%Y-%m-%d"):
        try:
            return datetime.strptime(normalized, pattern).date().isoformat()
        except ValueError:
            continue
    raise RuntimeError(f"祝日の日付を解釈できません。 value={value}")


def normalize(input_file: Path, output_file: Path):
    reader = csv.DictReader(io.StringIO(decode_source(input_file.read_bytes())))
    rows = []
    for source in reader:
        raw_date = find_value(source, DATE_HEADERS)
        name = find_value(source, NAME_HEADERS)
        if not raw_date or not name:
            continue
        rows.append({
            "calendarType": "NATIONAL_HOLIDAY",
            "holidayDate": parse_date(raw_date),
            "holidayName": name,
            "dayType": "CLOSED",
            "note": "内閣府公式CSV",
            "activeFlag": "true",
        })

    if not rows:
        raise RuntimeError("内閣府の祝日CSVからデータを取得できませんでした。")

    unique = {(row["calendarType"], row["holidayDate"]): row for row in rows}
    output_file.parent.mkdir(parents=True, exist_ok=True)
    with output_file.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=[
            "calendarType", "holidayDate", "holidayName",
            "dayType", "note", "activeFlag",
        ])
        writer.writeheader()
        writer.writerows(sorted(unique.values(), key=lambda row: row["holidayDate"]))

    print(f"CSV created: {output_file}")
    print(f"rows: {len(unique)}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url")
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    input_file = Path(args.input)
    if args.url and not input_file.exists():
        download(args.url, input_file)
    if not input_file.exists():
        raise RuntimeError("祝日CSVが見つかりません。")
    normalize(input_file, Path(args.output))


if __name__ == "__main__":
    main()
