### 渠道推送 Huawei Xiaomi Oppo Vivo

### 使用

#### 华为

1. 根gradle声明maven url

```groovy
buildscript {
    repositories {
        ...
        // 配置HMS Core SDK的Maven仓地址。
        maven {url 'https://developer.huawei.com/repo/'}
    }
    dependencies {
        ...
        // 增加agcp插件配置。
        classpath 'com.huawei.agconnect:agcp:1.4.2.300'
    }
}
allprojects {
    repositories {
        ...
        // 配置HMS Core SDK的Maven仓地址。
        maven {url 'https://developer.huawei.com/repo/'}
    }
}
```

2. app模块什么


### <d>aar二次打包</d>

> oppo aar嵌套太深, 终放弃

1. 随意创建一个library module
2. 删掉 \src\main\*文件
3. 更改aar后缀zip, 解压
4. 将classes.jar, libs\*.jar 移入library module\libs文件夹中
   a. 将其余文件移入\src\main\文件夹内
5. 精简library build.gradle文件, 如有需要引用的*.jar声明引用`api fileTree(dir: 'libs', includes: ['MiPush_SDK_Client_5_0_3-C.jar',])`
6. 混淆移出
7. 声明`id 'com.github.dcendents.android-maven'`