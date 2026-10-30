import copy, importlib.util, json, uuid, tempfile, threading, http.client, struct, unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('server_under_test',str(Path(__file__).with_name('server.py')))
s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
def base(kind='manual'):
 return {'schema':1,'incident_id':str(uuid.uuid4()),'time_utc_minute':'2026-10-29T11:30Z','kind':kind,'device':'Sony Xperia XZ1 Compact / SO-02K','android_sdk':35,'system_build':'1792998692','security_patch':'2026-08-01'}
def session():
 return dict(base('session'),package='com.ss.android.ugc.aweme',app_version_code=400701,application='抖音',app_version_name='40.7.0',description='日常视频花屏',session={'duration_seconds':10,'samples':[],'events':[],'playback':{'status':'captured','scope':'selected_app_and_shared_media','records':['+1.0s selected_app ACodec: OMX.qcom.video.decoder.hevc'],'renderer':'skiavk'}})
class Reports(unittest.TestCase):
 def test_old_reports(self):
  s.validate(dict(base(),symptom='playback'))
  r=session();r['session'].pop('playback');[r.pop(k) for k in ('application','description','app_version_name')];s.validate(r)
 def test_new_manual(self):
  s.validate(dict(base(),symptom='playback',application='抖音',description='日常视频花屏\nhttps://example.com/a',package='com.ss.android.ugc.aweme',app_version_code=400701))
 def test_invalid(self):
  for mutate in (lambda r:r['session']['playback'].update(status='success_guess'),lambda r:r.update(description='x'*4001),lambda r:r['session']['playback'].update(records=['x'*241]),lambda r:r['session']['playback'].update(records=['x']*49),lambda r:r['session']['playback'].update(records=['中'*200]*20),lambda r:r.update(package='invalid name'),lambda r:r.update(app_version_code=-1)):
   r=session();mutate(r)
   with self.assertRaises(ValueError):s.validate(r)
 def test_http_roundtrip_and_escape(self):
  with tempfile.TemporaryDirectory() as temp:
   inbox=s.Inbox(temp,'test-password-for-local-only','test-token-for-local-only-00000000000000')
   server=s.Server(('127.0.0.1',0),inbox);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
   def request(method,path,body=None,ctype='application/json',auth=True,cookie=None):
    c=http.client.HTTPConnection('127.0.0.1',server.server_port,timeout=5)
    headers={'Content-Type':ctype}
    if auth:headers['Authorization']='Bearer '+inbox.upload_token
    if cookie:headers['Cookie']=cookie
    c.request(method,path,body,headers);response=c.getresponse();data=response.read();code=response.status;c.close();return code,data
   try:
    old=dict(base(),symptom='playback');self.assertEqual(request('POST','/api/reports',json.dumps(old))[0],201)
    r=session();r['description']='<script>alert(1)</script>日常花屏';data=json.dumps(r,ensure_ascii=False).encode();wire=struct.pack('>I',len(data))+data
    self.assertEqual(request('POST','/api/sessions',wire,'application/vnd.xz1c.bug-session')[0],201)
    self.assertEqual(request('POST','/api/sessions',wire,'application/vnd.xz1c.bug-session',False)[0],401)
    # Submitted sessions are idempotent, and descriptions are escaped in the admin view.
    self.assertEqual(request('POST','/api/sessions',wire,'application/vnd.xz1c.bug-session')[0],201)
    token='local-session';inbox.sessions[token]=(s.time.time()+60,'csrf')
    code,html=request('GET','/report?id='+r['incident_id'],cookie='inbox_session='+token)
    self.assertEqual(code,200);self.assertNotIn(b'<script>',html);self.assertIn(b'&lt;script&gt;',html)
    with inbox.db() as db:self.assertEqual(db.execute('select count(*) from reports').fetchone()[0],2)
   finally:server.shutdown();server.server_close()
if __name__=='__main__':unittest.main()
