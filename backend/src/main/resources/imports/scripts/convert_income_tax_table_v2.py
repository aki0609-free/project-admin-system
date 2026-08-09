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


def find_table_layout(df):
    for idx in range(len(df)):
        values = [str(v).replace(" ", "").replace("　", "").strip()
                  for v in df.iloc[idx].tolist()]
        dependent_columns = []
        for dependents in range(8):
            label = f"{dependents}人"
            try:
                dependent_columns.append(values.index(label))
            except ValueError:
                dependent_columns = []
                break

        if dependent_columns:
            first_tax_column = dependent_columns[0]
            if first_tax_column < 2:
                break
            return idx, first_tax_column - 2, first_tax_column - 1, dependent_columns

    raise RuntimeError("扶養親族数0〜7人のヘッダー行を見つけられませんでした。")


def normalize_excel(input_file: Path, year: int, output_file: Path):
    raw = pd.read_excel(input_file, header=None, dtype=object)

    header_row, min_column, max_column, dependent_columns = find_table_layout(raw)
    data = raw.iloc[header_row + 1:].copy()

    rows = []

    for _, row in data.iterrows():
        values = row.tolist()

        min_salary_text = str(values[min_column]) if len(values) > min_column else ""
        if "超え" in min_salary_text:
            # 税額表末尾の計算式説明であり、税額帯のデータ行ではない。
            continue

        min_salary = to_int(values[min_column]) if len(values) > min_column else None
        max_salary = to_int(values[max_column]) if len(values) > max_column else None

        if min_salary is not None and max_salary is None:
            range_text = "".join(str(values[index]) for index in (min_column, max_column))
            if "未満" in range_text:
                max_salary = min_salary - 1
                min_salary = 0
            elif "以上" in range_text:
                max_salary = MAX_SALARY_SENTINEL

        if min_salary is None:
            continue

        if max_salary is None:
            max_salary = MAX_SALARY_SENTINEL

        for dependents, column_index in enumerate(dependent_columns):
            tax_value = values[column_index] if len(values) > column_index else None
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
    duplicate_keys = out.duplicated(
        subset=["year", "minSalary", "maxSalary", "dependents"],
        keep=False,
    )
    if duplicate_keys.any():
        raise RuntimeError(
            "同じ給与範囲・扶養人数の税額が重複しています。Excelの形式を確認してください。"
        )
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
