import subprocess, xml.etree.ElementTree as E, re, json, time
b=['adb']
def run(args): return subprocess.run(b+args,capture_output=True,check=True).stdout
def dump():
 run(['shell','uiautomator','dump','/data/local/tmp/vultr-reinstall-qa.xml'])
 root=E.fromstring(run(['exec-out','cat','/data/local/tmp/vultr-reinstall-qa.xml']))
 assert any(n.get('package')=='com.yjw.vultr.preview' for n in root.iter('node'))
 return root
def start():
 run(['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--ez','reinstallConfirm','true'])
 return dump()
def tap(root,text):
 n=next(n for n in root.iter('node') if n.get('text')==text)
 x,y,z,w=map(int,re.findall(r'\d+',n.get('bounds')))
 run(['shell','input','tap',str((x+z)//2),str((y+w)//2)])
try:
 root=start(); texts=[n.get('text','') for n in root.iter('node')]
 assert '重新部署这台服务器？' in texts
 assert '确认重新部署' in texts and '取消' in texts
 assert not any(n.get('class','').endswith('EditText') for n in root.iter('node'))
 assert any('203.0.113.42' in t and '11111111-1111-4111-8111-111111111111' in t and '无法恢复' in t for t in texts)
 tap(root,'取消'); assert not any(n.get('text')=='确认重新部署' for n in dump().iter('node'))
 root=start();tap(root,'确认重新部署');assert not any(n.get('text')=='确认重新部署' for n in dump().iter('node'))
 result={'version':'3.4.2','no_text_input':True,'exact_target_and_disk_warning':True,'cancel_dismisses':True,'confirm_accepts':True,'environment':'Fictional native preview without Internet permission; no real reinstall performed'}
 open('qa/reinstall-confirm-report.json','w').write(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print('PASS: reinstall button confirmation, no text input, target and warning, cancel and confirm (fictional only)')
finally:run(['shell','rm','-f','/data/local/tmp/vultr-reinstall-qa.xml','/data/local/tmp/vultr-installer-check.xml'])
