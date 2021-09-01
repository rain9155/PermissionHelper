package com.example.permissionhelper

import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView

const val UNDEFINED = "UNDEFINED"

/**
 * 展示权限列表和权限组信息
 */
class PermissionGroupActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "PermissionActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_group)
        supportActionBar?.title = "权限列表"

        val permissionGroups = this.packageManager.getAllPermissionGroups(0)
        val permissions = ArrayList<List<PermissionInfo>>(permissionGroups.size)
        permissionGroups.forEach { permissionGroup ->
            permissions.add(this.packageManager.queryPermissionsByGroup(permissionGroup.name, 0))
        }

        val undefinedPermissionGroup = permissionGroups.find { permissionGroup ->
            permissionGroup.name.contains(UNDEFINED)
        }
        if(undefinedPermissionGroup != null){
            val undefinedPermissions = permissions[permissionGroups.indexOf(undefinedPermissionGroup)]
            permissionGroups.remove(undefinedPermissionGroup)
            permissionGroups.add(0, undefinedPermissionGroup)
            permissions.remove(undefinedPermissions)
            permissions.add(0, undefinedPermissions)
        }


        Log.d(TAG, "permissionGroups = $permissionGroups, permissions = $permissions")

        val elvPermissionGroups = findViewById<ExpandableListView>(R.id.elv_permission_groups)
        elvPermissionGroups.setAdapter(PermissionGroupAdapter(permissionGroups, permissions))
    }

}

/**
 * 权限组列表的适配器
 */
class PermissionGroupAdapter(private val permissionGroups: List<PermissionGroupInfo>, private val permissions: List<List<PermissionInfo>>) : BaseExpandableListAdapter(){

    override fun getGroupCount(): Int {
        return permissionGroups.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return permissions[groupPosition].size
    }

    override fun getGroup(groupPosition: Int): Any {
        return permissionGroups[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return permissions[groupPosition][childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val groupViewGroup: GroupViewHolder
        val groupView: View
        if(convertView == null){
            groupView = LayoutInflater.from(parent!!.context).inflate(R.layout.item_permission_group, null)
            groupViewGroup = GroupViewHolder(groupView.findViewById(R.id.tv_permission_group))
            groupView.tag = groupViewGroup
        }else{
            groupViewGroup = convertView.tag as GroupViewHolder
            groupView = convertView
        }
        val context = parent!!.context
        val permissionGroup = permissionGroups[groupPosition]
        val name = if(permissionGroup.name.contains(UNDEFINED)) "android 10以后系统查询返回的危险权限不再归类到具体权限组，而是全部归类为UNDEFINED" else permissionGroup.name
        val des = "${permissionGroup.loadLabel(context.packageManager)}($name)"
        groupViewGroup.tvPermissionGroup.text = des
        groupViewGroup.tvPermissionGroup.setCompoundDrawables(permissionGroup.loadIcon(context.packageManager), null, null, null)
        return groupView
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val childViewGroup: ChildViewHolder
        val childView: View
        if(convertView == null){
            childView = LayoutInflater.from(parent!!.context).inflate(R.layout.item_permission, null)
            childViewGroup = ChildViewHolder(childView.findViewById(R.id.tv_permission))
            childView.tag = childViewGroup
        }else{
            childViewGroup = convertView.tag as ChildViewHolder
            childView = convertView
        }
        val context = parent!!.context
        val permission = permissions[groupPosition][childPosition]
        val des = "${permission.loadLabel(context.packageManager)}(${permission.name}, ${permission.protectionLevel})"
        childViewGroup.tvPermission.text = des
        childViewGroup.tvPermission.setCompoundDrawables(permission.loadIcon(context.packageManager), null, null, null)
        return childView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    class GroupViewHolder(val tvPermissionGroup: TextView)

    class ChildViewHolder(val tvPermission: TextView)

}