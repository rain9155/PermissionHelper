package com.example.permission.request

import android.util.ArrayMap
import com.example.permission.base.IRequestManager
import com.example.permission.base.Request
import com.example.permission.utils.LogUtil
import java.security.MessageDigest

/**
 * [IRequestManager]的默认实现
 * Created by 陈健宇 at 2021/9/12
 */
internal class DefaultRequestManager private constructor() : IRequestManager {

    companion object{
        private const val TAG = "RequestManager"

        fun create() = DefaultRequestManager()
    }

    private val requests = ArrayMap<String, Request>()

    override fun startRequest(request: Request) {
        val fragmentProvider = request.fragmentProvider
        val requestPermissions = request.getClonedRequestPermissions().sorted()
        val key = generateKey(fragmentProvider.stableIdentify(), requestPermissions)
        LogUtil.d(TAG, "startRequest: key = $key")
        if(requests[key] != null){
            val preRequest = requests[key]
            preRequest!!.run {
                requestCallback = request.requestCallback
                rejectedCallback = request.rejectedCallback
                rejectedForeverCallback = request.rejectedForeverCallback
                resultCallback = request.resultCallback
            }
            LogUtil.d(TAG, "startRequest: request already exist")
        }else{
            requests[key] = request
            val interceptors = listOf(
                PreRequestNode().apply { request.getRequestStepCallbackManager().add(this) },
                RequestNormalNode(),
                RequestSpecialNode(),
                PostRequestNode().apply { request.getRequestStepCallbackManager().add(this) },
                FinishRequestNode()
            )
            fragmentProvider.run {
                request.getRequestStepCallbackManager().add(this)
            }
            request.run {
                requestKey = key
                getRequestStepCallbackManager().add(this)
            }
            DefaultChain(request, interceptors).process(request)
            LogUtil.d(TAG, "startRequest: start process request")
        }
    }

    override fun finishRequest(request: Request) {
        val key = request.requestKey
        val removed = requests.remove(key)
        LogUtil.d(TAG, "finishRequest: key = $key, isRemoved = ${removed != null}")
    }

    override fun clearRequests() {
        LogUtil.d(TAG, "clearRequests")
        requests.clear()
    }

    private fun generateKey(pageIdentify: String, permissions: List<String>): String {
        //md5, 没转16进制之前是16字节
        val messageDigest = MessageDigest.getInstance("MD5")
        val key = messageDigest.digest(pageIdentify.toByteArray())
        val subKey = messageDigest.digest(permissions.toString().toByteArray())
        //转成16进制后是32字节
        return "${toHex(key)}#${toHex(subKey)}"
    }

    private fun toHex(byteArray: ByteArray): String {
        return with(StringBuilder()) {
            byteArray.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) {
                    append("0").append(hexStr)
                } else {
                    append(hexStr)
                }
            }
            toString()
        }
    }
}