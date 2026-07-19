import argparse
import re
import requests
from pathlib import Path

import pandas as pd


MAX_SALARY_SENTINEL = 999_999_999


def download(url: str, path: Path):
    if not url:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    res = requests.get(url, timeout=60)
    res.raise_for_status()
    path.write_bytes(res.content)


def to_int(value):
    if pd.isna(value):
        return None

    text = str(value)
    text = text.replace(",", "")
    text = text.replace("円", "")
    text = text.replace("未満", "")
    text = text.replace("以上", "")
    text = text.replace(" ", "")
    text = text.replace("　", "")

    match = re.search(r"\d+", text)
    return int(match.group()) if match else None


def clean_tax(value):
    if pd.isna(value):
        return None

    text = str(value).replace(",", "").replace("円", "").strip()
    if text in ("", "-", "―"):
        return None

    match = re.search(r"\d+", text)
    return int(match.group()) if match else None


def find_header_row(df):
    for idx in range(len(df)):
        values = [str(v).strip() for v in df.iloc[idx].tolist()]
        joined = "|".join(values)

        has_range = "以上" in joined and "未満" in joined
        has_dependents = any(v == "0" or "0人" in v or "0 人" in v for v in values)

        if has_range and has_dependents:
            return idx

    raise RuntimeError("ヘッダー行を見つけられませんでした。")


def normalize_excel(input_file: Path, year: int, output_file: Path):
    raw = pd.read_excel(input_file, header=None, dtype=object)

    header_row = find_header_row(raw)
    data = raw.iloc[header_row + 1:].copy()

    rows = []

    for _, row in data.iterrows():
        values = row.tolist()

        min_salary = to_int(values[0]) if len(values) > 0 else None
        max_salary = to_int(values[1]) if len(values) > 1 else None

        if min_salary is not None and max_salary is None:
            text = str(values[0])
            if "未満" in text:
                max_salary = min_salary - 1
                min_salary = 0
            elif "以上" in text:
                max_salary = MAX_SALARY_SENTINEL

        if min_salary is None:
            continue

        if max_salary is None:
            max_salary = MAX_SALARY_SENTINEL

        for dependents, tax_value in enumerate(values[2:10]):
            tax_amount = clean_tax(tax_value)

            if tax_amount is None:
                continue

            rows.append({
                "year": year,
                "minSalary": min_salary,
                "maxSalary": max_salary,
                "dependents": dependents,
                "taxAmount": tax_amount,
            })

    if not rows:
        raise RuntimeError("変換結果が0件です。")

    out = pd.DataFrame(rows).sort_values(["year", "minSalary", "dependents"])
    output_file.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(output_file, index=False, encoding="utf-8-sig")

    print(f"CSV created: {output_file}")
    print(f"rows: {len(out)}")


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

    normalize_excel(input_file, args.year, Path(args.output))


if __name__ == "__main__":
    main()