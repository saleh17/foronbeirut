#!/usr/bin/env python3
"""Generates app/src/main/res/raw/*.wav from scratch.

Sampled audio would mean licensing; synthesised audio means none. These are the
mechanical noises only — the music this game wants is Fairuz, and that has to be
commissioned rather than borrowed. Run from the repository root.
"""
import wave, struct, math, random, os

SR = 44100
OUT = 'app/src/main/res/raw/'

def write(name, samples):
    data = b''.join(struct.pack('<h', max(-32767, min(32767, int(s * 32767)))) for s in samples)
    with wave.open(OUT + name + '.wav', 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data)
    print('%-10s %5.2fs %5d KB' % (name, len(samples) / SR, os.path.getsize(OUT + name + '.wav') // 1024))

def env(i, n, attack=0.005, release=0.5):
    a = int(SR * attack)
    if i < a:
        return i / a
    return math.exp(-((i - a) / max(1, n - a)) / release)

def main():
    os.makedirs(OUT, exist_ok=True)

    n = int(SR * 0.10)
    write('tap', [(0.5 * math.sin(2*math.pi*180*i/SR) + 0.3 * math.sin(2*math.pi*95*i/SR)
                   + 0.25 * (random.random()*2-1) * math.exp(-i/(SR*0.004))) * env(i, n, 0.001, 0.06)
                  for i in range(n)])

    n = int(SR * 0.14)
    write('dough', [(0.55 * math.sin(2*math.pi*120*i/SR) + 0.2 * math.sin(2*math.pi*61*i/SR))
                    * env(i, n, 0.004, 0.05) for i in range(n)])

    n, prev, sz = int(SR * 0.55), 0.0, []
    for i in range(n):
        prev = prev * 0.86 + (random.random()*2-1) * 0.14
        sz.append(prev * 1.7 * math.sin(math.pi * i / n) ** 1.4)
    write('sizzle', sz)

    n = int(SR * 0.42)
    write('coin', [(0.42 * math.sin(2*math.pi*1180*i/SR) + 0.3 * math.sin(2*math.pi*1760*i/SR)
                    + 0.16 * math.sin(2*math.pi*2640*i/SR)) * env(i, n, 0.002, 0.16) for i in range(n)])

    n = int(SR * 0.5)
    write('serve', [(0.4 * math.sin(2*math.pi*660*i/SR) + 0.26 * math.sin(2*math.pi*990*i/SR))
                    * env(i, n, 0.004, 0.22) for i in range(n)])

    n, prev, pr = int(SR * 0.35), 0.0, []
    for i in range(n):
        prev = prev * 0.55 + (random.random()*2-1) * 0.45
        pr.append(prev * 0.75 * math.exp(-i/(SR*0.10)) * (0.4 + 0.6*math.sin(math.pi*i/n)))
    write('paper', pr)

    n = int(SR * 1.4)
    write('bell', [(0.4 * math.sin(2*math.pi*523*i/SR) + 0.3 * math.sin(2*math.pi*784*i/SR)
                    + 0.18 * math.sin(2*math.pi*1046*i/SR)) * env(i, n, 0.003, 0.5) for i in range(n)])

    n = int(SR * 0.18)
    write('nope', [(0.45 * math.sin(2*math.pi*150*i/SR) + 0.25 * math.sin(2*math.pi*100*i/SR))
                   * env(i, n, 0.002, 0.05) for i in range(n)])

if __name__ == '__main__':
    main()
