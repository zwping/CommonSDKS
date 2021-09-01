#!/usr/bin/env python3
#coding=utf-8

import os
import json
import requests
import sys
import time

# from requests import status_codes
# from requests_toolbelt import MultipartEncoderMonitor
# from androguard.core.bytecodes.apk import APK

""" Android 一键发布源码至Jitpack
@author: zwping

1.  git保存所有代码
2.  获取最新tag+0.0.1, 并记录tag值
3.  push github
4.  jitpack主动触发Get
5.  循环超时 获取最新版本号==tag
"""

cfg = {
    'loc_dir': '/Users/feiyan/zwping/commonsdks',
    'gth_dir': 'https://github.com/zwping/CommonSDKS'
}

def _log(msg, *args):
    """ 日志打印
    :param args: 支持 默认打印(msg) 成功打印(msg, suc) 错误打印(msg, err)
    """
    t = 1 if ('suc' in args or 'err' in args) else 0
    c = 32 if 'suc' in args else (31 if 'err' in args else '')
    print('\033[%d;%sm%s\033[0m' % (t, c, msg))


def _progress(cur, total):
    i = cur*100/total
    i1 = int(i/5)
    i2 = 20 - i1
    print('\r|\033[7;36;46;m{0}\033[0m{1}\033[0m|   {2}%'.format('|'*i1, ' '*i2, int(i)),  end = '')


def main():
    datas = {
        
    }
    _log('-- 检测脚本指令 ---', 'suc')
    if len(sys.argv) != 3:
        _log('指令格式错误', 'err')
        _log('当前指令: python ' + ' '.join([s for s in sys.argv]))
        _log('指令格式: python *.py dir token')
        return
    datas['dir'] = sys.argv[1]
    datas['token'] = sys.argv[2]

    _log('-- 获取最新编译的APK --', 'suc')
    paths = [os.path.join(datas['dir'], name)
             for name in os.listdir(datas['dir'])]
    paths = list(filter(lambda p: os.path.isfile(p) and os.path.splitext(p)[1] == '.apk',
                        paths))
    paths.sort(key=lambda name: os.path.getmtime(name), reverse=True)
    if len(paths) == 0:
        _log('未找到apk %s' % datas['dir'], 'err')
        return
    datas['f_path'] = paths[0] # 获取最大更新时间的APK文件

    _log('-- 初始化参数 --', 'suc')
    datas['f_name'] = os.path.basename(datas['f_path'])
    datas['f_size'] = os.path.getsize(datas['f_path'])
    datas['f_mtime'] = time.strftime("%Y-%m-%d %H:%M:%S",time.localtime(os.path.getmtime(datas['f_path'])))
    apk = APK(datas['f_path'])  # https://androguard.readthedocs.io/en/latest/
    datas['bundle_id'] = apk.package
    datas['name'] = apk.get_app_name()
    datas['v_code'] = apk.androidversion['Code']
    datas['v_name'] = apk.androidversion['Name']
    datas['fircode'] = 1
    # for d in apk.xml['AndroidManifest.xml'].xpath('//application/meta-data'):
    #     if len(d.values()) == 2 and d.values()[0] == 'fircode':
    #         datas['fircode'] = d.values()[1]
    rlog = requests.get('http://api.bq04.com/apps/latest/' + datas['bundle_id'], 
            {'type': 'android', 'api_token': datas['token']})
    if rlog.status_code != 200:
        _log('获取当前bundle_id对应更新日志失败 %s %s' % (rlog.status_code, rlog.text))
        return
    changelog = json.loads(rlog.text)['changelog']
    print(rlog.request.url)
    print(changelog)
    if changelog :
        datas['fircode'] = int(changelog) + 1
    print(datas['fircode'])
    for (k, v) in datas.items():
        _log('%s %s' % (k, '%.2fMB' % (v / 1024 / 1024) if (k=='f_size') else v))

    _log('-- 获取Fir凭证 --', 'suc')
    r = requests.post('https://api.bq04.com/apps',
                      {'type': 'android',
                       'bundle_id': datas['bundle_id'],
                       'api_token': datas['token'],
                       })
    if r.status_code != 201:
        _log('获取Fir上传凭证失败 %s %s' % (r.status_code, r.text), 'err')
        return
    c = json.loads(r.text)['cert']['binary']  # 凭证

    _log('-- 上传APK文件 --', 'suc')
    _log(datas['f_name'], 'suc')
    m = MultipartEncoderMonitor.from_fields(
        fields={
            'key': c['key'],
            'token': c['token'],
            'x:name': datas['name'],
            'x:version': datas['v_name'],
            'x:build': datas['v_code'],
            'x:changelog': str(datas['fircode']),
            'file': (datas['name'], open(datas['f_path'], 'rb'))},
        callback=lambda it: _progress(it.bytes_read, datas['f_size']))
    r = requests.post(c['upload_url'],
                      data=m,
                      headers={'Content-Type': m.content_type})
    _log('')
    if r.status_code != 200:
        _log('apk上传失败', 'err')
        _log(r.text)
        return
    _log('apk上传成功', 'suc')

    _log('-- 获取Fir最新版本 --', 'suc')
    r = requests.get('http://api.bq04.com/apps?api_token=%s' % datas['token'])
    if r.status_code != 200:
        _log('Fir最新版本获取失败', 'err')
        _log(r.text)
        return
    d = json.loads(r.text)
    for d1 in d['items']:
        if (d1['bundle_id'] == datas['bundle_id']):
            _log('%s (%s %s %s)' % (
                d1['name'], d1['type'], d1['master_release']['build'], d1['master_release']['version']))
            _log(time.strftime("%Y-%m-%d %H:%M:%S",
                               time.localtime(d1['updated_at'])))
            _log('http://%s/%s' % (d1['download_domain'], d1['short']))
            break

    _log('-- Fir更新完成 --', 'suc')

def git_next_tag() -> str:
    from urllib.parse import urlparse
    url = urlparse(cfg['gth_dir'])
    path = url.path.split('/')
    _log('获取tags limit 5')
    url = 'https://jitpack.io/api/refs/com.github.%s/%s' % (path[1], path[2])
    r = requests.get(url)
    tags = r.json()['tags']
    # 只支持x.y.z格式的tag
    maxTag = list(filter(lambda x: len(x)==5 ,list(d['tag_name'] for d in tags)))[0]
    _log('最新Tag: %s' % maxTag)
    # 001*10
    maxTag = int(maxTag.replace('.', '')) * 10
    # 10+10/10 = 2
    nextTag = int((maxTag + 10) / 10)
    # 0.0.2
    nextTag = '.'.join(list(d for d in str(nextTag).zfill(3)))
    return nextTag

def git_add_all():
    _log(os.popen('git add .'))
    _log(os.popen('git commit -m %s' % '一键更新Jitpack'))
    pass

"""
https://github.com/jitpack/jitpack.io/blob/master/ANDROID.md
"""
def jitpack_get():
    pass


try:
    tag = git_next_tag()
    _log('新Tag: %s' % tag)
    _log(os.popen('cd %s' % cfg['loc_dir']))
    _log(os.popen('git add .'))
    _log("git commit -m '%s'" % '一键更新Jitpack')
    # _log(os.popen())
except Exception as e:
    _log('main() %s' % e, 'err')
