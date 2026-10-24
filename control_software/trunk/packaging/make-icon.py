"""
Draws the program icon of the PRM80 control software: a small picture of
the main window (black display with RX / TX frequency, channel and
HIGH POWER, control panel with buttons) and the name "PRM80" below it.

Needs Python 3 with Pillow (pip install pillow). Run in this folder:
    python make-icon.py
Writes PRM80.ico (Windows, all sizes), PRM80.png (Linux, 256 px) and the
window icons ../src/main/resources/f4fez/prm80x0/gui/resources/icons/prm80-<size>.png

Sizes 48 px and more: window picture with "PRM80" below.
Sizes below 48 px (title bar, task bar): only the display, the text
would not be readable there.
"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "f4fez", "prm80x0", "gui", "resources")
LCD = os.path.join(RES, "lcd.ttf")
SANS = [
    "C:/Windows/Fonts/arialbd.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/Library/Fonts/Arial Bold.ttf",
]

# colours of the main window
RX = (51, 255, 204)        # RX frequency
TX = (255, 204, 0)         # TX frequency
CHAN = (255, 51, 51)       # channel number and comment
HP = (255, 0, 0)           # HIGH POWER
PANEL = (242, 242, 242)    # control panel
FRAME = (120, 120, 126)
BUTTON = (255, 255, 255)
BUTTON_BORDER = (170, 170, 176)
SELECTED = (215, 215, 220)
SLIDER = (40, 120, 220)


SANS_FILE = next((f for f in SANS if os.path.exists(f)), None)
if SANS_FILE is None:
    raise SystemExit("No bold sans serif font found, please add one to SANS")


def fit(fontfile, text, width, height):
    """Largest font of fontfile in which text fits into width x height."""
    size = 4
    best = ImageFont.truetype(fontfile, size)
    while True:
        f = ImageFont.truetype(fontfile, size + 1)
        b = f.getbbox(text)
        if b[2] - b[0] > width or b[3] - b[1] > height:
            return best
        best = f
        size += 1


def text_in(d, text, font, box, color, align="center", stroke=0, stroke_color=None):
    x0, y0, x1, y1 = box
    b = font.getbbox(text, stroke_width=stroke)
    w, h = b[2] - b[0], b[3] - b[1]
    x = x0 if align == "left" else x0 + (x1 - x0 - w) / 2
    d.text((x - b[0], y0 + (y1 - y0 - h) / 2 - b[1]), text, font=font, fill=color,
           stroke_width=stroke, stroke_fill=stroke_color or color)


def button(d, box, s, fill=BUTTON):
    d.rounded_rectangle(box, radius=6 * s, fill=fill, outline=BUTTON_BORDER, width=max(1, int(2 * s)))


def display(d, box, s, detail):
    """The black display of the main window inside box."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    d.rectangle(box, fill=(0, 0, 0))
    # RX frequency (large), TX frequency (smaller), as in the program
    f1 = fit(LCD, "145.600", w * 0.56, h * 0.36)
    text_in(d, "145.600", f1, (x0 + w * 0.05, y0 + h * 0.10, x0 + w * 0.62, y0 + h * 0.48), RX, "left", int(2 * s))
    f2 = fit(LCD, "145.000", w * 0.40, h * 0.24)
    text_in(d, "145.000", f2, (x0 + w * 0.15, y0 + h * 0.56, x0 + w * 0.62, y0 + h * 0.84), TX, "left", int(1.5 * s))
    # channel number
    f3 = fit(LCD, "00", w * 0.16, h * 0.36)
    text_in(d, "00", f3, (x0 + w * 0.66, y0 + h * 0.10, x0 + w * 0.84, y0 + h * 0.48), CHAN, "left", int(2 * s))
    # HIGH POWER and, if there is room, the comment line
    d.rectangle([x0 + w * 0.82, y0 + h * 0.10, x0 + w * 0.97, y0 + h * 0.24], fill=HP)
    if detail:
        d.rectangle([x0 + w * 0.66, y0 + h * 0.62, x0 + w * 0.94, y0 + h * 0.72], fill=CHAN)


def window(d, box, s, detail):
    """Picture of the main window: menu bar, display, control panel."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    d.rounded_rectangle(box, radius=10 * s, fill=PANEL, outline=FRAME, width=max(1, int(3 * s)))
    # menu / port row
    bar = y0 + h * 0.12
    if detail:
        for i in range(3):
            d.rectangle([x0 + w * (0.05 + i * 0.11), y0 + h * 0.045, x0 + w * (0.13 + i * 0.11), y0 + h * 0.075],
                        fill=(90, 90, 96))
    # display
    dy1 = y0 + h * 0.50
    display(d, (x0 + max(1, 3 * s), bar, x1 - max(1, 3 * s), dy1), s, detail)
    # control panel
    py0, py1 = dy1 + h * 0.06, y1 - h * 0.06
    ph = py1 - py0
    # volume slider
    sx = x0 + w * 0.07
    d.rectangle([sx - 2 * s, py0, sx + 2 * s, py1], fill=(150, 150, 156))
    d.rounded_rectangle([sx - w * 0.025, py0 + ph * 0.55, sx + w * 0.025, py0 + ph * 0.70], radius=3 * s, fill=SLIDER)
    # power / scan buttons
    button(d, (x0 + w * 0.14, py0 + ph * 0.05, x0 + w * 0.33, py0 + ph * 0.40), s)
    button(d, (x0 + w * 0.14, py0 + ph * 0.55, x0 + w * 0.33, py0 + ph * 0.90), s, SELECTED)
    # VFO / MEM
    button(d, (x0 + w * 0.42, py0 + ph * 0.05, x0 + w * 0.58, py0 + ph * 0.40), s)
    button(d, (x0 + w * 0.42, py0 + ph * 0.55, x0 + w * 0.58, py0 + ph * 0.90), s, SELECTED)
    # shift check boxes
    if detail:
        for i in range(3):
            cy = py0 + ph * (0.10 + i * 0.27)
            d.rectangle([x0 + w * 0.63, cy, x0 + w * 0.66, cy + ph * 0.14], outline=BUTTON_BORDER, width=max(1, int(2 * s)),
                        fill=(60, 120, 220) if i == 0 else BUTTON)
            d.rectangle([x0 + w * 0.68, cy + ph * 0.04, x0 + w * 0.79, cy + ph * 0.10], fill=(110, 110, 116))
    # + / - buttons
    button(d, (x0 + w * 0.83, py0, x0 + w * 0.95, py0 + ph * 0.46), s)
    button(d, (x0 + w * 0.83, py0 + ph * 0.54, x0 + w * 0.95, py1), s)


def icon(n):
    S = 8                                   # draw 8 times larger, then reduce
    N = n * S
    s = N / 256.0                           # line widths are given for 256 px
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if n >= 48:
        # window picture, "PRM80" below it
        window(d, (N * 0.03, N * 0.04, N * 0.97, N * 0.70), s, n >= 64)
        f = fit(SANS_FILE, "PRM80", N * 0.86, N * 0.22)
        text_in(d, "PRM80", f, (0, N * 0.74, N, N * 0.98), (30, 30, 34), "center",
                int(N * 0.012), (255, 255, 255))
    else:
        # small: only the display (fills the icon)
        d.rounded_rectangle([0, N * 0.12, N - 1, N * 0.88], radius=N * 0.08, fill=(0, 0, 0),
                            outline=FRAME, width=max(S, int(N * 0.03)))
        # digits would only be a blur here: lines in the colours of the display
        d.rectangle([N * 0.10, N * 0.24, N * 0.62, N * 0.44], fill=RX)           # RX
        d.rectangle([N * 0.26, N * 0.54, N * 0.62, N * 0.66], fill=TX)           # TX
        d.rectangle([N * 0.70, N * 0.24, N * 0.88, N * 0.44], fill=CHAN)         # channel
        d.rectangle([N * 0.70, N * 0.54, N * 0.88, N * 0.66], fill=CHAN)         # comment
    return img.resize((n, n), Image.LANCZOS)


sizes = [16, 20, 24, 32, 40, 48, 64, 128, 256]
imgs = {n: icon(n) for n in sizes}
icons = os.path.join(RES, "icons")
os.makedirs(icons, exist_ok=True)
for n in [16, 24, 32, 48, 64, 128, 256]:
    imgs[n].save(os.path.join(icons, f"prm80-{n}.png"))
imgs[256].save(os.path.join(HERE, "PRM80.ico"), sizes=[(n, n) for n in sizes],
               append_images=[imgs[n] for n in sizes if n != 256])
imgs[256].save(os.path.join(HERE, "PRM80.png"))
print("Icons written.")
