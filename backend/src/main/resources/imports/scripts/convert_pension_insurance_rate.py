import argparse
import csv
import re
from pathlib import Path

import pdfplumber


INSURANCE_TYPE = "PENSION"


def download(url: str, path: Path):
    if not url:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    import requests
    res = requests.get(url, timeout=60)
    res.raise_for_status()
    path.write_bytes(res.content)


def to_rate_decimal(value):
    if value is None:
        return None

    text = str(value).replace("％", "%").replace(",", "").strip()

    match = re.search(r"(\d+(?:\.\d+)?)\s*%", text)
    if match:
        return round(float(match.group(1)) / 100, 5)

    match = re.search(r"(\d+(?:\.\d+)?)", text)
    if not match:
        return None

    number = float(match.group(1))
    return round(number / 100, 5) if number > 1 else round(number, 5)


def extract_total_rate(input_file: Path):
    candidates = []

    with pdfplumber.open(input_file) as pdf:
        for page in pdf.pages:
            text = page.extract_text() or ""

            for line in text.splitlines():
                if "厚生年金" not in line and "保険料率" not in line:
                    continue

                rates = re.findall(r"\d+(?:\.\d+)?\s*[％%]", line)
                for rate_text in rates:
                    rate = to_rate_decimal(rate_text)
                    if rate is not None and 0.10 <= rate <= 0.25:
                        candidates.append(rate)

    if not candidates:
        raise RuntimeError("厚生年金保険料率を抽出できませんでした。PDFレイアウトを確認してください。")

    return candidates[0]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, required=True)
    parser.add_argument("--url")
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    input_file = Path(args.input)

    if args.url and not input_file.exists():
        download(args.url, input_file)

    total_rate = extract_total_rate(input_file)

    rows = [{
        "insuranceType": INSURANCE_TYPE,
        "year": args.year,
        "employeeRate": round(total_rate / 2, 5),
        "employerRate": round(total_rate / 2, 5),
    }]

    output_file = Path(args.output)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    with output_file.open("w", encoding="utf-8-sig", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)

    print(f"CSV created: {output_file}")
    print(f"totalRate: {total_rate}")


if __name__ == "__main__":
    main()
