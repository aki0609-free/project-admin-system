import argparse
import re
import requests
from pathlib import Path

import pandas as pd
import pdfplumber


MAX_SALARY_SENTINEL = 999_999_999


def download(url: str, path: Path):
    if not url:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    res = requests.get(url, timeout=60)
    res.raise_for_status()
    path.write_bytes(res.content)


def to_int(value):
    if value is None:
        return None

    text = str(value)
    text = text.replace(",", "")
    text = text.replace("円", "")
    text = text.replace("以上", "")
    text = text.replace("未満", "")
    text = text.strip()

    match = re.search(r"\d+", text)
    return int(match.group()) if match else None


def extract_rows(input_file: Path):
    rows = []

    with pdfplumber.open(input_file) as pdf:
        for page in pdf.pages:
            tables = page.extract_tables() or []

            for table in tables:
                for row in table:
                    values = [(v or "").strip() for v in row]

                    numbers = [to_int(v) for v in values]

                    if len(numbers) < 3:
                        continue

                    # 保険料額表はだいたい
                    # 等級 / 月額範囲 / 標準報酬月額 の並びを含む
                    standard_salary = next((n for n in numbers if n and 58000 <= n <= 1390000), None)

                    salary_numbers = [n for n in numbers if n and n >= 0]

                    if not standard_salary or len(salary_numbers) < 2:
                        continue

                    min_salary = salary_numbers[1] if len(salary_numbers) > 1 else None
                    max_salary = salary_numbers[2] if len(salary_numbers) > 2 else MAX_SALARY_SENTINEL

                    if min_salary is None:
                        continue

                    if max_salary <= min_salary:
                        max_salary = MAX_SALARY_SENTINEL

                    rows.append({
                        "minSalary": min_salary,
                        "maxSalary": max_salary,
                        "standardSalary": standard_salary,
                    })

    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, required=False)
    parser.add_argument("--url")
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    input_file = Path(args.input)

    if args.url and not input_file.exists():
        download(args.url, input_file)

    rows = extract_rows(input_file)

    if not rows:
        raise RuntimeError("標準報酬月額の抽出結果が0件です。PDFレイアウトを確認してください。")

    out = pd.DataFrame(rows).drop_duplicates()
    out = out.sort_values(["minSalary", "standardSalary"])

    output_file = Path(args.output)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(output_file, index=False, encoding="utf-8-sig")

    print(f"CSV created: {output_file}")
    print(f"rows: {len(out)}")


if __name__ == "__main__":
    main()