#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
maven="${MVN_BIN:-./mvnw}"
source_file='src/main/java/com/autoservice/service/CatalogService.java'
test_case='CatalogServiceTest#update_existingService_updatesFieldsAndSavesSameEntity'
proof='docs/mutation-evidence'
mkdir -p "$proof"

if [ "$(grep -Fc 'mapper.update(request, item);' "$source_file")" != '1' ]; then
    printf 'Не знайдено очікуваний рядок для перевірки мутації.\n'
    exit 1
fi
original="$(mktemp)"
cp "$source_file" "$original"
restore() {
    cp "$original" "$source_file"
    unlink "$original"
}
trap restore EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

printf '1/4: Тест із правильним кодом...\n'
"$maven" -B -ntp "-Dtest=$test_case" test > "$proof/01-baseline.log" 2>&1

perl -0pi -e 's/mapper\.update\(request, item\);/\/\/ mutation check: mapping update omitted/' "$source_file"
git diff -- "$source_file" > "$proof/mutation.diff"
printf '2/4: Той самий тест із навмисною помилкою (очікуємо падіння)...\n'
set +e
"$maven" -B -ntp "-Dtest=$test_case" test > "$proof/02-mutated.log" 2>&1
mutation_exit=$?
set -e
if [ "$mutation_exit" -eq 0 ]; then
    printf 'Тест не виявив мутацію. Перевірка не пройдена.\n'
    exit 1
fi
report='target/surefire-reports/TEST-com.autoservice.service.CatalogServiceTest.xml'
if [ ! -f "$report" ] || ! grep -q 'failures="1"' "$report" || ! grep -q 'errors="0"' "$report"; then
    printf 'Запуск завершився не очікуваним assertion failure. Перевірте 02-mutated.log.\n'
    exit 1
fi
cp 'target/surefire-reports/com.autoservice.service.CatalogServiceTest.txt' "$proof/02-mutated-test.txt"
cp "$original" "$source_file"

printf '3/4: Код відновлено, повторюємо тест...\n'
"$maven" -B -ntp "-Dtest=$test_case" test > "$proof/03-restored.log" 2>&1
printf '4/4: Повний набір unit-тестів і перевірка покриття...\n'
"$maven" -B -ntp clean verify > "$proof/04-full-verification.log" 2>&1
cmp -s "$original" "$source_file"
awk -F, 'NR > 1 { missed += $8; covered += $9; if ($3 == "CatalogService") { sm = $8; sc = $9 } } END { printf "Overall line coverage: %.2f%% (%d/%d)\n", 100 * covered / (covered + missed), covered, covered + missed; printf "CatalogService line coverage: %.2f%% (%d/%d)\n", 100 * sc / (sc + sm), sc, sc + sm }' target/site/jacoco/jacoco.csv > "$proof/coverage-summary.txt"
printf 'Baseline: PASS\nMutation: detected (1 assertion failure, no test errors)\nRestored: PASS\nFull verify: PASS\nOriginal production source: restored byte-for-byte\n' > "$proof/RESULT.txt"
cat "$proof/RESULT.txt"
cat "$proof/coverage-summary.txt"
printf '\nДокази: docs/mutation-evidence/\nЗвіт покриття: target/site/jacoco/index.html\n'
