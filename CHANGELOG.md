# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.1.0] - 2026-11-08

### Added
- A crate gone through can be looked at again: «look again» on a finished month opens what was kept there, to sort once more (#1).
- «Write them all off» for a series of near-identical shots, with a second tap, since none will be left (#1).

### Fixed
- Signing an act sometimes counted nothing: if Android rebuilt the screen behind the delete dialog, the files went but the freed space and the museum's growth stayed where they were (#2). Now it is counted by what the gallery really lost, even after the app was closed. A file the gallery gives no size for gets it from the file itself.
- The crate's summary says the space is freed once the act is signed.

<!-- tg
Исправлено: после подписи акта освобождённое место иногда не засчитывалось – теперь считается всегда.
Новое: разобранный месяц можно «пересмотреть» и разобрать оставленное ещё раз, а серию похожих кадров – «списать все».
-->

## [1.0.4] - 2026-11-01

### Changed
- Write off is now a swipe left, «later» a swipe up.
- The museum door is drawn in lines like the rest of the front, not filled orange, and fills the middle bay: the column that ended in a stub over it is gone.

### Fixed
- The stamps were still cut at the corners while they faded in.
- After a card flies off, the next one rises smoothly instead of jumping into place, and the card under it no longer shows up empty for a moment.

<!-- tg
Списать — теперь свайп влево, отложить — вверх.
Карточки сменяются плавнее, штампы больше не обрезаны.
Дверь музея больше не оранжевая.
-->

## [1.0.3] - 2026-11-01

### Changed
- A video plays right in the app instead of opening another app.
- «Экспонат» is now «лучшее» (and «exhibit» is now «best»): the hold gesture, the ★ button, its stamp and the counts.

### Fixed
- The swipe stamps («ОСТАВЛЕНО», «СПИСАНО»…) were cut at the corners.

<!-- tg
Видео теперь играет прямо в Кураторе, без перехода в другое приложение.
Штампы на карточках больше не обрезаны по краям.
Вместо непонятного «экспонат» теперь «лучшее».
-->

## [1.0.2] - 2026-11-01

### Changed
- The last onboarding button says «Открыть Куратор».

<!-- tg skip -->

## [1.0.1] - 2026-11-01

### Fixed
- A card could keep showing the previous photo when the next one took its place (the training crate showed the same shot in every lesson).

<!-- tg skip -->

## [1.0.0] - 2026-11-01

### Added
- Crates by month and special collections (screenshots, videos, chats, heavy files).
- The expert: blurred, dark, screenshots and duplicates put aside on the phone; series of near-identical shots as one card.
- Swipes and buttons: write off, keep, put into an album, put aside, hold for an exhibit; undo.
- The act of write-off with one system dialog for the whole act.
- Halls as albums, with a move into a real `Pictures/<hall>` folder.
- Light and dark themes; a training crate that teaches every gesture before the gallery is opened.
- Wings that open with the freed space, ranks, the exhibition of the week with a poster to send, "from the storeroom", the chronicle.
- An optional morning note. English and Russian.

<!-- tg
Куратор — разбор галереи как работа хранителя музея: ящики по месяцам, по одному жесту на фото.
Эксперт заранее откладывает смазанное, скриншоты и дубли, серии похожих кадров приходят одной карточкой.
Ничего не удаляется, пока вы не подпишете акт, а освобождённое место достраивает музей — крыло за крылом.
Светлая и тёмная темы, обучение на учебном ящике, без интернета.
-->
