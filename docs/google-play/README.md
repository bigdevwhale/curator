# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Curator-v<версия>.aab`.
  Первая версия для Play — **следующий релиз после v1.1.1** (v1.1.1 и раньше вышли без AAB). Секреты `SIGNING_*`
  в репозитории заданы; без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках перестаёт работать. Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.curator`.
  2. **Select key** → ключ с SHA-256 `a1576ad7c5c2aad634e1dd742f62a31f253f8f86f8243c3b7eb3fbb3942562da`
     (сертификат CN=Curator, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.curator` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория (на двух языках).
  Ссылка: `https://github.com/bigdevwhale/curator/blob/main/PRIVACY.md`.
- **Видео для декларации фото и видео** (раздел 4) не требуется, но Play может попросить показать, где
  нужен доступ ко всей галерее. На всякий случай: 20–30 секунд записи экрана — эксперт, ящик с карточками,
  свайп влево, подпись акта и системный диалог удаления.

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Curator: Gallery Cleaner` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать (приложение только портретное) | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из рендеров README (`docs/screenshots/<язык>`, демо-галерея
из стоковых фото Unsplash): Play не берёт картинки длиннее двух ширин, поэтому полные экраны уходят
за нижний край под подписью, а у листов (заключение эксперта, новое крыло) оставлена только карточка.
Как пересобрать:

```bash
./gradlew testDebugUnitTest -Prender --tests '*RenderScreens*' && python tools/shots.py
python tools/play.py   # музей, карточка, эксперт, акт, крыло, залы, зал, летопись
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **Photography** |
| Tags (до 5) | выберите из списка Play ближайшие: Gallery, Photo & video, Storage cleaner, Organizer, Tools |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/curator/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Photography — там ищут «галерея», «фото», «очистка фото». Альтернатива — **Tools**
(рядом с очистителями памяти), но там шумно и много рекламных «ускорителей». Категорию можно сменить позже.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- На все вопросы — **No**: приложение показывает только фото самого пользователя; своего контента с насилием,
  страхом, сексом, лексикой нет; азартных игр и покупок нет; общения между пользователями нет; местоположение
  не передаётся; браузера и поиска нет. Отправка афиши через системное «Поделиться» — не обмен контентом
  внутри приложения.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over** (политика: приложение не для детей младше 13).
  Младше 13 не отмечать, иначе действует политика Families.
- *Could your store listing unintentionally appeal to children?* → **No**.

**News apps** — **No.**

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**
- У приложения нет разрешения `INTERNET`: с телефона не уходит вообще ничего.
  - *Does your app collect or share any of the required user data types?* → **No**.
  - Итог в карточке: «No data collected», «No data shared with third parties».
- Отправка афиши выставки — по нажатию пользователя через системное меню «Поделиться»; по правилам Play
  передача, которую начинает сам пользователь, не считается «sharing». Системное резервное копирование Android
  делает система, а не приложение, его не декларируем.

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — ничего не отмечать.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Photo and video permissions** (появится после загрузки AAB: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`)
- Play разрешает их только приложениям, которым широкий доступ к фото нужен для основной функции; разбор
  и очистка галереи — ровно этот случай (одноразовый выбор через Photo Picker не подходит: нужно видеть
  всю галерею по месяцам, находить дубли и серии и удалять). Частичный доступ (`READ_MEDIA_VISUAL_USER_SELECTED`)
  поддержан: приложение работает с выбранными фото и предлагает открыть остальные.
- Основная функция: **Gallery / photo management** (если такого пункта нет — *Other*). Описание:
  > Curator is a gallery cleaner: it shows every photo and video on the phone as cards, one month at a time,
  > so the user can keep, delete, sort into albums or mark the best. To find blurred, dark and duplicate shots
  > and series of near-identical photos, and to show how much space each month takes, it needs to read the whole
  > gallery, not single picked items. Deletion and moving always go through Android's own confirmation dialog
  > (MediaStore.createDeleteRequest / createWriteRequest). The app has no INTERNET permission; photos never
  > leave the device.

**Прочие разрешения** — особых деклараций не нужно: `POST_NOTIFICATIONS` (утренняя записка) не
чувствительное; `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE` и общий `FOREGROUND_SERVICE`
добавляет WorkManager (типизированных `FOREGROUND_SERVICE_*`, которые требуют декларации, нет). Если Play всё же
покажет форму **Foreground service permissions** — ответ: приложение не запускает фоновых служб, разрешение
пришло из библиотеки WorkManager. `MANAGE_EXTERNAL_STORAGE`, местоположения и точных будильников нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Curator-v<версия>.aab` из GitHub Release.
3. Release name: `<версия>`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели;
   с декларацией доступа к фото и видео — может дольше.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; доступ к галерее — системный диалог фото и видео; подпись акта — системный диалог удаления.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.curator`.
