package com.example.permission.utils;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.example.permission.bean.Permission;
import java.util.ArrayList;
import java.util.List;

/**
 * 公共方法工具类
 * Created by 陈健宇 at 2019/6/3
 */
public class CommonUtil {

    /**
     * 把Permission数组转为List集合
     * @param permissions Permission数组
     * @return PermissionList集合
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static List<Permission> toList(String[] permissions){
        List<Permission> allGrantedPermissions = new ArrayList<>(permissions.length);
        for (int i = 0; i < permissions.length; i++){
            String name = permissions[i];
            Permission permission = new Permission(name, true);
            allGrantedPermissions.add(permission);
        }
        return allGrantedPermissions;
    }

    /**
     * 把Permission集合转为数组
     * @param permissions Permission集合
     * @return Permission数组
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String[] toArray(List<Permission> permissions){
        String[] result = new String[permissions.size()];
        for (int i = 0; i < permissions.size(); i++){
            String name = permissions.get(i).name;
            result[i]= name;
        }
        return result;
    }

}
