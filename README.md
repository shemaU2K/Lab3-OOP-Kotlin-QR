# QRVault — Android QR Code Scanner

Android-застосунок для сканування QR-кодів, декодування структурованих даних та збереження історії у локальній базі даних Room.

## Стек технологій
- **Мова**: Kotlin 2.0
- **UI**: Jetpack Compose + Material 3 (адаптивна світла/темна тема)
- **Сканування**: CameraX + Google ML Kit Barcode Scanning
- **Локальна БД**: Room (SQLite) + Coroutines Flow
- **Логування**: Timber (`QrDebugTree` для розробки, `QrReleaseTree` з маскуванням паролів для релізу)
- **Локалізація (i18n)**: `res/values/strings.xml` (EN) та `res/values-uk/strings.xml` (UK) + `locales_config.xml` + `AppCompatDelegate.setApplicationLocales`

## Запуск в Android Studio
1. Розпакуйте архів та відкрийте директорію у **Android Studio Ladybug+**.
2. Дочекайтеся синхронізації Gradle Sync (JDK 17).
3. Запустіть модуль `:app` на фізичному пристрої або емуляторі (API 26+).

