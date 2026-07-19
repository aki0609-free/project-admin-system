import argparse
import re
import requests
from pathlib import Path

import pandas as pd
import pdfplumber


INSURANCE_TYPE = "HEALTH_INSURANCE"


def download(url: str, path: Path):
    if not url:
        return

    path.parent.mkdir(parents=True, exist_ok=True)

    res = requests.get(url, timeout=60)
    res.raise_for_status()

    path.write_bytes(res.content)


def to_rate_decimal(value):
    if value is None:
        return None

    text = str(value)
    text = text.replace(",", "")
    text = text.replace("％", "%")
    text = text.strip()

    match = re.search(r"(\d+(?:\.\d+)?)\s*%", text)
    if match:
        return round(float(match.group(1)) / 100, 5)

    match = re.search(r"(\d+(?:\.\d+)?)", text)
    if match:
        number = float(match.group(1))

        # 9.85 のような表記なら %
        if number > 1:
            return round(number / 100, 5)

        # 0.0985 のような表記ならそのまま
        return round(number, 5)

    return None


def extract_health_rate(input_file: Path):
    candidates = []

    with pdfplumber.open(input_file) as pdf:
        for page in pdf.pages:
            text = page.extract_text() or ""

            for line in text.splitlines():
                normalized = line.replace("％", "%")

                if "健康保険料率" not in normalized and "介護保険第2号" not in normalized:
                    continue

                rates = re.findall(r"\d+(?:\.\d+)?\s*%", normalized)

                for rate_text in rates:
                    rate = to_rate_decimal(rate_text)

                    if rate is not None and 0.01 <= rate <= 0.15:
                        candidates.append(rate)

            tables = page.extract_tables() or []

            for table in tables:
                for row in table:
                    values = [(v or "").strip() for v in row]
                    joined = " ".join(values).replace("％", "%")

                    if "健康保険" not in joined and "介護保険" not in joined:
                        continue

                    for value in values:
                        rate = to_rate_decimal(value)
                        if rate is not None and 0.01 <= rate <= 0.15:
                            candidates.append(rate)

    if not candidates:
        raise RuntimeError("健康保険料率を抽出できませんでした。PDFレイアウトを確認してください。")

    # 協会けんぽの健康保険料率は 0.09〜0.11 付近が多いので、その範囲を優先
    preferred = [r for r in candidates if 0.08 <= r <= 0.12]

    return preferred[0] if preferred else candidates[0]


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

    total_rate = extract_health_rate(input_file)

    employee_rate = round(total_rate / 2, 5)
    employer_rate = round(total_rate / 2, 5)

    out = pd.DataFrame([
        {
            "insuranceType": INSURANCE_TYPE,
            "year": args.year,
            "employeeRate": employee_rate,
            "employerRate": employer_rate,
        }
    ])

    output_file = Path(args.output)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(output_file, index=False, encoding="utf-8")

    print(f"CSV created: {output_file}")
    print(f"totalRate: {total_rate}")
    print(f"employeeRate: {employee_rate}")
    print(f"employerRate: {employer_rate}")


if __name__ == "__main__":
    main()