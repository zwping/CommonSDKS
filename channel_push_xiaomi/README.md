### aar二次打包

1. 随意创建一个library module
2. 删掉 \src\main\*文件
3. 更改aar后缀zip, 解压
4. 将classes.jar, libs\*.jar 移入library module\libs文件夹中
    a. 将其余文件移入\src\main\文件夹内
5. 精简library build.gradle文件, 如有需要引用的*.jar声明引用`api fileTree(dir: 'libs', includes: ['MiPush_SDK_Client_5_0_3-C.jar',])`
6. 混淆移出
7. 声明`id 'com.github.dcendents.android-maven'`