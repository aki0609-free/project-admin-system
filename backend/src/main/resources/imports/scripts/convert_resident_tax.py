import argparse
from pathlib import Path

import pandas as pd


def normalize(input_file: Path, year: int, output_file: Path):
    df = pd.read_csv(input_file, dtype=str)

    rename_map = {
        "社員ID": "employeeCode",
        "従業員コード": "employeeCode",
        "氏名": "employeeName",
        "市区町村": "municipality",
        "年税額": "annualTaxAmount",
        "月額": "monthlyTaxAmount",
        "6月": "juneAmount",
        "7月以降": "monthlyTaxAmountFromJuly",
    }

    df = df.rename(columns={k: v for k, v in rename_map.items() if k in df.columns})
    df["year"] = year

    required = ["year", "employeeCode", "employeeName", "municipality", "annualTaxAmount"]
    missing = [c for c in required if c not in df.columns]

    if missing:
        raise RuntimeError(f"必要列がありません: {missing}")

    output_file.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(output_file, index=False, encoding="utf-8-sig")

    print(f"CSV created: {output_file}")
    print(f"rows: {len(df)}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, required=True)
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    normalize(Path(args.input), args.year, Path(args.output))


if __name__ == "__main__":
    main()