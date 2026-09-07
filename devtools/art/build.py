"""The art of Miracle Bringer, generated.

The halo's gold swatch and the two sounds are written by this script; the
visitor's skin is not -- it is a player skin Rusty chose, kept as a file
(see README.md, Provenance). Run with `uv run devtools/art/build.py`
[textures|sounds]; no arguments does everything.
"""

from __future__ import annotations

import json
import math
import struct
import subprocess
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/miraclebringer"
MODID = "miraclebringer"


def write_png(path: Path, width: int, height: int, pixels) -> None:
    """pixels: rows of (r, g, b, a) tuples, top row first."""
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


class Noise:
    """A deterministic grain so flat colours read as material, not plastic."""

    def __init__(self, seed: int) -> None:
        self.state = seed & 0xFFFFFFFF

    def next(self) -> float:
        self.state = (1664525 * self.state + 1013904223) & 0xFFFFFFFF
        return self.state / 0xFFFFFFFF



RATE = 44100


def synth(seconds, fn):
    n = int(RATE * seconds)
    return [max(-1.0, min(1.0, fn(i / RATE, i / n))) for i in range(n)]


def write_ogg(path: Path, samples) -> None:
    """Writes 16-bit mono PCM through ffmpeg into Ogg Vorbis."""
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = b"".join(struct.pack("<h", int(s * 32767)) for s in samples)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(RATE), "-ac", "1",
                    "-i", "pipe:0", "-c:a", "libvorbis", "-q:a", "5", str(path)],
                   input=pcm, check=True)


def lowpass(samples, hz):
    """One-pole lowpass; hz is the -3 dB point."""
    a = 1.0 - math.exp(-2.0 * math.pi * hz / RATE)
    out, y = [], 0.0
    for x in samples:
        y += (x - y) * a
        out.append(y)
    return out


def lowpass2(samples, hz):
    """Two poles: 12 dB an octave, enough to keep noise from reading as hiss."""
    return lowpass(lowpass(samples, hz), hz)


def highpass(samples, hz):
    """One-pole highpass: the input minus its lowpass."""
    return [x - y for x, y in zip(samples, lowpass(samples, hz))]


def saturate(samples, drive):
    """Soft clipping: tanh, so the peaks compress and grow harmonics instead of cracking."""
    top = math.tanh(drive)
    return [math.tanh(x * drive) / top for x in samples]


def normalize(samples, peak=0.95):
    top = max(abs(x) for x in samples) or 1.0
    return [x * peak / top for x in samples]


def slapback(samples, delays_ms, gains):
    """Discrete early reflections: the outdoors answering the shot."""
    out = list(samples)
    for ms, g in zip(delays_ms, gains):
        d = int(RATE * ms / 1000)
        for i in range(d, len(out)):
            out[i] += samples[i - d] * g
    return out


def mechanism(n, seed, events):
    """Sounds of the gun's action after the shot, at the seconds given:
    a "clack" is a short burst of bright noise with a low thunk under it (a
    pump racked, a magazine seated); a "ring" is a metallic ping (a bolt
    handle, a slide). Each event is (seconds, kind, gain)."""
    out = [0.0] * n
    noise = Noise(seed)
    for at, kind, gain in events:
        i0 = int(at * RATE)
        if kind == "clack":
            length = int(0.035 * RATE)
            burst = highpass(lowpass2([noise.next() * 2 - 1 for _ in range(length)], 3500), 1200)
            for i, b in enumerate(burst):
                tt = i / RATE
                if i0 + i < n:
                    out[i0 + i] += gain * (b * math.exp(-tt * 90) + 0.6 * math.sin(2 * math.pi * 170 * tt) * math.exp(-tt * 60))
        else:
            length = int(0.09 * RATE)
            for i in range(length):
                tt = i / RATE
                if i0 + i < n:
                    out[i0 + i] += gain * (math.sin(2 * math.pi * 2600 * tt) + 0.5 * math.sin(2 * math.pi * 4100 * tt)) * math.exp(-tt * 350)
    return out



def halo_swatch():
    """An 8x8 gold gradient: bright along the ring's outer edge, deeper inside."""
    px = [[(0, 0, 0, 0) for _ in range(8)] for _ in range(8)]
    for y in range(8):
        t = y / 7.0
        r = int(255 - 30 * t)
        g = int(230 - 70 * t)
        b = int(120 - 60 * t)
        for x in range(8):
            px[y][x] = (r, g, b, 255)
    return px


def blessing_icon():
    """The effect's icon, 18x18: a golden halo, seen a little from above."""
    px = [[(0, 0, 0, 0) for _ in range(18)] for _ in range(18)]
    cx, cy = 8.5, 8.5
    for y in range(18):
        for x in range(18):
            dx, dy = (x + 0.5 - cx), (y + 0.5 - cy) / 0.55   # an ellipse: the ring tilted toward the eye
            d = math.hypot(dx, dy)
            if 5.2 <= d <= 7.4:
                t = (d - 5.2) / 2.2
                r, g, bl = int(255 - 20 * t), int(232 - 60 * t), int(120 - 50 * t)
                px[y][x] = (r, g, bl, 255)
            elif 4.6 <= d < 5.2 or 7.4 < d <= 8.0:
                px[y][x] = (255, 240, 170, 110)   # a soft glow either side
    return px


def chord(seconds, freqs, attack, release, gain=0.25):
    """A sustained chord of sines with a slow attack and release, a little
    shimmer from detuned pairs -- a choir without a choir."""
    n = int(RATE * seconds)
    out = []
    for i in range(n):
        t = i / RATE
        env = min(1.0, t / attack) * min(1.0, max(0.0, (seconds - t) / release))
        s = 0.0
        for f in freqs:
            s += math.sin(2 * math.pi * f * t) + 0.6 * math.sin(2 * math.pi * f * 1.003 * t + 0.5)
            s += 0.25 * math.sin(2 * math.pi * f * 2 * t)
        out.append(gain * env * s / len(freqs))
    return out


def blessing():
    # A major chord, rising by a fifth halfway: arrival, then the light.
    a = chord(1.6, [261.6, 329.6, 392.0, 523.3], 0.35, 0.6)
    b = chord(2.2, [392.0, 493.9, 587.3, 784.0], 0.5, 1.0)
    n = int(RATE * 3.0)
    mix = [0.0] * n
    for i, s in enumerate(a):
        mix[i] += s
    start = int(RATE * 0.9)
    for i, s in enumerate(b):
        if start + i < n:
            mix[start + i] += s
    return normalize(lowpass2(mix, 5000), 0.8)


def ascend():
    # A shimmer sweeping upward over two seconds, thinning as it goes.
    n = int(RATE * 2.4)
    out, phase = [], 0.0
    noise = Noise(0xA5CE)
    for i in range(n):
        t = i / RATE
        f = 440.0 * (2.0 ** (t / 1.2))
        phase += 2 * math.pi * f / RATE
        env = min(1.0, t / 0.2) * max(0.0, 1.0 - t / 2.4)
        s = math.sin(phase) * 0.5 + math.sin(phase * 1.5) * 0.25 + (noise.next() - 0.5) * 0.15
        out.append(env * s * 0.6)
    return normalize(lowpass2(out, 6000), 0.7)


SOUNDS = {"blessing": blessing, "ascend": ascend}


def sounds_json():
    return {name: {"subtitle": f"subtitles.{MODID}.{name}", "sounds": [f"{MODID}:{name}"]} for name in SOUNDS}


def main(argv) -> int:
    want = set(argv[1:]) or {"textures", "sounds"}
    if "textures" in want:
        write_png(ASSETS / "textures/entity/halo.png", 8, 8, halo_swatch())
        write_png(ASSETS / "textures/mob_effect/blessing.png", 18, 18, blessing_icon())
        print("textures: entity/halo.png (8x8), mob_effect/blessing.png (18x18)")
    if "sounds" in want:
        for name, fn in SOUNDS.items():
            write_ogg(ASSETS / f"sounds/{name}.ogg", fn())
        (ASSETS / "sounds.json").write_text(json.dumps(sounds_json(), indent=2) + "\n")
        print(f"sounds: {', '.join(SOUNDS)} + sounds.json")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
