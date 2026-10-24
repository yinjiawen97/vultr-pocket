import subprocess,xml.etree.ElementTree as E,re
b=['adb']
def r(a):return subprocess.run(b+a,capture_output=True,check=True).stdout
def dump():
 r(['shell','uiautomator','dump','/data/local/tmp/vultr-manual-qa.xml']);root=E.fromstring(r(['exec-out','cat','/data/local/tmp/vultr-manual-qa.xml']));assert any(n.get('package')=='com.yjw.vultr.preview' for n in root.iter('node'));return root
def find(text):
 for _ in range(18):
  root=dump();nodes=[n for n in root.iter('node') if n.get('package')=='com.yjw.vultr.preview' and (n.get('text')==text or n.get('content-desc')==text)]
  if nodes:return nodes[-1]
  r(['shell','input','swipe','550','1900','550','1400','600'])
 raise AssertionError('Missing control '+text)
def tap(text):
 n=find(text);x,y,z,w=map(int,re.findall(r'\d+',n.get('bounds')));r(['shell','input','tap',str((x+z)//2),str((y+w)//2)])
r(['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--es','screen','服务器','--ei','width','360']);find('主机登录密码');tap('编辑登录信息')
for label,value in [('SSH 用户名','demo-user'),('SSH 密码','DemoSaved_Only_42')]:
 node=find(label);old=node.get('text','');tap(label);r(['shell','input','keyevent','123']);r(['shell','input','keyevent']+['67']*len(old));r(['shell','input','text',value]);ime=r(['shell','dumpsys','input_method']).decode(errors='replace')
 if re.search(r'mInputShown\s*=\s*true',ime):r(['shell','input','keyevent','4'])
tap('保存');assert find('主机登录密码').get('text')=='DemoSaved_Only_42';tap('刷新登录信息');assert find('主机登录密码').get('text')=='DemoSaved_Only_42'
r(['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--es','screen','服务器','--ei','width','360']);assert find('主机登录密码').get('text')=='DemoSaved_Only_42';print('PASS: manual login edit, plaintext display, cloud refresh preserves manual values, restart persistence',flush=True)
