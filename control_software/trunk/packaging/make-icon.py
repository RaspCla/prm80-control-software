"""
Draws the program icon of the PRM80 control software (looks like the
display of the main window, "PRM80" in the LCD font of the program).

Needs Python 3 with Pillow (pip install pillow). Run in this folder:
    python make-icon.py
Writes PRM80.ico (Windows, all sizes), PRM80.png (Linux, 256 px) and the
window icons ../src/main/resources/f4fez/prm80x0/gui/resources/icons/prm80-<size>.png
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "f4fez", "prm80x0", "gui", "resources")
FONT = os.path.join(RES, "lcd.ttf")
GREEN = (51, 255, 204)
ORANGE = (255, 204, 0)
RED = (255, 51, 51)

def fit_font(text, width, height):
    size = 4
    best = ImageFont.truetype(FONT, size)
    while True:
        f = ImageFont.truetype(FONT, size + 1)
        b = f.getbbox(text)
        if b[2] - b[0] > width or b[3] - b[1] > height:
            return best
        best = f
        size += 1

def draw_text(d, text, font, box, color, stroke=0):
    x0, y0, x1, y1 = box
    b = font.getbbox(text, stroke_width=stroke)
    w, h = b[2] - b[0], b[3] - b[1]
    d.text((x0 + (x1 - x0 - w) / 2 - b[0], y0 + (y1 - y0 - h) / 2 - b[1]), text, font=font, fill=color,
           stroke_width=stroke, stroke_fill=color)

def icon(n):
    S = 4                      # supersampling
    N = n * S
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = N * 0.16
    if n >= 48:
        # radio front: grey body, black display, light control panel with buttons
        d.rounded_rectangle([0, 0, N - 1, N - 1], radius=r, fill=(70, 70, 74, 255))
        d.rounded_rectangle([N*0.02, N*0.02, N*0.98, N*0.98], radius=r*0.9, fill=(48, 48, 52, 255))
        disp = [N*0.08, N*0.10, N*0.92, N*0.66]
        d.rounded_rectangle(disp, radius=N*0.05, fill=(0, 0, 0, 255), outline=(20, 20, 20, 255), width=max(1, int(N*0.01)))
        # PRM80 in the green LCD font
        f = fit_font("PRM80", (disp[2]-disp[0]) * 0.86, (disp[3]-disp[1]) * 0.42)
        stroke = int(S * (0.9 if n <= 64 else 0.6 * n / 64))
        draw_text(d, "PRM80", f, (disp[0], disp[1] + (disp[3]-disp[1])*0.06, disp[2], disp[1] + (disp[3]-disp[1])*0.60), GREEN, stroke)
        if n <= 64:
            # second line too small for digits: orange TX bar and red channel bar
            yb0 = disp[1] + (disp[3]-disp[1])*0.72; yb1 = disp[1] + (disp[3]-disp[1])*0.84
            d.rectangle([disp[0] + (disp[2]-disp[0])*0.10, yb0, disp[0] + (disp[2]-disp[0])*0.60, yb1], fill=ORANGE)
            d.rectangle([disp[0] + (disp[2]-disp[0])*0.70, yb0, disp[0] + (disp[2]-disp[0])*0.88, yb1], fill=RED)
    if n >= 48:
        by0, by1 = N*0.74, N*0.90
    if n > 64:
        # second line: orange TX digits and red channel, like the display
        f2 = fit_font("145.600", (disp[2]-disp[0]) * 0.50, (disp[3]-disp[1]) * 0.22)
        st2 = int(S * 0.3 * n / 64)
        draw_text(d, "145.600", f2, (disp[0] + (disp[2]-disp[0])*0.05, disp[1] + (disp[3]-disp[1])*0.62, disp[0] + (disp[2]-disp[0])*0.62, disp[3] - (disp[3]-disp[1])*0.08), ORANGE, st2)
        draw_text(d, "00", f2, (disp[0] + (disp[2]-disp[0])*0.68, disp[1] + (disp[3]-disp[1])*0.62, disp[2] - (disp[2]-disp[0])*0.06, disp[3] - (disp[3]-disp[1])*0.08), RED, st2)
    if n >= 48:
        # buttons
        bw = N*0.17
        for i, x in enumerate([N*0.10, N*0.31, N*0.52, N*0.73]):
            col = (210, 210, 214, 255) if i < 3 else (230, 230, 234, 255)
            d.rounded_rectangle([x, by0, x + bw, by1], radius=N*0.03, fill=col)
    else:
        # small sizes: only the black display with PRM80 (as large as possible)
        d.rounded_rectangle([0, 0, N - 1, N - 1], radius=r, fill=(0, 0, 0, 255), outline=(90, 90, 96, 255), width=S)
        text = "PRM80"
        stroke = int(S * (0.5 if n <= 24 else 0.75))
        f = fit_font(text, N * 0.84, N * 0.70)
        draw_text(d, text, f, (N * 0.04, N * 0.04, N * 0.96, N * 0.66), GREEN, stroke)
        # orange line = TX row
        d.rectangle([N*0.14, N*0.72, N*0.58, N*0.80], fill=ORANGE)
        d.rectangle([N*0.68, N*0.72, N*0.86, N*0.80], fill=RED)
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
