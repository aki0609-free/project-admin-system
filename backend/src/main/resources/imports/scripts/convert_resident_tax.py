import argparse
import csv
from pathlib import Path


FISCAL_MONTHS = (6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4, 5)


def normalize_header(value: str) -> str:
    return value.replace(" ", "").replace("　", "").strip()


def read_rows(input_file: Path):
    last_error = None
    for encoding in ("utf-8-sig", "cp932"):
        try:
            with input_file.open("r", encoding=encoding, newline="") as source:
                reader = csv.DictReader(source)
                if reader.fieldnames is None:
                    raise RuntimeError("ヘッダー行がありません。")
                headers = [normalize_header(header) for header in reader.fieldnames]
                rows = []
                for source_row in reader:
                    rows.append({
                        headers[index]: value.strip()
                        for index, value in enumerate(source_row.values())
                    })
                return rows
        except UnicodeDecodeError as error:
            last_error = error
    raise RuntimeError("CSVの文字コードを判定できませんでした。") from last_error


def first_value(row, *headers):
    for header in headers:
        value = row.get(normalize_header(header), "").strip()
        if value:
            return value
    return ""


def to_int(value: str, label: str) -> int:
    normalized = value.replace(",", "").replace("円", "").strip()
    if not normalized:
        raise RuntimeError(f"{label}が空です。")
    try:
        return int(normalized)
    except ValueError as error:
        raise RuntimeError(f"{label}が数値ではありません: {value}") from error


def expand_row(row, default_fiscal_year: int):
    employee_id = to_int(
        first_value(row, "employeeId", "社員ID", "従業員ID"),
        "employeeId",
    )
    fiscal_year_text = first_value(row, "fiscalYear", "年度")
    fiscal_year = to_int(fiscal_year_text, "fiscalYear") if fiscal_year_text else default_fiscal_year

    normalized_month = first_value(row, "month", "月")
    normalized_amount = first_value(row, "taxAmount", "税額")
    if normalized_month and normalized_amount:
        month = to_int(normalized_month, "month")
        if month < 1 or month > 12:
            raise RuntimeError(f"monthは1〜12で指定してください: {month}")
        return [(employee_id, fiscal_year, month, to_int(normalized_amount, "taxAmount"))]

    month_values = {}
    for month in FISCAL_MONTHS:
        value = first_value(row, f"{month}月", f"month{month}")
        if value:
            month_values[month] = to_int(value, f"{month}月")

    if len(month_values) == 12:
        return [(employee_id, fiscal_year, month, month_values[month]) for month in FISCAL_MONTHS]

    june_amount = first_value(row, "6月", "juneAmount")
    july_onward = first_value(row, "7月以降", "monthlyTaxAmountFromJuly")
    monthly_amount = first_value(row, "月額", "monthlyTaxAmount")

    if monthly_amount:
        amount = to_int(monthly_amount, "月額")
        return [(employee_id, fiscal_year, month, amount) for month in FISCAL_MONTHS]

    if june_amount and july_onward:
        june = to_int(june_amount, "6月")
        remaining = to_int(july_onward, "7月以降")
        return [
            (employee_id, fiscal_year, month, june if month == 6 else remaining)
            for month in FISCAL_MONTHS
        ]

    raise RuntimeError(
        "月別税額が不足しています。12か月分、月額、または6月＋7月以降を指定してください。"
    )


def normalize(input_file: Path, fiscal_year: int, output_file: Path):
    source_rows = read_rows(input_file)
    output_rows = []
    for row_number, row in enumerate(source_rows, start=2):
        try:
            output_rows.extend(expand_row(row, fiscal_year))
        except RuntimeError as error:
            raise RuntimeError(f"{row_number}行目: {error}") from error

    if not output_rows:
        raise RuntimeError("変換対象データがありません。")

    output_file.parent.mkdir(parents=True, exist_ok=True)
    with output_file.open("w", encoding="utf-8-sig", newline="") as destination:
        writer = csv.writer(destination)
        writer.writerow(("employeeId", "fiscalYear", "month", "taxAmount"))
        writer.writerows(output_rows)

    print(f"CSV created: {output_file}")
    print(f"rows: {len(output_rows)}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, required=True)
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    normalize(Path(args.input), args.year, Path(args.output))


if __name__ == "__main__":
    main()
