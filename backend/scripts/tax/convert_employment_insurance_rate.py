import argparse
import re
import requests
from pathlib import Path

import pandas as pd
import pdfplumber


INSURANCE_TYPE = "EMPLOYMENT_INSURANCE"


CATEGORY_KEYWORDS = {
    "GENERAL": ["一般の事業", "一般"],
    "AGRICULTURE_FORESTRY_FISHERY": ["農林水産", "清酒製造"],
    "CONSTRUCTION": ["建設"],
}


def download(url: str, path: Path):
    if not url:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    res = requests.get(url, timeout=60)
    res.raise_for_status()
    path.write_bytes(res.content)


def per_thousand_to_decimal(value):
    return round(float(value) / 1000, 5)


def extract_rates(input_file: Path, category: str):
    full_text = ""

    with pdfplumber.open(input_file) as pdf:
        for page in pdf.pages:
            full_text += "\n" + (page.extract_text() or "")

    keywords = CATEGORY_KEYWORDS.get(category)
    if not keywords:
        raise RuntimeError(f"未対応のcategoryです: {category}")

    for line in full_text.splitlines():
        if not any(k in line for k in keywords):
            continue

        rates = re.findall(r"(\d+(?:\.\d+)?)\s*/\s*1000", line)

        if len(rates) >= 2:
            return per_thousand_to_decimal(rates[0]), per_thousand_to_decimal(rates[1])

        if len(rates) == 1:
            return per_thousand_to_decimal(rates[0]), None

    raise RuntimeError(f"雇用保険料率を抽出できませんでした。category={category}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, required=True)
    parser.add_argument("--category", default="GENERAL")
    parser.add_argument("--url")
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    input_file = Path(args.input)

    if args.url and not input_file.exists():
        download(args.url, input_file)

    employee_rate, employer_rate = extract_rates(input_file, args.category)

    out = pd.DataFrame([{
        "insuranceType": INSURANCE_TYPE,
        "year": args.year,
        "employeeRate": employee_rate,
        "employerRate": employer_rate,
    }])

    output_file = Path(args.output)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(output_file, index=False, encoding="utf-8-sig")

    print(f"CSV created: {output_file}")
    print(f"employeeRate: {employee_rate}")
    print(f"employerRate: {employer_rate}")


if __name__ == "__main__":
    main()