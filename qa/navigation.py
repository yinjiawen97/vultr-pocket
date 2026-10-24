import subprocess,xml.etree.ElementTree as E,re,json
from pathlib import Path
b=['adb']
def run(args):return subprocess.run(b+args,capture_output=True,check=True).stdout
def dump():
 run(['shell','uiautomator','dump','/data/local/tmp/vultr-qa-nav.xml']);return E.fromstring(run(['exec-out','cat','/data/local/tmp/vultr-qa-nav.xml']))
def tap(text,last=False):
 nodes=[n for n in dump().iter('node') if n.get('package')=='com.yjw.vultr.preview' and n.get('text')==text]
 assert nodes,text
 n=nodes[-1] if last else nodes[0];x,y,z,w=map(int,re.findall(r'\d+',n.get('bounds')));run(['shell','input','tap',str((x+z)//2),str((y+w)//2)])
def visible():return [(n.get('text'),n.get('bounds')) for n in dump().iter('node') if n.get('package')=='com.yjw.vultr.preview' and n.get('text')]
run(['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--es','screen','服务器','--ez','multi','true'])
dump();run(['shell','input','swipe','550','1750','550','1100','700']);before=visible();tap('订阅',True);tap('服务器',True);after=visible()
prior={t:p for t,p in before if t.startswith('示例节点')};later=dict(after)
assert prior and all(later.get(t)==p for t,p in prior.items()),'server scroll not restored'
print('PASS: switching tabs restores server scroll',flush=True)
tap('查看详情');assert any(t=='服务器详情' for t,_ in visible());run(['shell','input','keyevent','KEYCODE_BACK']);after=visible();assert any(t=='全部服务器' for t,_ in after) or any(t.startswith('示例节点') for t,_ in after)
print('PASS: detail belongs to server tab and returns to list',flush=True)
run(['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--es','screen','服务器']);dump();tap('更多',True);tap('永久删除服务器');texts=[t for t,_ in visible()];assert '永久删除这台服务器？' in texts and any('203.0.113.42' in t and '11111111-1111-4111-8111-111111111111' in t for t in texts)
Path('qa/screens/delete-confirm.png').write_bytes(run(['exec-out','screencap','-p']));tap('取消');assert not any(t=='永久删除这台服务器？' for t,_ in visible());print('PASS: deletion confirmation shows exact target and cancels without action',flush=True)
