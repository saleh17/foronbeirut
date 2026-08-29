# -*- coding: utf-8 -*-
"""Turn the character SVGs in the design canvas into Android vector drawables.

The art already exists and is signed off; hand-translating it into Compose paths
would guarantee drift. This converts it mechanically instead, so the app and the
canvas cannot disagree.
"""
import io, re, math

SRC = '/home/user/foronbeirut/design/Main.dc.html'
OUT = '/home/user/foronbeirut/app/src/main/res/drawable/'

def colour(v):
    v = v.strip()
    if v in ('none', ''):
        return None
    m = re.match(r'rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]*)\s*)?\)', v)
    if m:
        r, g, b = (int(m.group(i)) for i in (1, 2, 3))
        a = m.group(4)
        alpha = 1.0 if a in (None, '') else float(a if not a.startswith('.') else '0' + a)
        return '#%02X%02X%02X%02X' % (round(alpha * 255), r, g, b)
    if v.startswith('#'):
        h = v[1:]
        if len(h) == 3:
            h = ''.join(c * 2 for c in h)
        return '#' + h.upper()
    raise ValueError('colour? ' + v)

def attrs_of(tag):
    return dict(re.findall(r'([a-zA-Z-]+)="([^"]*)"', tag))

def circle_path(cx, cy, r):
    return 'M%g,%gA%g,%g 0 1,0 %g,%gA%g,%g 0 1,0 %g,%gZ' % (
        cx - r, cy, r, r, cx + r, cy, r, r, cx - r, cy)

def ellipse_path(cx, cy, rx, ry):
    return 'M%g,%gA%g,%g 0 1,0 %g,%gA%g,%g 0 1,0 %g,%gZ' % (
        cx - rx, cy, rx, ry, cx + rx, cy, rx, ry, cx - rx, cy)

def rect_path(x, y, w, h, rx):
    if not rx:
        return 'M%g,%gH%gV%gH%gZ' % (x, y, x + w, y + h, x)
    rx = min(rx, w / 2, h / 2)
    return ('M%g,%gH%gA%g,%g 0 0,1 %g,%gV%gA%g,%g 0 0,1 %g,%gH%gA%g,%g 0 0,1 %g,%gV%gA%g,%g 0 0,1 %g,%gZ'
            % (x + rx, y, x + w - rx, rx, rx, x + w, y + rx, y + h - rx, rx, rx,
               x + w - rx, y + h, x + rx, rx, rx, x, y + h - rx, y + rx, rx, rx, x + rx, y))

def f(a, k, d=0.0):
    return float(a.get(k, d))

def path_element(tag, name, a, indent):
    d = None
    if name == 'path':
        d = a.get('d')
        if not d or '{{' in d:
            return ''  # the mouth is drawn live, not baked in
    elif name == 'circle':
        d = circle_path(f(a, 'cx'), f(a, 'cy'), f(a, 'r'))
    elif name == 'ellipse':
        d = ellipse_path(f(a, 'cx'), f(a, 'cy'), f(a, 'rx'), f(a, 'ry'))
    elif name == 'rect':
        d = rect_path(f(a, 'x'), f(a, 'y'), f(a, 'width'), f(a, 'height'), f(a, 'rx'))
    else:
        return ''

    out = [indent + '<path']
    out.append(indent + '    android:pathData="%s"' % d)
    fill = colour(a.get('fill', 'none'))
    if fill:
        out.append(indent + '    android:fillColor="%s"' % fill)
    stroke = colour(a.get('stroke', 'none'))
    if stroke:
        out.append(indent + '    android:strokeColor="%s"' % stroke)
        out.append(indent + '    android:strokeWidth="%s"' % a.get('stroke-width', '1'))
        if 'stroke-linecap' in a:
            out.append(indent + '    android:strokeLineCap="%s"' % a['stroke-linecap'])
        if 'stroke-linejoin' in a:
            out.append(indent + '    android:strokeLineJoin="%s"' % a['stroke-linejoin'])
    if 'opacity' in a:
        o = a['opacity']
        o = o if not o.startswith('.') else '0' + o
        if fill:
            out.append(indent + '    android:fillAlpha="%s"' % o)
        if stroke:
            out.append(indent + '    android:strokeAlpha="%s"' % o)
    out.append(indent + '    />')
    return '\n'.join(out) + '\n'

def group_open(transform, indent):
    """SVG transform -> vector <group>. Android applies translate * rotate(pivot) * scale."""
    parts = []
    m = re.match(r'translate\(([-\d.]+)[ ,]([-\d.]+)\)\s*scale\(([-\d.]+)\)', transform)
    if m:
        parts = ['android:translateX="%s"' % m.group(1), 'android:translateY="%s"' % m.group(2),
                 'android:scaleX="%s"' % m.group(3), 'android:scaleY="%s"' % m.group(3)]
    if not parts:
        m = re.match(r'translate\(([-\d.]+)[ ,]([-\d.]+)\)\s*rotate\(([-\d.]+)\)', transform)
        if m:
            parts = ['android:translateX="%s"' % m.group(1), 'android:translateY="%s"' % m.group(2),
                     'android:rotation="%s"' % m.group(3)]
    if not parts:
        m = re.match(r'rotate\(([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\)', transform)
        if m:
            parts = ['android:rotation="%s"' % m.group(1),
                     'android:pivotX="%s"' % m.group(2), 'android:pivotY="%s"' % m.group(3)]
    if not parts:
        m = re.match(r'translate\(([-\d.]+)[ ,]([-\d.]+)\)$', transform.strip())
        if m:
            parts = ['android:translateX="%s"' % m.group(1), 'android:translateY="%s"' % m.group(2)]
    if not parts:
        raise ValueError('transform? ' + transform)
    return indent + '<group\n' + '\n'.join(indent + '    ' + p for p in parts) + '>\n'

def convert(svg):
    root = attrs_of(re.match(r'<svg[^>]*>', svg).group(0))
    vb = root.get('viewBox', '0 0 110 200').split()
    body, indent, depth = [], '    ', 0
    for tag in re.findall(r'<(/?)(\w+)([^>]*)>', svg):
        close, name, rest = tag
        if name == 'svg':
            continue
        if name == 'g':
            if close:
                if depth:
                    depth -= 1
                    body.append('    ' + '    ' * depth + '</group>\n')
                continue
            a = attrs_of('<g' + rest + '>')
            if 'transform' in a:
                body.append(group_open(a['transform'], '    ' + '    ' * depth))
                depth += 1
            continue
        if close:
            continue
        body.append(path_element(rest, name, attrs_of('<' + name + rest + '>'), '    ' + '    ' * depth))
    while depth:
        depth -= 1
        body.append('    ' + '    ' * depth + '</group>\n')

    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated from design/Main.dc.html — do not hand-edit. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="%sdp"\n    android:height="%sdp"\n'
            '    android:viewportWidth="%s"\n    android:viewportHeight="%s">\n%s</vector>\n'
            % (vb[2], vb[3], vb[2], vb[3], ''.join(body)))

s = io.open(SRC, encoding='utf-8').read()
svgs = dict(re.findall(r'<sc-if value="\{\{c\.is([A-L])\}\}"[^>]*>\s*(<svg[\s\S]*?</svg>)\s*</sc-if>', s))
assert len(svgs) == 12
for letter in 'ABCDEFGHIJKL':
    xml = convert(svgs[letter])
    io.open(OUT + 'cust_%s.xml' % letter.lower(), 'w', encoding='utf-8').write(xml)
    print('cust_%s.xml  %4d lines' % (letter.lower(), xml.count('\n')))
