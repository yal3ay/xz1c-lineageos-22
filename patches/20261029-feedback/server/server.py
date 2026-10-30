#!/usr/bin/env python3
"""Local-first diagnostic inbox. No request, address, header or body logging."""
import contextlib
import datetime as dt
import hashlib
import hmac
import html
import http.cookies
import http.server
import json
import os
from pathlib import Path
import re
import secrets
import sqlite3
import threading
import time
import struct
import tempfile
import urllib.parse

MAX_BODY = 65536
MAX_VIDEO = 20 * 1024 * 1024
# Reserve capacity for rollback journals, metadata and maintenance rather than filling the filesystem.
MAX_VIDEO_STORAGE = 4_400_000_000
MIN_FREE_BYTES = 256 * 1024 * 1024
DB_WRITE_ALLOWANCE = 2 * 1024 * 1024
RETENTION = 30 * 86400
MAX_REPORTS = 2000
UUID = re.compile(r'[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}')
NAME = r'[A-Za-z_$][A-Za-z0-9_$]*'
QUALIFIED = NAME + r'(?:\.' + NAME + r')+'
FRAME = re.compile(r'(?:' + NAME + r'\.)+(?:' + NAME + r'|<init>|<clinit>)\((?:[A-Za-z0-9_$]+\.(?:java|kt)(?::[0-9]{1,6})?|Unknown Source|Native Method)\)|lib[A-Za-z0-9_.+-]{1,100}\.so\+0x[0-9a-f]{1,16}')
SIGNALS = {'SIGILL','SIGABRT','SIGBUS','SIGFPE','SIGSEGV','SIGSYS'}
ALLOWED = {'schema','incident_id','time_utc_minute','kind','symptom','device','android_sdk',
           'system_build','security_patch','package','app_version_code','signal','exception_types','code_frames','session','application','description','app_version_name'}
REQUIRED = {'schema','incident_id','time_utc_minute','kind','device','android_sdk','system_build','security_patch'}

def valid_date(value, fmt):
    try: dt.datetime.strptime(value, fmt); return True
    except (ValueError, TypeError): return False

def validate_details(r):
    for key, limit in (('application',100),('description',4000),('app_version_name',100)):
        if key in r and (type(r[key]) is not str or len(r[key]) > limit or (key != 'app_version_name' and not r[key].strip())): raise ValueError()

def validate(r):
    if type(r) is not dict or not REQUIRED <= r.keys() or r.keys() - ALLOWED: raise ValueError()
    if type(r['schema']) is not int or r['schema'] != 1: raise ValueError()
    if type(r['incident_id']) is not str or not UUID.fullmatch(r['incident_id']): raise ValueError()
    if not valid_date(r['time_utc_minute'], '%Y-%m-%dT%H:%MZ') or len(r['time_utc_minute']) != 17: raise ValueError()
    if r['kind'] not in ('crash','native_crash','anr','manual','session'): raise ValueError()
    if r['device'] != 'Sony Xperia XZ1 Compact / SO-02K': raise ValueError()
    if type(r['android_sdk']) is not int or not 35 <= r['android_sdk'] <= 50: raise ValueError()
    if type(r['system_build']) is not str or not re.fullmatch(r'[0-9]{1,20}|unknown',r['system_build']): raise ValueError()
    if r['security_patch'] != 'unknown' and (not valid_date(r['security_patch'],'%Y-%m-%d') or len(r['security_patch']) != 10): raise ValueError()
    if r['kind'] == 'session':
        if not REQUIRED | {'package','app_version_code','session'} <= r.keys() or r.keys() - REQUIRED - {'package','app_version_code','session','application','description','app_version_name'}: raise ValueError()
        validate_details(r)
        if type(r['package']) is not str or len(r['package']) > 180 or not re.fullmatch(QUALIFIED, r['package']): raise ValueError()
        if type(r['app_version_code']) is not int or not 0 <= r['app_version_code'] <= 2**63-1: raise ValueError()
        v = r['session']
        if type(v) is not dict or not {'duration_seconds','samples','events'} <= v.keys() or v.keys()-{'duration_seconds','samples','events','video','playback'}: raise ValueError()
        if type(v['duration_seconds']) is not int or not 0 <= v['duration_seconds'] <= 180: raise ValueError()
        if type(v['samples']) is not list or len(v['samples']) > 37: raise ValueError()
        previous = -1
        for sample in v['samples']:
            if type(sample) is not dict or set(sample) != {'elapsed_seconds','available_memory_mb','low_memory','interactive'}: raise ValueError()
            t = sample['elapsed_seconds']; memory = sample['available_memory_mb']
            if type(t) is not int or not 0 <= t <= v['duration_seconds'] or t < previous: raise ValueError()
            previous = t
            if type(memory) is not int or not 0 <= memory <= 16384: raise ValueError()
            if type(sample['low_memory']) is not bool or type(sample['interactive']) is not bool: raise ValueError()
        if type(v['events']) is not list or len(v['events']) > 3: raise ValueError()
        for event in v['events']:
            if type(event) is not dict or event.get('kind') not in ('crash','native_crash','anr'): raise ValueError()
            validate(event)
            if event['package'] != r['package'] or event['app_version_code'] != r['app_version_code']: raise ValueError()
        if 'playback' in v:
            diagnostic = v['playback']
            if type(diagnostic) is not dict or not {'status','scope','records'} <= diagnostic.keys() or diagnostic.keys() - {'status','scope','records','renderer','renderer_32bit'}: raise ValueError()
            if diagnostic['scope'] != 'selected_app_and_shared_media' or diagnostic['status'] not in ('captured','partial','no_matching_records','unavailable'): raise ValueError()
            records = diagnostic['records']
            if type(records) is not list or len(records) > 48: raise ValueError()
            if any(type(line) is not str or len(line) > 240 or '\n' in line or '\r' in line for line in records): raise ValueError()
            if sum(len(line.encode('utf-8')) for line in records) > 8192: raise ValueError()
            for name in ('renderer','renderer_32bit'):
                if name in diagnostic and (type(diagnostic[name]) is not str or len(diagnostic[name]) > 80): raise ValueError()
        if 'video' in v:
            video = v['video']
            if type(video) is not dict or set(video) != {'bytes','sha256'}: raise ValueError()
            if type(video['bytes']) is not int or not 24 <= video['bytes'] <= MAX_VIDEO: raise ValueError()
            if type(video['sha256']) is not str or not re.fullmatch('[0-9a-f]{64}',video['sha256']): raise ValueError()
        return r
    if 'session' in r: raise ValueError()
    if r['kind'] == 'manual':
        if r.get('symptom') not in ('crash','slow','black_screen','playback','other'): raise ValueError()
        if set(r)-REQUIRED-{'symptom','application','description','package','app_version_code','app_version_name'}: raise ValueError()
        validate_details(r)
        if ('application' in r) != ('description' in r): raise ValueError()
        if ('package' in r) != ('app_version_code' in r): raise ValueError()
        if 'package' in r:
            if type(r['package']) is not str or len(r['package']) > 180 or not re.fullmatch(QUALIFIED,r['package']): raise ValueError()
            if type(r['app_version_code']) is not int or not 0 <= r['app_version_code'] <= 2**63-1: raise ValueError()
    else:
        if not {'package','app_version_code','exception_types','code_frames'} <= r.keys() or 'symptom' in r: raise ValueError()
        p=r['package']
        if type(p) is not str or len(p)>180 or (p!='android' and not re.fullmatch(QUALIFIED,p)): raise ValueError()
        if type(r['app_version_code']) is not int or not 0<=r['app_version_code']<=2**63-1: raise ValueError()
        a=r['exception_types']; f=r['code_frames']
        if type(a) is not list or len(a)>6 or type(f) is not list or len(f)>48: raise ValueError()
        for v in a:
            if type(v) is not str or len(v)>180 or not re.fullmatch(NAME+r'(?:\.'+NAME+r')*',v) or not v.endswith(('Exception','Error','Throwable')): raise ValueError()
        for v in f:
            if type(v) is not str or len(v)>240 or not FRAME.fullmatch(v): raise ValueError()
        if 'signal' in r and r['signal'] not in SIGNALS: raise ValueError()
    return r

def decode(raw):
    def pairs(items):
        r={}
        for k,v in items:
            if k in r: raise ValueError()
            r[k]=v
        return r
    def no_constant(v): raise ValueError()
    return validate(json.loads(raw, object_pairs_hook=pairs,parse_constant=no_constant))

def validate_mp4(path):
    # Structural container validation only; no server-side decoder executes uploaded media.
    total=path.stat().st_size;seen=set();position=0;boxes=0
    with path.open('rb') as stream:
        while position<total:
            header=stream.read(8)
            if len(header)!=8:raise ValueError()
            size,kind=struct.unpack('>I4s',header);header_size=8
            if size==1:
                extended=stream.read(8)
                if len(extended)!=8:raise ValueError()
                size=struct.unpack('>Q',extended)[0];header_size=16
            elif size==0:size=total-position
            if size<header_size or position+size>total:raise ValueError()
            if boxes==0 and (kind!=b'ftyp' or not 16<=size<=128):raise ValueError()
            if kind==b'ftyp':
                brand=stream.read(4)
                if brand not in (b'isom',b'iso2',b'mp41',b'mp42',b'avc1'):raise ValueError()
            seen.add(kind);position+=size;stream.seek(position);boxes+=1
            if boxes>128:raise ValueError()
    if not {b'ftyp',b'moov',b'mdat'}<=seen:raise ValueError()

class StorageFull(Exception):
    pass

class Inbox:
    def __init__(self, directory, password, upload_token):
        if len(upload_token)<32:raise ValueError("Missing upload credential")
        self.upload_token=upload_token
        self.directory=Path(directory);self.directory.mkdir(parents=True,exist_ok=True,mode=0o700)
        self.path=self.directory/'reports.sqlite3'; self.lock=threading.RLock()
        self.salt=secrets.token_bytes(16);self.password_hash=self.hash_password(password)
        self.sessions={};self.buckets={}
        self.upload_slots=threading.BoundedSemaphore(2)
        self.reserved_bytes=0
        self.video_dir=self.directory/'video';self.video_dir.mkdir(mode=0o700,exist_ok=True)
        with self.db() as db:
            db.execute('CREATE TABLE IF NOT EXISTS reports (id TEXT PRIMARY KEY, created INTEGER NOT NULL, package TEXT NOT NULL, kind TEXT NOT NULL, body TEXT NOT NULL, resolved INTEGER NOT NULL DEFAULT 0)')
        os.chmod(self.path,0o600)
    def hash_password(self,p): return hashlib.scrypt(p.encode(),salt=self.salt,n=16384,r=8,p=1)
    @contextlib.contextmanager
    def db(self):
        db=sqlite3.connect(self.path)
        try:
            db.execute('PRAGMA secure_delete=ON')
            db.execute('PRAGMA temp_store=MEMORY')
            yield db
            db.commit()
            # File removals happen only after the database transaction commits.
            # Failure to clean an orphan is retried on the next operation.
            try:
                with self.lock:self.cleanup_files(db)
            except (OSError,sqlite3.Error):pass
        except Exception:
            db.rollback();raise
        finally:db.close()
    def allow(self,key,limit):
        # Global buckets only: no client addresses or stable client identifiers, even in memory.
        with self.lock:
            now=time.monotonic();start,count=self.buckets.get(key,(now,0))
            if now-start>60:start,count=now,0
            self.buckets[key]=(start,count+1)
            return count<limit
    def free_bytes(self):
        st=os.statvfs(self.directory)
        return st.f_bavail * st.f_frsize
    @contextlib.contextmanager
    def reserve(self,size):
        if type(size) is not int or size < 0:raise ValueError()
        with self.lock:
            with self.db() as db:self.prune(db)
            if self.free_bytes() - self.reserved_bytes - size < MIN_FREE_BYTES:raise StorageFull()
            self.reserved_bytes += size
        try:yield
        finally:
            with self.lock:self.reserved_bytes -= size
    def prune(self,db):
        db.execute('DELETE FROM reports WHERE created < ?', (int(time.time())-RETENTION,))
        db.execute('DELETE FROM reports WHERE id IN (SELECT id FROM reports ORDER BY created DESC,id DESC LIMIT -1 OFFSET ?)',(MAX_REPORTS,))
    def cleanup_files(self,db):
        live={row[0] for row in db.execute('SELECT id FROM reports')}
        for p in self.video_dir.iterdir():
            if p.suffix == '.mp4' and p.stem not in live: p.unlink(missing_ok=True)
            elif p.suffix == '.part' and p.stat().st_mtime < time.time()-3600: p.unlink(missing_ok=True)
    def save_session(self,r,temp):
        body=json.dumps(r,ensure_ascii=True,sort_keys=True,separators=(',',':'))
        rid=r['incident_id'];target=self.video_dir/(rid+'.mp4');created=False
        with self.lock:
            try:
                with self.db() as db:
                    self.prune(db)
                    old=db.execute('SELECT body FROM reports WHERE id=?',(rid,)).fetchone()
                    if old:
                        if old[0]!=body or (temp and not target.is_file()):raise ValueError()
                        return rid
                    if temp:
                        used=sum(p.stat().st_size for p in self.video_dir.glob('*.mp4'))
                        if used+temp.stat().st_size>MAX_VIDEO_STORAGE:raise StorageFull()
                        os.replace(temp,target);created=True
                    db.execute('INSERT INTO reports(id,created,package,kind,body) VALUES(?,?,?,?,?)',
                        (rid,(int(time.time())//60)*60,r['package'],'session',body))
                    self.prune(db)
            except Exception:
                if created:target.unlink(missing_ok=True)
                raise
        return rid
    def save(self,r):
        body=json.dumps(r,ensure_ascii=True,sort_keys=True,separators=(',',':'))
        with self.lock,self.db() as db:
            self.prune(db);previous=db.execute('SELECT body FROM reports WHERE id=?',(r['incident_id'],)).fetchone()
            if previous:
                if previous[0]!=body:raise ValueError()
            else:
                db.execute('INSERT INTO reports(id,created,package,kind,body) VALUES(?,?,?,?,?)',(r['incident_id'],(int(time.time())//60)*60,r.get('package',r.get('application','manual')),r['kind'],body))
                self.prune(db)
        return r['incident_id']
    def session(self,cookie):
        try:
            c=http.cookies.SimpleCookie(cookie);token=c['inbox_session'].value
        except (http.cookies.CookieError,KeyError):return None
        with self.lock:
            now=time.time();self.sessions={k:v for k,v in self.sessions.items() if v[0]>now}
            return self.sessions.get(token)

STYLE='''body{font:16px system-ui,sans-serif;background:#f4f7fb;color:#13233a;max-width:1000px;margin:32px auto;padding:0 20px}section{background:white;border-radius:22px;padding:24px;margin:18px 0}a{color:#246dcc}button,input,select{font:inherit;padding:10px;border:1px solid #b8c8dd;border-radius:14px}button{background:#246dcc;color:white;cursor:pointer}pre{white-space:pre-wrap;overflow-wrap:anywhere;font-size:14px}section{overflow-x:auto}select{max-width:100%}table{width:100%;min-width:560px;border-collapse:collapse}td,th{text-align:left;padding:12px 5px;border-bottom:1px solid #e8eef5}.muted{color:#61748e}.danger{background:#a32d49}form{display:inline-block;margin:4px}label{display:block;margin:8px 0}h1{font-size:28px}'''
def page(title,body):return ('<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>'+html.escape(title)+'</title><style>'+STYLE+'</style><h1>'+html.escape(title)+'</h1>'+body+'</html>').encode()
def esc(v):return html.escape(str(v),quote=True)

class Handler(http.server.BaseHTTPRequestHandler):
    server_version='DiagnosticInbox'
    sys_version=''
    def log_message(self,*args):pass
    def log_error(self,*args):pass
    def setup(self):
        super().setup();self.connection.settimeout(5)
    @property
    def inbox(self):return self.server.inbox
    def reply(self,status,data,content='application/json; charset=utf-8',headers=None):
        if not isinstance(data,bytes):data=json.dumps(data,separators=(',',':')).encode()
        if content.startswith('text/html') and self.server.base:
            base=self.server.base.encode()
            data=data.replace(b'href="/',b'href="'+base+b'/').replace(b'action="/',b'action="'+base+b'/').replace(b'src="/',b'src="'+base+b'/')
        if headers and 'Location' in headers and headers['Location'].startswith('/'):
            headers=dict(headers);headers['Location']=self.server.base+headers['Location']
        self.send_response(status);self.send_header('Content-Type',content)
        self.send_header('Content-Length',str(len(data)))
        self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff')
        self.send_header('Referrer-Policy','no-referrer')
        self.send_header('Content-Security-Policy',"default-src 'none'; style-src 'unsafe-inline'; media-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'")
        for k,v in (headers or {}).items():self.send_header(k,v)
        self.end_headers();self.wfile.write(data)
    def error(self,status):
        if self.path.split('?')[0].startswith(self.server.base+'/api/'):
            return self.reply(status,{'error':'request_not_accepted'})
        messages={507:'反馈存储空间不足，请删除不需要的报告后再试。',400:'请求无法处理，请返回后重试。',403:'登录未成功，或页面已过期。请返回登录页面重试。',404:'这份报告不存在，或已经删除、过期。',429:'操作较频繁，请稍后再试。'}
        message=messages.get(status,'暂时无法完成，请稍后再试。')
        return self.reply(status,page('Bug 反馈', '<section><p>'+message+'</p><a href="/">返回</a></section>'),'text/html; charset=utf-8')
    def body(self,limit=MAX_BODY):
        if self.headers.get('Transfer-Encoding') is not None:raise ValueError()
        lengths=self.headers.get_all('Content-Length',[])
        if len(lengths)!=1 or not lengths[0].isdigit():raise ValueError()
        n=int(lengths[0]);
        if not 0<n<=limit:raise ValueError()
        b=self.rfile.read(n)
        if len(b)!=n:raise ValueError()
        return b
    def route(self):
        path=self.path
        base=self.server.base
        if base:
            if not path.startswith(base+'/'):raise ValueError()
            path=path[len(base):]
        return path
    def auth(self):
        return self.inbox.session(self.headers.get('Cookie',''))
    def do_GET(self):
        try:self.get()
        except Exception:self.error(500)
    def get(self):
        url=urllib.parse.urlsplit(self.route())
        if url.path=='/health':return self.reply(200,{'ok':True})
        session=self.auth()
        if not session:
            return self.reply(200,page('XZ1C Bug 反馈','<section><p>仅供维护者查看用户自愿提交的技术报告。</p><form method="post" action="/login"><label>管理密码 <input type="password" name="password" required autocomplete="current-password"></label><button>登录</button></form></section>'),'text/html; charset=utf-8')
        if url.path=='/video':return self.video(url)
        csrf=session[1];form='<input type="hidden" name="csrf" value="'+esc(csrf)+'">'
        with self.inbox.lock,self.inbox.db() as db:
            self.inbox.prune(db)
            if url.path=='/report':
                rid=urllib.parse.parse_qs(url.query).get('id',[''])[0]
                row=db.execute('SELECT id,body,resolved FROM reports WHERE id=?',(rid,)).fetchone()
                if not row:return self.error(404)
                r=json.loads(row[1]);body='<p><a href="/">返回报告列表</a></p><section><p>以下为用户提交的应用、描述、技术信息与可选录像。共享媒体记录可能来自其他播放器，须结合复现时间判断。</p><pre>'+esc(json.dumps(r,ensure_ascii=False,indent=2))+'</pre>'
                if r.get('session',{}).get('video'):
                    body+='<p>用户单独同意提交的录屏，可能含个人信息。仅用于排查，30天内删除。</p><video controls preload="none" style="max-width:100%" src="/video?id='+esc(rid)+'"></video>'
                for action,label,extra in [('resolve','标记为已处理' if not row[2] else '标记为待处理','<input type="hidden" name="resolved" value="'+str(0 if row[2] else 1)+'">'),('delete','删除这份报告','')]:
                    body+='<form method="post" action="/'+action+'">'+form+'<input type="hidden" name="id" value="'+esc(rid)+'">'+extra+'<button>'+label+'</button></form>'
                return self.reply(200,page('Bug 报告详情',body+'</section>'),'text/html; charset=utf-8')
            if url.path!='/':return self.error(404)
            selected=urllib.parse.parse_qs(url.query).get('package',[''])[0]
            packages=[x[0] for x in db.execute('SELECT DISTINCT package FROM reports ORDER BY package')]
            options='<option value="">全部应用</option>'+''.join('<option value="'+esc(p)+'"'+(' selected' if p==selected else '')+'>'+esc(p)+'</option>' for p in packages)
            rows=db.execute('SELECT id,created,package,kind,resolved FROM reports '+('WHERE package=? ' if selected else '')+'ORDER BY created DESC LIMIT 200', (selected,) if selected else ()).fetchall()
        remaining=self.inbox.free_bytes()
        storage_note=('可用空间约 %.2f GB；为管理和清理保留约 0.27 GB。' % (remaining / 1_000_000_000))
        if remaining <= MIN_FREE_BYTES + DB_WRITE_ALLOWANCE:storage_note+=' 空间不足，已暂停接收新报告；请删除不需要的报告。'
        body='<p class="muted">'+esc(storage_note)+'</p><p class="muted">报告保留30天，最多2000份。不保存来源地址、账号或设备标识；不提供远程操作手机的功能。</p><section><form method="get" action="/"><select name="package">'+options+'</select> <button>筛选</button></form><form method="post" action="/logout">'+form+'<button>退出登录</button></form><table><tr><th>接收时间</th><th>应用</th><th>类别</th><th>状态</th><th></th></tr>'
        labels={'crash':'崩溃','native_crash':'底层崩溃','anr':'无响应','manual':'手动反馈','session':'重现记录'}
        for rid,created,pkg,kind,resolved in rows:
            date=dt.datetime.fromtimestamp(created,dt.timezone.utc).strftime('%Y-%m-%d %H:%M')
            body+='<tr><td>'+esc(date)+' UTC</td><td>'+esc(pkg)+'</td><td>'+labels.get(kind,'问题')+'</td><td>'+('已处理' if resolved else '待处理')+'</td><td><a href="/report?id='+esc(rid)+'">查看</a></td></tr>'
        if not rows:body+='<tr><td colspan="5">暂无报告</td></tr>'
        return self.reply(200,page('XZ1C Bug 反馈',body+'</table></section>'),'text/html; charset=utf-8')
    def video(self,url):
        rid=urllib.parse.parse_qs(url.query).get('id',[''])[0]
        if not UUID.fullmatch(rid):return self.error(404)
        with self.inbox.lock,self.inbox.db() as db:
            self.inbox.prune(db)
            row=db.execute('SELECT body FROM reports WHERE id=?',(rid,)).fetchone()
            if not row or not json.loads(row[0]).get('session',{}).get('video'):return self.error(404)
            path=self.inbox.video_dir/(rid+'.mp4')
            if not path.is_file():return self.error(404)
            source=path.open('rb');size=path.stat().st_size
        with source:
            start,end,status=0,size-1,200
            requested=self.headers.get('Range')
            if requested:
                match=re.fullmatch(r'bytes=([0-9]*)-([0-9]*)',requested)
                if not match or not any(match.groups()):return self.reply(416,b'',headers={'Content-Range':'bytes */'+str(size)})
                a,b=match.groups()
                if not a:start=max(0,size-int(b))
                else:start=int(a);end=min(end,int(b)) if b else end
                if start>end or start>=size:return self.reply(416,b'',headers={'Content-Range':'bytes */'+str(size)})
                status=206
            self.send_response(status);self.send_header('Content-Type','video/mp4')
            self.send_header('Content-Length',str(end-start+1));self.send_header('Accept-Ranges','bytes')
            if status==206:self.send_header('Content-Range',f'bytes {start}-{end}/{size}')
            self.send_header('Content-Disposition','inline; filename="'+rid+'.mp4"')
            self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff')
            self.send_header('Referrer-Policy','no-referrer');self.send_header('Content-Security-Policy',"default-src 'none'; frame-ancestors 'none'")
            self.end_headers();source.seek(start);remaining=end-start+1
            while remaining:
                block=source.read(min(65536,remaining))
                if not block:break
                self.wfile.write(block);remaining-=len(block)
    def upload_session(self):
        if not hmac.compare_digest(self.headers.get('Authorization',''),'Bearer '+self.inbox.upload_token):return self.error(401)
        if not self.inbox.allow('sessions',4):return self.error(429)
        if self.headers.get_content_type()!='application/vnd.xz1c.bug-session':return self.error(415)
        if not self.inbox.upload_slots.acquire(False):return self.error(429)
        temp=None
        try:
            if self.headers.get('Transfer-Encoding') is not None:raise ValueError()
            lengths=self.headers.get_all('Content-Length',[])
            if len(lengths)!=1 or not lengths[0].isdigit():raise ValueError()
            total=int(lengths[0])
            if not 5<=total<=MAX_BODY+MAX_VIDEO+4:raise ValueError()
            header=self.rfile.read(4)
            if len(header)!=4:raise ValueError()
            count=struct.unpack('>I',header)[0]
            if not 0<count<=MAX_BODY or 4+count>total:raise ValueError()
            raw=self.rfile.read(count)
            if len(raw)!=count:raise ValueError()
            r=decode(raw)
            if r['kind']!='session':raise ValueError()
            meta=r['session'].get('video');size=meta['bytes'] if meta else 0
            if total!=4+count+size:raise ValueError()
            with self.inbox.reserve(size + DB_WRITE_ALLOWANCE):
                if size:
                    fd,name=tempfile.mkstemp(prefix='upload-',suffix='.part',dir=self.inbox.video_dir);temp=Path(name)
                    digest=hashlib.sha256();remaining=size;deadline=time.monotonic()+120
                    with os.fdopen(fd,'wb') as stream:
                        while remaining:
                            if time.monotonic()>deadline:raise ValueError()
                            block=self.rfile.read(min(65536,remaining))
                            if not block:raise ValueError()
                            stream.write(block);digest.update(block);remaining-=len(block)
                    if not hmac.compare_digest(digest.hexdigest(),meta['sha256']):raise ValueError()
                    validate_mp4(temp)
                rid=self.inbox.save_session(r,temp)
                return self.reply(201,{'report_id':rid})
        finally:
            if temp:temp.unlink(missing_ok=True)
            self.inbox.upload_slots.release()
    def do_POST(self):
        try:self.post()
        except StorageFull:self.reply(507,{'error':'storage_full'})
        except OSError as e:
            if e.errno == 28:self.reply(507,{'error':'storage_full'})
            else:self.error(500)
        except sqlite3.Error as e:
            if getattr(e,'sqlite_errorcode',None) == sqlite3.SQLITE_FULL:self.reply(507,{'error':'storage_full'})
            else:self.error(500)
        except (ValueError,TypeError,KeyError,OverflowError,RecursionError,UnicodeError):self.error(400)
        except Exception:self.error(500)
    def post(self):
        path=self.route()
        if path=='/api/sessions':return self.upload_session()
        if path=='/api/reports':
            if not hmac.compare_digest(self.headers.get('Authorization',''), 'Bearer '+self.inbox.upload_token):
                return self.error(401)
            if not self.inbox.allow('reports',30):return self.error(429)
            if self.headers.get_content_type()!='application/json':return self.error(415)
            report=decode(self.body())
            if report['kind']=='session':raise ValueError()
            with self.inbox.reserve(DB_WRITE_ALLOWANCE):
                rid=self.inbox.save(report)
            return self.reply(201,{'report_id':rid})
        if path=='/login':
            if not self.inbox.allow('login',10):return self.error(429)
            fields=urllib.parse.parse_qs(self.body(2048).decode(),max_num_fields=3)
            password=fields.get('password',[''])[0]
            if not hmac.compare_digest(self.inbox.password_hash,self.inbox.hash_password(password)):return self.error(403)
            with self.inbox.lock:
                if len(self.inbox.sessions)>=64:self.inbox.sessions.clear()
                token=secrets.token_urlsafe(32);self.inbox.sessions[token]=(time.time()+8*3600,secrets.token_urlsafe(24))
            cookie='inbox_session='+token+'; Path='+self.server.base+'/; HttpOnly; SameSite=Strict; Max-Age=28800'+('; Secure' if self.server.secure else '')
            return self.reply(303,{},headers={'Location':'/','Set-Cookie':cookie})
        session=self.auth()
        if not session:return self.error(403)
        fields=urllib.parse.parse_qs(self.body(1024).decode(),max_num_fields=5)
        if not hmac.compare_digest(fields.get('csrf',[''])[0],session[1]):return self.error(403)
        rid=fields.get('id',[''])[0]
        if path=='/logout':
            cookie=http.cookies.SimpleCookie(self.headers.get('Cookie',''))
            with self.inbox.lock:self.inbox.sessions.pop(cookie['inbox_session'].value,None)
            return self.reply(303,{},headers={'Location':'/','Set-Cookie':'inbox_session=; Path='+self.server.base+'/; Max-Age=0; HttpOnly; SameSite=Strict'})
        if not UUID.fullmatch(rid):return self.error(400)
        with self.inbox.lock,self.inbox.db() as db:
            if path=='/delete':
                db.execute('DELETE FROM reports WHERE id=?',(rid,))
            elif path=='/resolve':
                value=fields.get('resolved',[''])[0]
                if value not in ('0','1'):raise ValueError()
                db.execute('UPDATE reports SET resolved=? WHERE id=?',(int(value),rid))
            else:return self.error(404)
        return self.reply(303,{},headers={'Location':'/'})

class Server(http.server.ThreadingHTTPServer):
    daemon_threads=True
    def __init__(self,address,inbox,secure=False,base=""):
        if base and not re.fullmatch(r"/[a-z0-9-]+",base):raise ValueError()
        self.base=base;self.last_prune=0
        self.inbox=inbox;self.secure=secure;self.slots=threading.BoundedSemaphore(12)
        super().__init__(address,Handler)
    def service_actions(self):
        if time.monotonic()-self.last_prune>3600:
            try:
                with self.inbox.lock,self.inbox.db() as db:self.inbox.prune(db)
            except (sqlite3.Error,OSError):pass
            self.last_prune=time.monotonic()
    def handle_error(self,*args):pass
    def process_request(self,request,address):
        if not self.slots.acquire(False):request.close();return
        try:super().process_request(request,address)
        except Exception:self.slots.release();request.close()
    def process_request_thread(self,request,address):
        try:super().process_request_thread(request,address)
        finally:self.slots.release()

if __name__=='__main__':
    os.umask(0o077)
    password=os.environ.get('INBOX_ADMIN_PASSWORD','')
    if len(password)<20:raise SystemExit('Set INBOX_ADMIN_PASSWORD to at least 20 characters.')
    upload_token=os.environ.get('INBOX_UPLOAD_TOKEN','')
    if len(upload_token)<32:raise SystemExit('Set INBOX_UPLOAD_TOKEN privately.')
    directory=os.environ.get('INBOX_DATA_DIR',str(Path.home()/'.xz1c-feedback'))
    public_url=os.environ.get('INBOX_PUBLIC_URL','http://127.0.0.1:18776')
    url=urllib.parse.urlsplit(public_url)
    if url.scheme!='https' and not (url.scheme=='http' and url.hostname in ('127.0.0.1','localhost')):raise SystemExit('Public deployment requires HTTPS.')
    server=Server(('127.0.0.1',int(os.environ.get('INBOX_PORT','18776'))),Inbox(directory,password,upload_token),url.scheme=='https',url.path.rstrip('/'))
    server.serve_forever(poll_interval=0.5)
