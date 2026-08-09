import argparse
import csv
import re
from pathlib import Path

import pdfplumber


INSURANCE_TYPE = "CARE_INSURANCE"


def download(url: str, path: Path):
    if not url:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    import requests
    response = requests.get(url, timeout=60)
    response.raise_for_status()
    path.write_bytes(response.content)


def to_rate_decimal(value):
    if value is None:
        return None
    text = str(value).replace("％", "%").replace(",", "").strip()
    match = re.search(r"(\d+(?:\.\d+)?)\s*%", text)
    if not match:
        return None
    return round(float(match.group(1)) / 100, 5)


def extract_rate(input_file: Path):
    candidates = []
    with pdfplumber.open(input_file) as pdf:
        for page in pdf.pages:
            text = page.extract_text() or ""
            for line in text.splitlines():
                if "介護保険料率" not in line:
                    continue
                for rate_text in re.findall(r"\d+(?:\.\d+)?\s*[％%]", line):
                    rate = to_rate_decimal(rate_text)
                    if rate is not None and 0.005 <= rate <= 0.05:
                        candidates.append(rate)

    if not candidates:
        raise RuntimeError("介護保険料率を抽出できませんでした。PDFレイアウトを確認してください。")
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

    total_rate = extract_rate(input_file)
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
