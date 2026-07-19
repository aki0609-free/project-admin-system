import argparse
import re
import requests
from pathlib import Path

import pandas as pd


MAX_SALARY_SENTINEL = 999_999_999


def download(url: str | None, path: Path):
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


def tax_to_int(value):
    if pd.isna(value):
        return None

    text = str(value)
    text = text.replace(",", "")
    text = text.replace("円", "")
    text = text.strip()

    if text in ("", "-", "―"):
        return None

    match = re.search(r"\d+", text)
    return int(match.group()) if match else None


def normalize_excel(input_file: Path, year: int, output_file: Path):
    raw = pd.read_excel(input_file, sheet_name=0, header=None, dtype=object)

    rows = []

    for _, row in raw.iterrows():
        values = row.tolist()

        # このExcelは以下の列構成
        # 0: 行番号
        # 1: min salary
        # 2: max salary
        # 3〜10: 扶養 0人〜7人
        if len(values) < 11:
            continue

        min_salary = to_int(values[1])
        max_salary = to_int(values[2])

        if min_salary is None:
            continue

        # 先頭行「105000 / 円未満」対応
        if isinstance(values[2], str) and "未満" in values[2]:
            max_salary = min_salary - 1
            min_salary = 0

        if max_salary is None:
            max_salary = MAX_SALARY_SENTINEL

        for dependents in range(0, 8):
            tax_amount = tax_to_int(values[3 + dependents])

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
        raise RuntimeError("変換結果が0件です。Excelの列位置を確認してください。")

    out = pd.DataFrame(rows)
    out = out.drop_duplicates()
    out = out.sort_values(["year", "minSalary", "dependents"])

    output_file.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(output_file, index=False, encoding="utf-8")

    print(f"CSV created: {output_file}")
    print(f"rows: {len(out)}")


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument("--year", type=int, required=True)
    parser.add_argument("--url", required=False)

    parser.add_argument("--input", required=False)
    parser.add_argument("--source", required=False)

    parser.add_argument("--output", required=True)

    args = parser.parse_args()

    input_path = args.input or args.source

    if not input_path:
        raise RuntimeError("--input または --source を指定してください。")

    input_file = Path(input_path)
    output_file = Path(args.output)

    if args.url and not input_file.exists():
        download(args.url, input_file)

    if not input_file.exists():
        raise RuntimeError(f"入力ファイルが存在しません: {input_file}")

    normalize_excel(
        input_file=input_file,
        year=args.year,
        output_file=output_file,
    )


if __name__ == "__main__":
    main()