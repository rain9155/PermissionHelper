# PermissionHelper

### 简化android6.0动态权限申请过程，一行代码搞定权限申请，可以一次申请单个或多个权限，支持特殊权限的申请，欢迎大家star、fork，如有问题请[issue](https://github.com/rain9155/PermissionHelper/issues)

## Preface

在原来申请一个权限需要2步，第一步：第一步在需要申请权限的地方检查该权限是否被同意，如果同意了就直接执行，如果不同意就动态申请权限；第二步：重写Activity或Fragment的onRequestPermissionsResult方法，在里面根据grantResults数组判断权限是否被同意，如果同意就直接执行，如果不同意就要进行相应的提示，如果用户勾选了“don't ask again”，还要引导用户去“settings”界面打开权限，这时还要重写onActivityResult判断权限是否被同意。

就是这简单的两步，却夹杂了大量的if()else()语句，不但不优雅，而且每次都要写同样的样板代码，特别繁琐，所以针对这种情况，结合日常开发需要，我使用PermissionHelper封装了权限请求逻辑，在底层通过一个没有界面的Fragment代理权限申请的过程，通过链式调用让开发者一行代码完成权限的申请，简化了权限请求过程，不用每次写重复的代码。

## Feature

- [x] 支持特殊权限的申请，申请时和其他权限的申请步骤一样
- [x] 具有生命周期感应能力，只在界面可见时才发起请求和回调结果
- [x] 系统配置更改(例如屏幕旋转)后能够恢复之前权限申请流程，不会中断权限申请流程
- [x] 灵活性高，可以设置请求、拒绝发生时回调，在回调发生时暂停权限申请流程，然后根据用户意愿再决定是否继续权限申请

## Preview



## Download



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
}
```

## How to use ?

### 1、申请权限



### 2、其他操作



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





