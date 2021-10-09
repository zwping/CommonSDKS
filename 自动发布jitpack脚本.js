// 你只管修改代码，git提交 tag++ jitpack发布均由该脚本完成

/*
 基于nodejs自动发布jitpack

 使用
    复制当前js脚本至项目根目录 [自定义域名需要手动添加]
    [export NODE_PATH=$(npm root -g)]
    npm install -g shelljs axios
    node 自动发布jitpack脚本.js

 脚本执行过程
    进入仓库工程目录
    读取基础配置
    判断文件是否修改**
    增加版本号/名
    增加tag
    push代码
    pushtag
    同步代码至jitpack
    触发jitpack build
    轮询查看build结果
    [自定义域名同步 编译]
*/
'use strict'

var cfg = {
    root: '', // 根目录
    groupId: '', // zwping
    artifactId: '', // alibx
    vname: '',
    vcode: '',
    newVName: '', // 值等于tag值
    newVCode: '',

    cusGroupId: 'com.zwping', // 自定义域名, 需要单独配置
}

if (!cfg.root) cfg.root = __dirname

var fs = require('fs')
var axios = require('axios')
var shell = require('shelljs')
function echo(msg) { // 日志输出
    // shell.echo(msg)
    console.log('[log]: ' + msg)
}

if (shell.cd(cfg.root).code !== 0) { 
    echo(`工程根目录错误${cfg.root}`); return
}
echo(`进入工程根目录成功 ${cfg.root}`)

if (!shell.which('git')) {
    echo('sorry, 当前环境不支持git')
    shell.exit(1)
    return
}

var remotev = shell.exec('git remote -v').split(' ')[0].split('\t')[1]
echo(`远程仓库地址: ${remotev}`)

if (remotev.startsWith('git@')) {
    cfg.groupId = remotev.split(':')[1].split('/')[0]
    cfg.artifactId = remotev.split('/')[1].replace('.git', '')
}

// echo(getDomainName())
// return

var status = shell.exec('git status')
// echo(status)
if (status.indexOf('nothing to commit') !== -1 && 
    status.indexOf('git push') === -1 // commit过后未push | push失败
    ) {
        echo('未有代码更改 end'); return
}

var mFileNum = shell.exec('git status -s -uno | wc -l').trim()
echo(`已修改 ${mFileNum} 处文件`)

function autoAddLibVersion() {
    var dirs = shell.ls('').filter(f => fs.lstatSync(`${cfg.root}/${f}`).isDirectory())
    // echo(`根目录下有以下文件夹${dirs}`)
    dirs = dirs.map(f => `${cfg.root}/${f}`).filter(f => shell.ls(f).indexOf('build.gradle') !== -1)
    for (var f of dirs) {
        var file = `${f}/build.gradle`
        var content = fs.readFileSync(file, 'utf-8')
        if (content.indexOf('com.android.library') !== -1) {
            echo(`依赖库 ${f}`)
            var code, code1, name, name1
            var lines = content.split('\n')
            for (var line of lines) {
                if (line.indexOf('versionCode') !== -1) {
                    cfg.vcode = line.replace('versionCode', '').trim()
                    cfg.newVCode = cfg.vcode*1 + 1
                    code = line
                    code1 = line.replace(cfg.vcode, cfg.newVCode)
                    continue
                }
                if (line.indexOf('versionName') !== -1) {
                    cfg.vname = line.replace('versionName', '').replace(/\"/g, '').trim()
                    var n = cfg.vname.split('.')
                    n = (n[0]*100 + n[1]*10 + n[2]*1)*1 + 1
                    cfg.newVName = n.toString().padStart(3, '0').split('').join('.')
                    name = line
                    name1 = line.replace(cfg.vname, cfg.newVName)
                    continue
                }
            }
            echo(`版本号: ${cfg.vcode} -> ${cfg.newVCode}`)
            echo(`版本名: ${cfg.vname} -> ${cfg.newVName}`)
            content = content.replace(code, code1).replace(name, name1)
            fs.writeFileSync(file, content, err => {
                echo(`文件修改失败 ${err} ${file}`); return
            })
            echo(`文件修改成功${file}`)
        }
    }
}
autoAddLibVersion()

shell.exec('git add .')
shell.exec("git commit -m 'auto sync code'")
echo('code push...')
var branch = shell.exec('git branch').split('\n').filter(it => it.indexOf('*') !== -1)[0]
branch = branch.replace('*', '').trim() // 手动获取当前分支 main/master
shell.exec(`git push origin ${branch}`)
echo('code push成功')
shell.exec(`git tag ${cfg.newVName}`)
echo('tag push...')
shell.exec(`git push origin ${cfg.newVName}`)
echo('tag push成功')

echo(`cfg对象 ${JSON.stringify(cfg)}`)

var errNum = 0
function get_builds() {
    var url = `https://jitpack.io/api/builds/${getDomainName()}/${cfg.artifactId}/${cfg.newVName}`
    echo(`${getCusBuildTag()}正在查询jitpack build结果`)
    axios.get(url)
        .then(r => {
            echo(JSON.stringify(r.data))
            if (!r.data.status ) {
                echo(`${getCusBuildTag()}查询异常 ${url} ${JSON.stringify(r.data)}`); return
            }
            if (r.data.status === 'ok') {
                echo(`${getCusBuildTag()}恭喜您, 自动发布jitpack成功 /撒花`)
                echo(`${getCusBuildTag()}maven { url 'https://jitpack.io' }`)
                var multi = r.data.modules.length>0 ? `:[${r.data.modules}]` : '' // 多lib
                echo(`${getCusBuildTag()}implementation '${getDomainName()}.${cfg.artifactId}${multi}:${cfg.newVName}'`)
                get_downs()
                if (cfg.cusGroupId) {
                    errNum=0; cfg.cusGroupId = ''; get_refs(); // 第二轮同步编译
                }
                return
            }
            get_builds()
        })
        .catch(err => {
            if (++errNum > 60*3) {
                echo(`${getCusBuildTag()}jitpack 编译失败 ${err}`)
                echo(url)
                return
            }
            if (errNum%10 === 0) echo(`${getCusBuildTag()}正在重查(${errNum})jitpack build`)
            setTimeout(() => get_builds(), 1000)
        })
}

function get_downs() {
    var url = `https://jitpack.io/api/downloads/${getDomainName()}/${cfg.artifactId}`
    echo(`${getCusBuildTag()}正在查询用户使用情况`)
    axios.get(url)
    .then(r => {
        echo(`${getCusBuildTag()}${JSON.stringify(r.data)}`)
    })
    .catch(err => {
        echo(`${getCusBuildTag()}${err} ${url}`)
    })
}


var refsNum = 0
function get_refs() {
    echo(`${getCusBuildTag()}正在同步 jitpack from github`)
    var url = `https://jitpack.io/api/refs/${getDomainName()}/${cfg.artifactId}`
    axios.get(url)
        .then(r => {
            // echo(JSON.stringify(r.data))
            ++refsNum
            var hasTag = r.data.tags.filter(it => it.tag_name === cfg.newVName)
            if (hasTag.length === 0) {
                setTimeout(() => get_refs(), 1000); return
            }
            if (refsNum > 30) {
                echo(`${getCusBuildTag()}同步失败 ${url}`); return
            }
            get_builds()
        })
        .catch(err => {
            echo(`${getCusBuildTag()}get_refs ${err} ${url}`)
        })
}

get_refs()

function getDomainName() {
    return cfg.cusGroupId ? `${cfg.cusGroupId}` : `com.github.${cfg.groupId}`
}
function getCusBuildTag() {
    return cfg.cusGroupId ? '[自定义域名] ' : ''
}
