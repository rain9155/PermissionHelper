# PermissionHelper 1.x (deprecated)

### 1.x版本已经废弃，不再维护，请使用[2.x](/README.md)版本

### 简化android6.0动态权限申请过程，一行代码搞定权限申请，可以一次申请单个或多个权限，支持特殊权限的申请，欢迎大家star、fork，如有问题请[issue](https://github.com/rain9155/PermissionHelper/issues)

## Pre

在原来申请一个权限需要2步，第一步：第一步在需要申请权限的地方检查该权限是否被同意，如果同意了就直接执行，如果不同意就动态申请权限；第二步：重写Activity或Fragment的onRequestPermissionsResult方法，在里面根据grantResults数组判断权限是否被同意，如果同意就直接执行，如果不同意就要进行相应的提示，如果用户勾选了“don't ask again”，还要引导用户去“settings”界面打开权限，这时还要重写onActivityResult判断权限是否被同意。

就是这简单的两步，却夹杂了大量的if()else()语句，不但不优雅，而且每次都要写同样的样板代码，特别烦，所以我就封装了权限请求逻辑，支持链式调用，使得权限请求过程简化了许多，不用每次写重复的代码，它的原理就是通过一个没有界面的Fragment代理权限申请的过程，然后把权限结果回调给我们。

## Preview

![s1](/screenshots/s1.gif)
![s2](/screenshots/s2.gif)

## Download

下载查看示例。<br>
![qr](/screenshots/qr.png)

## How to install?

在项目的根目录的build.gradle中引入仓库：

```groovy
allprojects {
    repositories {
        mavenCentral()
    }
}
```

然后在项目的app目录下的build.gradle中引入，如下：

```java
dependencies {
    implementation 'io.github.rain9155:permissionhelper:1.0.2'
}
```

## How to use?

里面的方法都有详细注释。

### 1、检查单个权限

```java
PermissionHelper.getInstance().with(this).checkPermission(Manifest.permission.CALL_PHONE))
```

### 2、检查多个权限

```java
mPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_CONTACTS, Manifest.permission.CAMERA};

PermissionHelper.getInstance().with(this).checkPermissions(mPermissions);
```

### 3、检查特殊权限

SpecialPermission是一个枚举，里面列举了3个特殊权限，以后发现更多再添加上去。

```java
PermissionHelper.getInstance().with(this).checkSpecialPermission(SpecialPermission.WRITE_SYSTEM_SETTINGS)
```

### 4、申请单个普通权限

对应[IPermissionCallback](https://github.com/rain9155/PermissionHelper/blob/master/permission/src/main/java/com/example/permission/callback/IPermissionCallback.java)接口。

```java
PermissionHelper.getInstance().with(this).requestPermission(
    Manifest.permission.CALL_PHONE,
    new IPermissionCallback() {
	@Override
	public void onAccepted(Permission permission) {
	    //...
	}

	@Override
	public void onDenied(Permission permission) {
	    //...
	}

	//这里可以根据业务逻辑选择重写onDeniedAndReject（）,这里我重写了
	@Override
	public void onDeniedAndReject(Permission permission) {
	  //...
	}
    }
);
```

回调方法解释：

```java
/**
* 权限同意后回调，有三种情况：
* （1）用户点击授权了这个权限
* （2）之前已经同意了无需再授权此权限
* （3）系统版本小于M
* @param permission 封装了信息的权限
*/
void onAccepted(Permission permission);

/**
* 权限拒绝后回调，只有一种情况：
* （1）用户点击拒绝授权了这个权限
* @param permission 封装了信息的权限
*/
void onDenied(Permission permission);

/**
* 权限拒绝后回调，你可以选择重写它并在里面处理逻辑，如引导用户到权限申请页同意这个权限
* 只有一种情况：
* （1）没用户点击拒绝授权了这个权限，并勾选了don’t ask again
* @param permission 封装了信息的权限
*/
default void onDeniedAndReject(Permission permission){}
```

### 5、申请多个普通权限

对应[IPermissionsCallback](https://github.com/rain9155/PermissionHelper/blob/master/permission/src/main/java/com/example/permission/callback/IPermissionsCallback.java)接口。

```java
PermissionHelper.getInstance().with(this).requestPermissions(
    mPermissions,
    new IPermissionsCallback() {
	@Override
	public void onAccepted(List<Permission> permissions) {
	  //...
	}

	@Override
	public void onDenied(List<Permission> permissions) {
	  //...
	}

	//这里可以根据业务逻辑选择重写onDeniedAndReject（），这里我没有重写
    }
```
回调方法解释：

```java
/**
* 权限同意的回调，有三种情况：
*（1）用户点击授权了一个或多个权限
*（2）之前已经同意了无需再授权此权限
*（3）系统版本小于M
* @param permissions 用户同意授权的权限列表
*/
void onAccepted(List<Permission> permissions);

/**
* 权限拒绝的回调，可以引导用户到权限申请页同意一个或多个权限，只有一种情况：
* （1）用户点击拒绝授权一个或多个权限
* @param permissions 用户拒绝授权的权限列表
*/
void onDenied(List<Permission> permissions);

/**
* 权限拒绝的回调，你可以选择重写它并在里面处理逻辑，如引导用户到权限申请页同意一个或多个权限
* 只有一种情况：
* （1）没用户点击拒绝授权一个或多个权限，并勾选了don’t ask again
* @param permissionsDenied 用户拒绝授权的权限列表
* @param permissionsReject 用户拒绝授权并勾选了don’t ask again的权限列表
*/
default void onDeniedAndReject(List<Permission> permissionsDenied, List<Permission> permissionsReject){}

```

### 6、申请特殊普通权限

对应[ISpecialPermissionCallback](https://github.com/rain9155/PermissionHelper/blob/master/permission/src/main/java/com/example/permission/callback/ISpecialPermissionCallback.java)接口。

```java
PermissionHelper.getInstance().with(this).requestSpecialPermission(
    SpecialPermission.WRITE_SYSTEM_SETTINGS,
    new ISpecialPermissionCallback() {
	@Override
	public void onAccepted(SpecialPermission permission) {
	   //...
	}

	@Override
	public void onDenied(SpecialPermission permission) {
	   //...
	}
    }
);
```

回调方法解释：

```java
/**
* 用户同意该特殊权限
* @param permission 特殊权限
*/
void onAccepted(SpecialPermission permission);

/**
* 用户拒绝该特殊权限
* @param permission 特殊权限
*/
void onDenied(SpecialPermission permission);
```

### 其他操作

上面那6步就是基本操作了，如果你不想每次都写那个with(this)方法，可以在BaseActivity中这样写:

```java
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_special);
        //统一初始化，后面不用使用with(this)
        PermissionHelper.getInstance().init(this);
    }
}
```

跳转到应用详情页或权限详情页。

```java
//这个直接跳转到应用详情页
PermissionHelper.getInstance().with(this).gotoAppDetail();
或
//如果你的手机是华为、小米或魅族，会优先跳转到权限详情页
PermissionHelper.getInstance().with(this).gotoPermissionDetail();
```

更多细节查看[Demo](https://github.com/rain9155/PermissionHelper/tree/master/app/src/main/java/com/example/permissionhelper)，代码是最好的老师，如果对哪个回调方法不知道什么意思，可以查看该方法的注释,每个方法都有详细的注释。

## License

```java
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

