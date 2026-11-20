"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/*.jpg (1080 px wide renders of the demo, see tools/shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen is cut short under its caption:
full screens keep their top and run off the bottom edge, sheets (the expert, a new wing) keep only their card.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONTS = ROOT / "app/src/main/res/font"

# The icon's colours: the green panelled wall, its darker battens, the stone, the lit door, the bronze disc.
GREEN = (0x1D, 0x3A, 0x31)
GREEN_DEEP = (0x14, 0x2A, 0x23)
STONE = (0xE2, 0xD5, 0xB8)
GOLD = (0xF3, 0xC7, 0x6A)
BRONZE = (0xC4, 0x9A, 0x5C)
INK = (0x0C, 0x1A, 0x15)

# The order they appear in the store, how to cut the screen, and the caption: a stone line and a gold one.
# "top" keeps the top of the screen; (top, bottom) are the rows of a sheet's card, and only the card is kept.
PAGES = [
    ("1-museum", "top", ("Your gallery is", "a museum storeroom"), ("Ваша галерея —", "запасник музея")),
    ("3-card", "top", ("One gesture", "for every photo"), ("Один жест", "на каждое фото")),
    ("2-expert", (492, 1850), ("The expert finds", "blurred shots and copies"), ("Эксперт находит", "смазанное и дубли")),
    ("5-act", "top", ("Nothing is deleted", "until you sign"), ("Ничего не удаляется,", "пока вы не подпишете")),
    ("9-wing", (622, 1720), ("Freed space", "builds the museum"), ("Свободное место", "строит музей")),
    ("4-halls", "top", ("Halls and", "the exhibition of the week"), ("Залы и выставка", "недели")),
    ("6-hall", "top", ("A hall becomes", "a real album"), ("Зал становится", "настоящим альбомом")),
    ("7-chronicle", "top", ("The chronicle", "of your museum"), ("Летопись", "вашего музея")),
]


def font(size, face="cormorant_bold"):
    return ImageFont.truetype(str(FONTS / f"{face}.ttf"), size)


def fit(text, size, width, face="cormorant_bold"):
    while font(size, face).getlength(text) > width:
        size -= 2
    return size


def panels(d, w, h, step=152):
    """The dark battens of the icon's wall, running the whole height."""
    for x in range(step // 3, w, step):
        d.rectangle((x, 0, x + 12, h), fill=GREEN_DEEP)


def board(w, h):
    g = Image.new("RGB", (w, h), GREEN)
    panels(ImageDraw.Draw(g), w, h)
    return g.convert("RGBA")


def lay(g, card, x, y):
    """A screen on the wall: a soft shadow and a thin bronze frame, like a picture in a hall."""
    sh = Image.new("RGBA", (card.width + 80, card.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rectangle((40, 54, 40 + card.width, 54 + card.height), fill=(*INK, 170))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(20)), (x - 40, y - 40))
    g.alpha_composite(card.convert("RGBA"), (x, y))
    ImageDraw.Draw(g).rectangle((x - 8, y - 8, x + card.width + 7, y + card.height + 7), outline=BRONZE, width=8)


def screenshot(src, cut, caption):
    W, H = 1080, 1920
    g = board(W, H)
    d = ImageDraw.Draw(g)
    title = font(min(fit(line, 92, W - 120) for line in caption))
    d.text((W / 2, 128), caption[0], font=title, fill=STONE, anchor="mm")
    d.text((W / 2, 228), caption[1], font=title, fill=GOLD, anchor="mm")
    im = Image.open(src).convert("RGB")
    top = 330
    if cut == "top":
        w = 840
        im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
        # The screen runs off the bottom edge, like a hall that goes on.
        lay(g, im.crop((0, 0, w, H - top + 20)), (W - w) // 2, top)
    else:
        # A sheet: only its card, without the dimmed screen behind it, larger and centred in the space.
        card = im.crop((24, cut[0], im.width - 24, cut[1]))
        w = 960
        card = card.resize((w, round(card.height * w / card.width)), Image.LANCZOS)
        room = H - top - 60
        if card.height > room:
            card = card.crop((0, 0, w, room))
        lay(g, card, (W - w) // 2, top + (room - card.height) // 2)
    return g.convert("RGB")


def icon(size):
    """The README logo has rounded corners; each column of the wall is carried into them, so Play's mask finds a full square."""
    im = Image.open(ROOT / "docs/logo.png").convert("RGBA")
    w, h = im.size
    px = im.load()
    for x in range(w):
        wall = px[x, h // 2]
        for y in range(h):
            r, g_, b, a = px[x, y]
            if a < 255:
                k = a / 255
                px[x, y] = (round(r * k + wall[0] * (1 - k)), round(g_ * k + wall[1] * (1 - k)), round(b * k + wall[2] * (1 - k)), 255)
    return im.convert("RGB").resize((size, size), Image.LANCZOS)


def feature(lang):
    W, H = 1024, 500
    g = board(W, H)
    d = ImageDraw.Draw(g)
    # The museum front with its stats, from the top of the museum screen under the header.
    front = Image.open(SHOTS / lang / "1-museum.jpg").convert("RGB").crop((0, 260, 1080, 1360))
    front = front.resize((round(front.width * 380 / front.height), 380), Image.LANCZOS)
    lay(g, front, W - front.width - 56, 60)
    temple = Image.open(ROOT / "docs/logo.png").convert("RGBA").crop((80, 104, 432, 392))
    temple = temple.resize((temple.width * 2 // 5, temple.height * 2 // 5), Image.LANCZOS)
    # Only the stone and the door, not the wall behind them, so the battens don't double.
    mask = Image.new("L", temple.size, 0)
    for x in range(temple.width):
        for y in range(temple.height):
            r, g_, b, a = temple.getpixel((x, y))
            mask.putpixel((x, y), a if r > 90 else 0)
    g.paste(temple, (52, 62), mask)
    name, lines = ("Curator", ["Sort your gallery like", "a museum storeroom"]) if lang == "en" else \
        ("Куратор", ["Разбор галереи", "как в запаснике музея"])
    left = W - front.width - 56 - 52 - 40
    d.text((52, 190), name, font=font(fit(name, 92, left)), fill=STONE)
    d.rectangle((52, 296, 52 + left, 299), fill=BRONZE)
    sub = font(min(fit(l, 44, left, "cormorant_semibold") for l in lines), "cormorant_semibold")
    for i, l in enumerate(lines):
        d.text((52, 318 + i * 54), l, font=sub, fill=GOLD if i == 1 else STONE)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon(512).save(OUT / "icon-512.png", optimize=True)
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, cut, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", cut, en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
