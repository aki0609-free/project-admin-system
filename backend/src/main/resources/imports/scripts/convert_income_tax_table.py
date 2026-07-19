import argparse
import re
from pathlib import Path

import pandas as pd


MAX_SALARY_SENTINEL = 999_999_999


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
    if not match:
        return None

    return int(match.group())


def clean_tax(value):
    if pd.isna(value):
        return None

    text = str(value).replace(",", "").replace("円", "").strip()

    if text in ("", "-", "―"):
        return None

    match = re.search(r"\d+", text)
    if not match:
        return None

    return int(match.group())


def find_header_row(df):
    """
    「以上」「未満」「0人」「1人」... が並ぶ行を探す。
    国税庁Excelのレイアウト差分に備えて、ゆるく検出。
    """
    for idx in range(len(df)):
        values = [str(v).strip() for v in df.iloc[idx].tolist()]
        joined = "|".join(values)

        has_range = "以上" in joined and "未満" in joined
        has_dependents = any("0" == v or "0人" in v or "0 人" in v for v in values)

        if has_range and has_dependents:
            return idx

    raise RuntimeError("ヘッダー行を見つけられませんでした。Excelの形式を確認してください。")


def normalize_excel(input_file: Path, year: int, output_file: Path):
    raw = pd.read_excel(input_file, header=None, dtype=object)

    header_row = find_header_row(raw)

    # ヘッダー行の次行以降がデータ候補
    data = raw.iloc[header_row + 1:].copy()

    rows = []

    for _, row in data.iterrows():
        values = row.tolist()

        # 先頭2列付近に「以上」「未満」の金額がある想定
        min_salary = to_int(values[0]) if len(values) > 0 else None
        max_salary = to_int(values[1]) if len(values) > 1 else None

        # 「105,000円未満」などが1セルに入る場合
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

        # 扶養人数 0〜7人分。
        # 通常は3列目以降に 0人,1人,... が並ぶ想定。
        dependent_tax_values = values[2:10]

        for dependents, tax_value in enumerate(dependent_tax_values):
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
        raise RuntimeError("変換結果が0件です。Excelの列位置を確認してください。")

    out = pd.DataFrame(rows)
    out = out.sort_values(["year", "minSalary", "dependents"])

    output_file.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(output_file, index=False, encoding="utf-8-sig")

    print(f"CSV created: {output_file}")
    print(f"rows: {len(out)}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, required=True)
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)

    args = parser.parse_args()

    normalize_excel(
        input_file=Path(args.input),
        year=args.year,
        output_file=Path(args.output),
    )


if __name__ == "__main__":
    main()