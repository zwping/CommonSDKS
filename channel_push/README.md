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