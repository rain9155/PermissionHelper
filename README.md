# PermissionHelper 2.x

### 简化android6.0动态权限申请过程，一行代码搞定权限申请，可以一次申请单个或多个权限，支持特殊权限的申请，欢迎大家star、fork，如有问题请[issue](https://github.com/rain9155/PermissionHelper/issues)

## Preface

在原来申请一个权限需要2步，第一步：第一步在需要申请权限的地方检查该权限是否被同意，如果同意了就直接执行，如果不同意就动态申请权限；第二步：重写Activity或Fragment的onRequestPermissionsResult方法，在里面根据grantResults数组判断权限是否被同意，如果同意就直接执行，如果不同意就要进行相应的提示，如果用户勾选了“don't ask again”，还要引导用户去“settings”界面打开权限，这时还要重写onActivityResult判断权限是否被同意。

就是这简单的两步，却夹杂了大量的if()else()语句，不但不优雅，而且每次都要写同样的样板代码，特别繁琐，所以针对这种情况，结合日常开发需要，我使用PermissionHelper封装了权限请求逻辑，在底层通过一个没有界面的Fragment代理权限申请的过程，通过链式调用让开发者一行代码完成权限的申请，简化了权限请求过程，不用每次写重复的代码。

## Feature

- [x] 支持特殊权限的申请，申请时和其他权限的申请步骤一样
- [x] 具有生命周期感应能力，只在界面可见时才发起请求和回调结果
- [x] 系统配置更改(例如屏幕旋转)后能够恢复之前权限申请流程，不会中断权限申请流程(有特殊场景，见后面[Other](#Other)说明)
- [x] 灵活性高，可以设置请求、拒绝发生时回调，在回调发生时暂停权限申请流程，然后根据用户意愿再决定是否继续权限申请流程
- [x] 已适配到android 11(后台定位权限独立申请、MANAGE_EXTERNAL_STORAGE权限申请)

## Preview

![permission](/screenshots/permission.gif)

## Download

[点击下载查看示例](https://github.com/rain9155/PermissionHelper/raw/update/app/apk/app-debug.apk)

## How to install ？

在项目根目录的build.gradle中引入仓库：

```groovy
allprojects {
    repositories {
        mavenCentral()
    }
}
```

然后在项目的app目录下的build.gradle中引入依赖：

```groovy
dependencies {
    implementation 'io.github.rain9155:permissionhelper:2.0.0'
    //PermissionHelper还需要依赖appcompat库，版本号多少都可以
    implementation "androidx.appcompat:appcompat:1.x.x"
}
```
同时应用的targetSdkVersion要 **>= 23**，PermissionHelper不兼容api <= 22的[AppOpsManager](https://developer.android.com/reference/android/app/AppOpsManager)的权限检查逻辑。

## How to use ?

通过`PermissionHelper`的`with`静态方法获取`PermissionHelper`实例，`with`方法支持传入activity或fragment实例，然后拼接`permissions`方法传入要请求的权限，最后拼接`request`方法发起请求，最终在`IResultCallback#onResult`回调中处理授权结果：

```kotlin
//要请求的权限
val permissions = listOf(Manifest.permission.CALL_PHONE,Manifest.permission.WRITE_SETTINGS)

//调用PermissionHelper的request方法向用户申请权限
PermissionHelper.with(this)
    .permissions(permissions)
    .request(object : IResultCallback {
        override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
            //授权结果回调：
            //isAllGrant：true时表示用户同意了所有权限，false时表示用户拒绝了某些权限
            //grantedPermissions：用户同意的权限
            //rejectedPermissions：用户拒绝的权限
        }
    })
```

当用户拒绝了某些权限时，应用可能无法继续进行下去，这时我们需要向用户解释被拒绝的权限对应用的必要性，以征得用户再次同意，你可以直接在`IResultCallback#onResult`回调中当isAllGrant为false时弹出弹窗向用户解释原因，然后当用户同意时再次调用`PermissionHelper#request`方法传入要申请的权限再次发起申请，这样会出现回调嵌套的情况，难免会有点不优雅，`PermissionHelper`支持在调用`request`方法前拼接一个`explainAfterRejected`方法，传入`IRejectedCallback`实现，当用户拒绝了某些权限后，`IRejectedCallback#onRejected`方法就会回调，在该回调中，你可以弹出弹窗向用户解释被拒绝的权限对应用的必要性:

```kotlin
val permissions = listOf(Manifest.permission.CALL_PHONE,Manifest.permission.WRITE_SETTINGS)

PermissionHelper.with(this)
    .permissions(permissions)
    .explainAfterRejected(object : IRejectedCallback {
        override fun onRejected(process: IRejectedCallback.IRejectedProcess, rejectedPermissions: List<String>) {
            //当用户拒绝了某些权限后, 该回调先于IResultCallback#onResult回调，可以在这里弹出弹窗向用户解释被拒绝的权限对应用的必要性：
            //process：用户同意再次申请权限时，调用process的相应方法，继续权限申请流程
            //rejectedPermissions：被用户拒绝的权限
    }).request(object : IResultCallback {
        override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
            //授权结果回调
        }
    })
```

`IRejectedCallback#onRejected`回调中的process参数是`IRejectedProcess`类型，它里面只有两个方法：`IRejectedProcess#requestAgain`方法和`IRejectedProcess#rejectRequest`方法，用户同意再次申请权限时调用`IRejectedProcess#requestAgain`方法传入继续申请的权限再次发起权限申请流程，当用户不同意再次申请权限时，调用`IRejectedProcess#rejectRequest`方法终止权限的申请流程，紧接着`IResultCallback#onResult`就会回调，可以在里面做最终的结果处理，如果用户同意再次申请权限，但在二次权限申请的过程中勾选了**不再询问选项**(android 11后连续点击多次拒绝等同于勾选了Dont Ask again)，那么该权限就会被用户**永久拒绝**，下一次请求时不会出现该权限的申请框，针对这种情况，我们需要引导用户到设置界面同意该权限，`PermissionHelper`支持在调用`request`方法前拼接一个`explainAfterRejectedForever`方法，传入`IRejectedForeverCallback`实现，当用户永久拒绝了某些权限后，`IRejectedForeverCallback#onRejectedForever`方法就会回调，在该回调中，你可以弹出弹窗再次向用户解释被拒绝的权限对应用的必要性：

```kotlin
val permissions = listOf(Manifest.permission.CALL_PHONE,Manifest.permission.WRITE_SETTINGS)

PermissionHelper.with(this)
    .permissions(permissions)
    .explainAfterRejected(object : IRejectedCallback {
        override fun onRejected(process: IRejectedCallback.IRejectedProcess, rejectedPermissions: List<String>) {
           //当用户 第一次拒绝 了某些权限后, 该回调先于IResultCallback#onResult回调，可以在这里弹出弹窗向用户解释被拒绝的权限对应用的必要性
        }
    }).explainAfterRejectedForever(object : IRejectedForeverCallback {
        override fun onRejectedForever(process: IRejectedForeverCallback.IRejectedForeverProcess, rejectedForeverPermissions: List<String>) {
            //当用户 永久拒绝 了某些权限后, 该回调先于IResultCallback#onResult回调，可以在这里弹出弹窗向用户解释被拒绝的权限对应用的必要性
            //process：用户同意去设置界面时，调用process的相应方法，继续权限申请流程
            //rejectedForeverPermissions：被用户永久拒绝的权限
        }
    }).request(object : IResultCallback {
        override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
            //授权结果回调
        }
    })
```

`IRejectedForeverCallback#onRejectedForever`回调中的process参数是`IRejectedForeverProcess`类型，它里面只有两个方法：`IRejectedForeverProcess#gotoSettings`方法和`IRejectedForeverProcess#rejectRequest`方法，当用户同意去设置界面时调用`IRejectedForeverProcess#gotoSettings`方法前往设置页面，当用户不同意时，调用`IRejectedForeverProcess#rejectRequest`方法终止权限的申请流程，紧接着`IResultCallback#onResult`就会回调，可以在里面做最终的结果处理，除了在权限被拒绝后向用户解释原因，`PermissionHelper`还支持在权限发起申请前向用户解释原因，这样用户后续同意的意愿更大，向前面一样，`PermissionHelper`可以在调用`request`方法前拼接一个`explainBeforeRequest`方法，传入`IRequestCallback`实现，当请求发起前，`IRequestCallback#onRequest`方法就会回调，在该回调中，你可以弹出弹窗向用户解释要申请的权限对应用的必要性：

```kotlin
val permissions = listOf(Manifest.permission.CALL_PHONE,Manifest.permission.WRITE_SETTINGS)

PermissionHelper.with(this)
    .permissions(permissions)
    ..explainBeforeRequest(object : IRequestCallback {
        override fun onRequest(process: IRequestCallback.IRequestProcess, requestPermissions: List<String>) {
            //当 发起 权限请求前，该回调先于后面所有回调回调，可以在这里弹出弹窗向用户解释要申请的权限对应用的必要性
            //process：用户同意后，调用process的相应方法，继续权限申请流程
            //requestPermissions：即将要请求的权限(不包含已经被授予的权限, requestPermissions <= permissions)
        }
    }).explainAfterRejected(object : IRejectedCallback {
        override fun onRejected(process: IRejectedCallback.IRejectedProcess, rejectedPermissions: List<String>) {
           //当用户 第一次拒绝 了某些权限后, 该回调先于IResultCallback#onResult回调，可以在这里弹出弹窗向用户解释被拒绝的权限对应用的必要性
        }
    }).explainAfterRejectedForever(object : IRejectedForeverCallback {
        override fun onRejectedForever(process: IRejectedForeverCallback.IRejectedForeverProcess, rejectedForeverPermissions: List<String>) {
            //当用户 永久拒绝 了某些权限后, 该回调先于IResultCallback#onResult回调，可以在这里弹出弹窗向用户解释被拒绝的权限对应用的必要性
        }
    }).request(object : IResultCallback {
        override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
            //授权结果回调
        }
    })
```

`IRequestCallback#onRequest`回调中的process参数是`IRequestProcess`类型，它里面只有两个方法：`IRequestProcess#requestContinue`方法和`IRequestProcess#rejectRequest`方法，当用户同意继续申请权限时调用`IRejectedForeverProcess#requestContinue`方法恢复权限申请流程，当用户不同意时，调用`IRejectedForeverProcess#rejectRequest`方法终止权限的申请流程，如果设置了`IRejectedCallback`，紧接着`IRejectedCallback#onRejected`就会回调，否则`IResultCallback#onResult`就会回调，可以在里面做最终的结果处理。

除此之外PermissionHelper还提供了一些跟权限相关的工具方法：

```kotlin
//检查单个权限是否被授予，返回boolean值
val isGrant = PermissionHelper.checkPermission(this, Manifest.permission.CALL_PHONE)

//检查多个权限是否被授予，返回还未被授予的权限列表，如果返回的列表为empty，说明传进的所有权限都被授予了
val permissions = listOf(Manifest.permission.CALL_PHONE,Manifest.permission.WRITE_SETTINGS, Manifest.permission.SYSTEM_ALERT_WINDOW)
val rejectedPermissions = PermissionHelper.checkPermissions(this, permissions)

//跳转到不同厂商的权限设置界面，如果跳转失败(不支持的厂商)，则跳转到应用详情页
PermissionHelper.gotoSettings(this)

//跳转到不同厂商的权限设置界面，如果跳转失败(不支持的厂商)，则跳转到应用详情页, 可以传入requestCode，须自己在activity中重写onActivityResult，根据requestCode再次检查权限
PermissionHelper.gotoSettings(this, requestCode = 0x001)
```

## Other

上面就是`PermissionHelper`的基本使用方法，但是在权限申请中还有一些特殊场景会导致权限请求流程被中断，这些都是无法避免的，需要另外说明：

1、前往设置页面拒绝某些权限返回后，app进程会销毁重建，这时使用`PermissionHelper`进行的权限请求流程就会被中断，这是因为在进程被重建了，`PermissionHelper`保存的数据无法被恢复，不过这也是系统的行为，`PermissionHelper`也无法避免，但是我们可以针对这种情况作出一些优化，参考[Android在应用设置里关闭权限，返回生命周期处理](https://www.jianshu.com/p/cb68ca511776)，我们可以在权限请求的页面中通过savedInstanceState判断app进程是否被重建，如果app进程被重建了，我们就直接回到app的启动页；

2、android 11后申请安装外部来源应用权限后，app进程会销毁重建，这时使用`PermissionHelper`进行的权限请求流程就会被中断，原因和1一样，解决办法也可以参考1，对于android 11这个行为变更可以参考[Android 11特性调整：安装外部来源应用需要重启APP](https://cloud.tencent.com/developer/news/637591)；

3、`PermissionHelper`在系统配置变更后(例如屏幕旋转)也可以恢复之前的权限请求流程，如果你设置了`explainBeforeRequest`、`explainAfterRejected`或`explainAfterRejectedForever`回调，需要你在回调发生时调用对应`IProcess`的方法才可以继续权限请求流程，否则就会中断权限的请求流程，同时如果当回调发生时恰好发生系统配置变更，那么回调中与用户交互的部分就会丢失，例如你在回调中弹出了一个弹窗向用户解释权限申请原因，需要用户点击弹窗的确定或取消按钮才会继续调用`IProcess`的相应方法，那么当系统配置发生变更后，弹窗就会消失，这时用户就没法点击弹窗相应按钮，就会由于没有调用`IProcess`的相应方法中断权限的申请流程，所以`PermissionHelper`针对这种情况，支持当系统配置变更后再次回调相应的回调，从而恢复权限申请流程，如果不需要，可以在调用`request`方法前拼接`reCallbackAfterConfigurationChanged`方法传入false，默认为true.

## License

```
Copyright 2019 rain9155

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License a

          http://www.apache.org/licenses/LICENSE-2.0 
          
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```





