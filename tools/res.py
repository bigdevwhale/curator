"""Writes the string resources (English and Russian) from one table, so the two never drift apart.

python tools/res.py
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"

S = [
    ("app_name", "Curator", "Куратор"),
    ("museum_of", "A museum of one life", "Музей одной жизни"),
    ("tab_museum", "Museum", "Музей"),
    ("tab_halls", "Halls", "Залы"),
    ("tab_act", "Act", "Акт"),
    ("settings", "Settings", "Настройки"),
    ("next", "Next", "Дальше"),
    ("ok", "OK", "Хорошо"),
    ("save", "Save", "Сохранить"),
    ("cancel", "Cancel", "Отмена"),
    ("close", "Close", "Закрыть"),
    ("undo", "undo", "вернуть"),
    ("rename", "Rename", "Переименовать"),
    ("accept", "Accept", "Принять"),
    ("on", "on", "да"),
    ("off", "off", "нет"),
    # museum
    ("chronicle", "Chronicle", "Летопись"),
    ("chronicle_sub", "everything the museum has lived through", "всё, что было с музеем"),
    ("in_fonds", "kept", "оставлено"),
    ("exhibits", "best", "лучших"),
    ("freed", "freed", "освобождено"),
    ("wing_next", "Next to build: %1$s. Free %2$s more", "Следующая стройка — %1$s. Осталось освободить %2$s"),
    ("wings_all", "The museum is fully built", "Музей отстроен целиком"),
    ("rank_next", "%2$s more to describe to become %1$s", "До звания «%1$s» осталось описать %2$s"),
    ("partial_t", "The museum sees only the photos you picked", "Музею открыты только выбранные фото"),
    ("partial_b", "To sort the whole gallery, allow access to all photos and videos. Tap here.", "Чтобы разобрать всю галерею, разрешите доступ ко всем фото и видео. Нажмите сюда."),
    ("act_waits", "The act is waiting for your signature", "Акт ждёт вашей подписи"),
    ("otd_t", "From the storeroom", "Из запасника"),
    ("otd_b", "taken on this very day", "снято в этот же день"),
    ("otd_exhibit", "one of your best shots, taken on this very day", "ваш лучший кадр, снятый в этот же день"),
    ("funds", "Special crates", "Особые ящики"),
    ("fund_shots", "Screenshots", "Скриншоты"),
    ("fund_video", "Videos", "Видео"),
    ("fund_chats", "From chats", "Из чатов"),
    ("fund_heavy", "Heavy", "Тяжёлые"),
    ("fund_empty", "all described", "всё описано"),
    ("crates", "Crates of arrivals", "Ящики поступлений"),
    ("by_months", "by month", "по месяцам"),
    ("crate_open", "open", "вскрыть"),
    ("crate_done", "all described", "всё описано"),
    ("crate_left", "%1$s left", "осталось %1$s"),
    ("syncing", "Taking stock of the gallery…", "Принимаем галерею на учёт…"),
    ("no_crates", "No photos yet. The crates will arrive as soon as there are some.", "Фотографий пока нет. Как только появятся, придут и ящики."),
    # crate
    ("crate_no", "Crate No. %1$d", "Ящик № %1$d"),
    ("crate_cards_left", "cards left: %1$d", "осталось карточек: %1$d"),
    ("crate_empty", "Everything in this crate is described.", "В этом ящике всё уже описано."),
    ("expert_looks", "The expert is looking the crate over…", "Эксперт осматривает ящик…"),
    ("expert_t", "Expert report", "Заключение эксперта"),
    ("expert_b", "The expert looked over %1$s and found %2$d rejects. Tap one to save it.", "Эксперт осмотрел %1$s и отложил в брак %2$d. Если что-то жалко, коснитесь — и оно останется."),
    ("expert_off", "Write off the rejects (%1$d)", "Списать брак (%1$d)"),
    ("expert_none", "Keep them all", "Оставить всё"),
    ("expert_all", "I’ll look at each one myself", "Посмотрю всё сам"),
    ("rescued", "saved", "спасён"),
    ("d_shot", "Screenshot", "Скриншот"),
    ("d_blur", "Blurred", "Смазано"),
    ("d_dark", "Too dark", "Темно"),
    ("d_dup", "Duplicate", "Дубль"),
    ("a_del", "write off", "списать"),
    ("a_hall", "album", "в альбом"),
    ("a_ex", "best", "лучшее"),
    ("a_keep", "keep", "оставить"),
    ("s_del", "written off", "списано"),
    ("s_keep", "kept", "оставлено"),
    ("s_hall", "to album", "в альбом"),
    ("s_later", "later", "отложено"),
    ("s_ex", "best", "лучшее"),
    ("gestures", "← write off · → keep · ↓ album · ↑ later · hold ★", "← списать · → оставить · ↓ в альбом · ↑ потом · удержать ★"),
    ("later_note", "Put aside: it comes back at the end of the crate", "Отложено: вернётся в конце ящика"),
    ("nothing_to_undo", "Nothing to undo", "Возвращать нечего"),
    ("series_first", "Choose the original first", "Сначала выберите подлинник"),
    ("series_t", "One thing, %1$d versions", "Похожие кадры: %1$d"),
    ("series_b", "Choose the original: the copies go into the act.", "Выберите подлинник — копии уйдут в акт."),
    ("original", "original", "подлинник"),
    ("keep_all", "Keep all", "Оставить все"),
    ("keep_original", "Keep the original", "Оставить подлинник"),
    ("inv_no", "Inv. No. %1$s", "Инв. № %1$s"),
    ("which_hall", "Which album?", "В какой альбом?"),
    ("new_hall", "New album", "Новый альбом"),
    ("new_hall_t", "New album", "Новый альбом"),
    ("new_hall_hint", "What is it called?", "Как он называется?"),
    ("open_hall", "Create the album", "Создать альбом"),
    ("sum_caps", "Inventory closed", "Опись закрыта"),
    ("sum_t", "The crate is done", "Ящик разобран"),
    ("to_act", "into the act", "в акт"),
    ("to_free", "freed once the act is signed", "освободится после подписи акта"),
    ("sum_ex", "Your new best shots already hang in the main hall and go to this week’s exhibition.", "Новые лучшие кадры уже висят в главном зале и попадут на выставку недели."),
    ("sum_later", "What you put aside is waiting in the crate.", "Отложенное вернётся в конце ящика."),
    ("sum_plain", "No best shots this time. An honest inventory too.", "На этот раз без лучших кадров — бывает и так."),
    ("next_crate", "Next crate", "Следующий ящик"),
    ("go_sign", "Sign the act", "Подписать акт"),
    ("to_museum", "Back to the museum", "В музей"),
    # act
    ("act_form", "Form No. 7", "Форма № 7"),
    ("act_title", "Act of write-off", "Акт списания"),
    ("act_no", "Act No. %1$d", "Акт № %1$d"),
    ("act_empty_sub", "nothing to write off", "списывать нечего"),
    ("act_empty", "Things you write off in the crates wait here for your signature. Until you sign, nothing is deleted.", "Сюда попадает всё, что вы списали в ящиках. Пока акт не подписан, ничего не удаляется."),
    ("act_hint", "Nothing is deleted until you sign. Tap a thing to look at it again or keep it after all.", "Пока акт не подписан, ничего не удаляется. Коснитесь предмета, чтобы рассмотреть его или всё-таки оставить."),
    ("curator_sign", "Curator", "Хранитель"),
    ("curator_name", "the Curator", "Хранитель"),
    ("sign_free", "Sign: free %1$s", "Подписать — освободить %1$s"),
    ("sign_hint", "The phone will ask once for all of them.", "Телефон спросит разрешения один раз — за всё сразу."),
    ("act_signed", "Act signed", "Акт подписан"),
    ("return_fonds", "Don’t delete", "Не удалять"),
    ("leave_in_act", "Leave in the act", "Оставить в акте"),
    ("open_gallery", "Open in the gallery", "Открыть в галерее"),
    # halls
    ("halls_caps", "Your albums", "Ваши альбомы"),
    ("halls_title", "Halls", "Залы"),
    ("show_caps", "Exhibition of the week", "Выставка недели"),
    ("show_in_hall", "Exhibition of the week · in the lobby", "Выставка недели · в вестибюле"),
    ("show_in_rotunda", "Exhibition of the week · under the rotunda", "Выставка недели · под куполом ротонды"),
    ("show_empty", "Nothing hangs yet this week. Hold a card in a crate to mark it as one of the best.", "На этой неделе ещё ничего не повесили. Удержите карточку в ящике — и она попадёт в лучшее."),
    ("show_rename", "Rename", "Переименовать"),
    ("show_share", "Send the poster", "Отправить афишу"),
    ("past_shows", "Past exhibitions", "Прошлые выставки"),
    ("plan", "Plan", "План"),
    ("main_hall", "Main hall", "Главный зал"),
    ("halls_hint", "Every hall is an album. Swipe a card down in a crate to put it into one.", "Каждый зал — это альбом. Смахните карточку в ящике вниз, чтобы положить её в альбом."),
    ("hall_empty", "The album is empty for now. Swipe a card down in any crate and it lands here.", "Альбом пока пуст. Смахните карточку вниз в любом ящике — и она окажется здесь."),
    ("main_empty", "No best shots yet. Hold a card in a crate to mark it as one.", "Лучших кадров пока нет. Удержите карточку в ящике — и она попадёт в лучшее."),
    ("folder_t", "Show it in the phone's gallery", "Показать в галерее телефона"),
    ("folder_b", "The photos move to %1$s, and the phone's gallery shows this album.", "Фото переедут в папку %1$s — и галерея телефона покажет этот альбом."),
    ("folder_set", "Lives in the folder %1$s: the gallery shows it as an album.", "Лежит в папке %1$s — галерея показывает его как альбом."),
    ("folder_go", "Move into the folder", "Переложить в папку"),
    ("folder_again", "Move the new ones too", "Переложить новые фото"),
    ("close_hall", "Delete the album", "Удалить альбом"),
    ("close_hall_hint", "The photos stay in the gallery; only the album goes.", "Фото останутся в галерее, исчезнет только альбом."),
    ("unexhibit", "Remove from best", "Убрать из лучшего"),
    ("out_of_hall", "Take out of the album", "Убрать из альбома"),
    # ceremonies & chronicle
    ("freed_caps", "%1$s freed", "Освобождено %1$s"),
    ("ribbon", "Cut the ribbon", "Перерезать ленточку"),
    ("rank_caps", "A new rank", "Новое звание"),
    ("ev_founded", "The museum was founded.", "Основан музей."),
    ("ev_crate", "Crate “%1$s” opened:", "Вскрыт ящик «%1$s»:"),
    ("ev_act", "Act of write-off signed: %1$s, %2$s freed.", "Подписан акт списания: %1$s, освобождено %2$s."),
    ("ev_hall", "Album “%1$s” created.", "Создан альбом «%1$s»."),
    ("ev_rank", "Rank given: %1$s.", "Присвоено звание: %1$s."),
    # onboarding & access
    ("ob_1t", "Your gallery is a museum storeroom", "Ваша галерея — запасник музея"),
    ("ob_1b", "Thousands of photos lie in crates, one per month. Open one at a time: twenty-odd cards, a couple of minutes. The space you free builds the museum, wing by wing.", "Тысячи фото разложены по ящикам — по ящику на месяц. Разбирайте их по одному: пара десятков карточек, пара минут. А освободившееся место достраивает музей, крыло за крылом."),
    ("tr_start", "Learn on a training crate", "Потренироваться на учебном ящике"),
    ("tr_skip", "Skip", "Пропустить"),
    ("tr_caps", "Training crate · %1$d of %2$d", "Учебный ящик · %1$d из %2$d"),
    ("tr_up_t", "Swipe up", "Смахните вверх"),
    ("tr_up_b", "Not sure? Up puts it aside: it comes back later.", "Не можете решить? Вверх — отложить, карточка вернётся позже."),
    ("tr_right_t", "Swipe right", "Смахните вправо"),
    ("tr_right_b", "A good one. Right means keep: it stays in the gallery as it was.", "Хороший кадр. Вправо — оставить: он останется в галерее, как и был."),
    ("tr_down_t", "Swipe down", "Смахните вниз"),
    ("tr_down_b", "Down keeps it too, and puts it into an album: a hall of the museum.", "Вниз — оставить и сразу положить в альбом. Альбомы здесь называются залами."),
    ("tr_left_t", "Swipe left", "Смахните влево"),
    ("tr_left_b", "A blurred shot. Left means write off: it goes into the act.", "Смазанный кадр. Влево — списать: он уйдёт в акт."),
    ("tr_hold_t", "Press and hold", "Нажмите и удержите"),
    ("tr_hold_b", "Hold your finger on your favourite shot: it joins the best and goes to the exhibition of the week.", "Удержите палец на лучшем кадре — он попадёт в лучшее и на выставку недели."),
    ("tr_pick_t", "Pick the original", "Выберите подлинник"),
    ("tr_pick_b", "Four near-identical shots come as one card. Tap the best: the copies go into the act.", "Четыре почти одинаковых кадра приходят одной карточкой. Коснитесь лучшего — копии уйдут в акт."),
    ("tr_miss", "Not that way. Try again.", "Не туда. Попробуйте ещё раз."),
    ("ob_private", "Photos never leave the phone: the app has no internet access at all.", "Фото никуда не уходят с телефона: у приложения даже нет доступа в интернет."),
    ("set_theme", "Theme", "Тема"),
    ("theme_system", "System", "Как в системе"),
    ("theme_light", "Light", "Светлая"),
    ("theme_dark", "Dark", "Тёмная"),
    ("set_training", "Go through the training again", "Пройти обучение заново"),
    ("ob_3t", "Nothing is deleted without your signature", "Без вашей подписи ничего не удаляется"),
    ("ob_3b", "Written-off things wait in the act. You can look at them again or take them back. Only your signature deletes them, with one question from the phone for all of them.", "Списанное сначала ждёт в акте: его можно пересмотреть или вернуть. Удаляется оно только после вашей подписи, и телефон спросит разрешение один раз на всё сразу."),
    ("ob_go", "Open Curator", "Открыть Куратор"),
    ("closed_t", "The storeroom is locked", "Запасник заперт"),
    ("closed_b", "The museum needs to see your photos and videos to sort them. They are looked at on the phone only: the app has no internet access at all.", "Чтобы разбирать фото и видео, музею нужно их видеть. Смотрит он их только на телефоне — у приложения даже нет доступа в интернет."),
    ("closed_ask", "Allow access", "Разрешить доступ"),
    ("closed_settings", "Open the app’s settings", "Открыть настройки приложения"),
    # settings
    ("set_crate", "Crate size", "Размер ящика"),
    ("set_crate_hint", "How many things go into one crate.", "Сколько предметов в одном ящике."),
    ("set_expert", "The expert looks for", "Эксперт ищет"),
    ("set_expert_hint", "What gets put aside before you start. You always confirm.", "Что откладывать заранее. Последнее слово всегда за вами."),
    ("set_remind", "Morning note", "Утренняя записка"),
    ("set_remind_hint", "At ten, if crates are waiting: one crate is a couple of minutes.", "В десять утра, если ящики ждут: один ящик — пара минут."),
    ("set_lang", "Language", "Язык"),
    ("lang_system", "System", "Как в системе"),
    ("lang_ru", "Русский", "Русский"),
    ("lang_en", "English", "English"),
    ("privacy", "Privacy", "Приватность"),
    ("privacy_text", "Photos never leave the phone. The app has no internet permission; the expert works on small thumbnails right here.", "Фото никуда не уходят с телефона. У приложения нет доступа в интернет, эксперт смотрит маленькие копии прямо здесь."),
    ("community", "StillCraft", "StillCraft"),
    ("community_text", "Curator is one of the StillCraft apps. Tell us what’s broken or what’s missing.", "Куратор — одно из приложений StillCraft. Расскажите, что сломалось или чего не хватает."),
    ("community_bug", "Report a bug", "Сообщить об ошибке"),
    ("community_bug_sub", "opens the bot in Telegram", "откроется бот в Telegram"),
    ("community_idea", "Suggest an idea", "Предложить идею"),
    ("community_idea_sub", "what the museum is missing", "чего не хватает музею"),
    ("community_follow", "Follow updates", "Следить за обновлениями"),
    ("community_follow_sub", "a message when a new version is out", "сообщение, когда выйдет новая версия"),
    ("community_chat", "StillCraft chat", "Чат StillCraft"),
    ("community_chat_sub", "people who use these apps", "люди, которые пользуются этими приложениями"),
    ("about", "About", "О приложении"),
    ("about_text", "Curator %1$s. Free, open source, no ads.", "Куратор %1$s. Бесплатно, с открытым кодом, без рекламы."),
    ("photo_credits", "Training crate photos: Steven Lewis, Roberto Nickson, Matthew Wiebe, Jeff Sheldon, Alexey Topolyanskiy and E+N Photographies on Unsplash.", "Фото в учебном ящике: Steven Lewis, Roberto Nickson, Matthew Wiebe, Jeff Sheldon, Alexey Topolyanskiy и E+N Photographies, Unsplash."),
    ("source", "Source code", "Исходный код"),
    ("n_channel", "Morning note", "Утренняя записка"),
    ("n_title", "Crate No. %1$d is waiting", "Ящик № %1$d ждёт вскрытия"),
]

# name, (en one, en other), (ru one, few, many)
P = [
    ("things", ("%1$s thing", "%1$s things"), ("%1$s предмет", "%1$s предмета", "%1$s предметов")),
    ("exhibits", ("%1$s best shot", "%1$s best shots"), ("%1$s лучший кадр", "%1$s лучших кадра", "%1$s лучших кадров")),
    ("described", ("%1$s described", "%1$s described"), ("описан %1$s предмет", "описано %1$s предмета", "описано %1$s предметов")),
    ("ev_described", ("%1$d thing described.", "%1$d things described."), ("описан %1$d предмет.", "описано %1$d предмета.", "описано %1$d предметов.")),
    ("halls", ("%1$d hall", "%1$d halls"), ("%1$d зал", "%1$d зала", "%1$d залов")),
    ("days", ("%1$d day", "%1$d days"), ("%1$d день", "%1$d дня", "%1$d дней")),
    ("years_ago", ("%1$d year ago", "%1$d years ago"), ("%1$d год назад", "%1$d года назад", "%1$d лет назад")),
    ("moved", ("%1$d thing moved to Pictures/%2$s. The phone’s gallery shows it as an album now.", "%1$d things moved to Pictures/%2$s. The phone’s gallery shows them as an album now."),
               ("В папку Pictures/%2$s перенесён %1$d предмет. Теперь галерея телефона показывает его как альбом.",
                "В папку Pictures/%2$s перенесено %1$d предмета. Теперь галерея телефона показывает их как альбом.",
                "В папку Pictures/%2$s перенесено %1$d предметов. Теперь галерея телефона показывает их как альбом.")),
    ("n_body", ("%1$d thing still in crates. One crate is a couple of minutes.", "%1$d things still in crates. One crate is a couple of minutes."),
               ("В ящиках ещё %1$d предмет. Один ящик — пара минут.", "В ящиках ещё %1$d предмета. Один ящик — пара минут.", "В ящиках ещё %1$d предметов. Один ящик — пара минут.")),
]

A = [
    ("ranks", ["Attendant", "Junior researcher", "Keeper", "Senior keeper", "Chief curator", "Museum director"],
              ["Смотритель", "Младший научный сотрудник", "Хранитель", "Старший хранитель", "Главный хранитель", "Директор музея"]),
    ("rank_text", ["You sit in the corner of the hall and watch.", "A hundred things described. They gave you a desk and a lamp.",
                   "Five hundred things. You have keys to the storeroom now.", "Two thousand things. The young ones come to you for advice.",
                   "Five thousand things. Nothing is written off without your signature.", "Twelve thousand things. The museum is yours: from the cellar to the dome."],
                  ["Сидите в углу зала и присматриваете.", "Описана первая сотня предметов. Вам выделили стол и лампу.",
                   "Пятьсот предметов. Теперь у вас ключи от запасника.", "Две тысячи предметов. Молодые сотрудники ходят к вам за советом.",
                   "Пять тысяч предметов. Без вашей подписи ничего не списывается.", "Двенадцать тысяч предметов. Музей ваш — от подвала до купола."]),
    ("wings", ["the main block", "the wing", "the gallery", "the rotunda", "the observatory", "the sculpture garden"],
              ["главный корпус", "флигель", "галерея", "ротонда", "обсерватория", "сад скульптур"]),
    ("wing_opened", ["The main block opened.", "The wing opened.", "The gallery opened.", "The rotunda opened.", "The observatory opened.", "The sculpture garden opened."],
                    ["Открыт главный корпус.", "Открыт флигель.", "Открыта галерея.", "Открыта ротонда.", "Открыта обсерватория.", "Открыт сад скульптур."]),
    ("wing_text", ["The museum begins.", "It was getting crowded in the main block. Now the halls have room to grow.",
                   "A long enfilade with windows on the garden. The best things move in here.", "Under the dome there is room for the exhibition of the week.",
                   "From up here you can see all the years at once.", "The museum is built. All that’s left is to walk in it."],
                  ["Музей начинается.", "В главном корпусе стало тесно. Теперь залам есть куда расти.",
                   "Длинная анфилада с окнами в сад. Сюда переезжает всё лучшее.", "Под куполом нашлось место для выставки недели.",
                   "Отсюда видны сразу все годы.", "Музей отстроен. Осталось только гулять по нему."]),
    ("hall_ideas", ["Family", "Travels", "Friends", "Children", "Home", "Pets", "Food", "Nature", "Holidays", "Work", "Documents", "Recipes"],
                   ["Семья", "Путешествия", "Друзья", "Дети", "Дом", "Питомцы", "Еда", "Природа", "Праздники", "Работа", "Документы", "Рецепты"]),
]


def esc(s):
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return s.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')


def write(lang, col):
    out = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- Generated by tools/res.py: edit the table there. -->", "<resources>"]
    for name, en, ru in S:
        v = en if col == 0 else ru
        extra = ' translatable="false"' if name in ("lang_ru", "lang_en", "community") and col == 0 else ""
        if col == 1 and extra:
            continue
        out.append(f'    <string name="{name}"{extra}>{esc(v)}</string>')
    for name, en, ru in P:
        out.append(f'    <plurals name="{name}">')
        if col == 0:
            out.append(f'        <item quantity="one">{esc(en[0])}</item>')
            out.append(f'        <item quantity="other">{esc(en[1])}</item>')
        else:
            for q, v in zip(("one", "few", "many", "other"), (ru[0], ru[1], ru[2], ru[2])):
                out.append(f'        <item quantity="{q}">{esc(v)}</item>')
        out.append("    </plurals>")
    for name, en, ru in A:
        out.append(f'    <string-array name="{name}">')
        for v in (en if col == 0 else ru):
            out.append(f"        <item>{esc(v)}</item>")
        out.append("    </string-array>")
    out.append("</resources>")
    d = RES / ("values" if lang == "en" else "values-ru")
    d.mkdir(parents=True, exist_ok=True)
    (d / "strings.xml").write_text("\n".join(out) + "\n", encoding="utf-8")


if __name__ == "__main__":
    for a in A:
        assert len(a[1]) == len(a[2]), a[0]
    write("en", 0)
    write("ru", 1)
    print(len(S), "strings,", len(P), "plurals,", len(A), "arrays")
