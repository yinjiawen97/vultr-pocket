"""Layout checks against the offline preview package; never opens the production app."""
import subprocess, xml.etree.ElementTree as E, re, json, shlex
from pathlib import Path

adb = ['adb']  # set ANDROID_SERIAL to pick a device
out = Path(__file__).resolve().parent / 'screens'
def run(args):
    return subprocess.run(adb + args, capture_output=True, check=True).stdout
def bounds(node):
    return list(map(int, re.findall(r'\d+', node.get('bounds'))))
def dump():
    run(['shell', 'uiautomator', 'dump', '/data/local/tmp/os-qa.xml'])
    root = E.fromstring(run(['exec-out', 'cat', '/data/local/tmp/os-qa.xml']))
    assert any(n.get('package') == 'com.yjw.vultr.preview' for n in root.iter('node')), 'Preview unavailable or screen locked'
    return root
cases = [
    ('ubuntu-rack', 'Ubuntu 24.04 LTS x64', {}),
    ('ubuntu-pulse', 'Ubuntu 24.04 LTS x64', {'style':'pulse'}),
    ('debian-dark', 'Debian 12 x64', {'dark': True}),
    ('windows', 'Windows Server 2025 Standard x64', {}),
    ('macos-dark', 'macOS Sonoma', {'dark': True}),
    ('linux', 'Alpine Linux 3.21', {}),
    ('fedora', 'Fedora 42', {}),
    ('rocky', 'Rocky Linux 9', {}),
    ('alma', 'AlmaLinux 9', {}),
    ('arch', 'Arch Linux', {}),
    ('unknown', 'Custom ISO', {}),
    ('missing', '系统待获取', {'missingOs': True}),
    ('large', 'Windows Server 2025 Standard x64', {'long': True, 'font': 2.0}),
    ('list', 'Ubuntu 24.04 LTS x64', {'screen': '服务器'}),
    ('detail-managed', 'Ubuntu 24.04 LTS x64', {'screen': '详情'}),
    ('detail-unmanaged', 'Windows Server 2025 Standard x64', {'screen': '详情', 'unmanaged': True}),
    ('landscape', 'Ubuntu 24.04 LTS x64', {'landscape': True}),
]
report = []
for name, osname, extra in cases:
    values = {'screen':'总览', 'style':'rack', 'motion':False, 'os':osname, **extra}
    args = ['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--ei','width','360']
    for key, value in values.items():
        args += ['--ez' if isinstance(value,bool) else '--ef' if isinstance(value,float) else '--es', key, str(value).lower() if isinstance(value,bool) else str(value)]
    run(["shell", shlex.join(args[1:])])
    for attempt in range(12):
        root = dump()
        view = next(n for n in root.iter('node') if n.get('class')=='android.widget.ScrollView' and n.get('package')=='com.yjw.vultr.preview')
        left, top, right, bottom = bounds(view)
        matches = [n for n in root.iter('node') if n.get('text')==osname]
        if matches:
            x,y,z,w = bounds(matches[0])
            if y >= top+10 and w <= bottom-10 and w > y:
                break
        middle = (left+right)//2
        height = bottom-top
        # Small steps keep the system row visible even at large font size.
        run(['shell','input','swipe',str(middle),str(top+int(height*.8)),str(middle),str(top+int(height*.55)),'600'])
    else:
        raise AssertionError('System label not fully visible: '+name)
    assert z>x and w>y, 'Empty or clipped system label'
    assert any(n.get('class')=='android.widget.ImageView' and n.get('package')=='com.yjw.vultr.preview' for n in root.iter('node')), 'Missing local system glyph'
    (out/('system-'+name+'.png')).write_bytes(run(['exec-out','screencap','-p']))
    report.append({'case':name,'result':'PASS','system':osname,'bounds':[x,y,z,w]})
    print('PASS:',name,flush=True)
(out/'system-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
