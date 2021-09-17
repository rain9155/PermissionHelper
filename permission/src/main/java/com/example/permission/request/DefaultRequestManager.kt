package com.example.permission.request

import android.util.ArrayMap
import android.util.Pair
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
        private const val SPLITERATOR = "###"

        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DefaultRequestManager()
        }
    }

    private val requests = ArrayMap<String, Request>()

    override fun startRequest(request: IRequestManager.Request) {
        val requestKey = generateRequestKey(
            request.fragmentProvider.get().requestPageIdentify(),
            request.requestPermissions.sorted()
        )
        LogUtil.d(TAG, "startRequest: requestKey = $requestKey")
        val preRequest = requests[requestKey]
        if(preRequest != null && !preRequest.isInterrupt && !preRequest.isFinish){
            preRequest.run {
                requestCallback = request.requestCallback
                rejectedCallback = request.rejectedCallback
                rejectedForeverCallback = request.rejectedForeverCallback
                resultCallback = request.resultCallback
                reCallbackAfterConfigurationChanged = request.reCallbackAfterConfigurationChanged
            }
            LogUtil.d(TAG, "startRequest: request already exist")
        }else{
            val originalRequest = Request(
                request.fragmentProvider,
                requestKey,
                request.reCallbackAfterConfigurationChanged,
                request.requestCallback,
                request.rejectedCallback,
                request.rejectedForeverCallback,
                request.resultCallback,
                request.requestPermissions.toMutableList()
            ).apply {
                getRequestStepCallbackManager().add(this)
                getRequestStepCallbackManager().add(fragmentProvider)
            }
            val interceptors = listOf(
                PreRequestNode().apply { originalRequest.getRequestStepCallbackManager().add(this) },
                RequestNormalNode(),
                RequestSpecialNode(),
                PostRequestNode().apply { originalRequest.getRequestStepCallbackManager().add(this) },
                FinishRequestNode()
            )
            requests[requestKey] = originalRequest
            LogUtil.d(TAG, "startRequest: start process request")
            DefaultChain(originalRequest, interceptors).process(originalRequest)
        }
    }

    override fun finishRequest(requestKey: String) {
        val removed = requests.remove(requestKey)
        LogUtil.d(TAG, "finishRequest: requestKey = $requestKey, isRemoved = ${removed != null}")
    }

    override fun clearPageRequests(pageIdentity: String) {
        LogUtil.d(TAG, "clearPageRequests: pageIdentity = $pageIdentity")
        val mainKey = generateMainKey(pageIdentity)
        requests.filterKeys { key ->
            key.contains(mainKey)
        }.keys.forEach { key ->
            val isRemoved = requests.remove(key) != null
            LogUtil.d(TAG, "clearPageRequests: key = $key, mainKey = $mainKey, isRemoved = $isRemoved")
        }
    }

    private fun generateRequestKey(pageIdentify: String, permissions: List<String>): String {
        return "${generateMainKey(pageIdentify)}${SPLITERATOR}${generateSubKey(permissions)}"
    }

    private fun generateMainKey(pageIdentify: String): String {
        val mainKey = MessageDigest.getInstance("MD5").digest(pageIdentify.toByteArray())
        return toHex(mainKey)
    }

    private fun generateSubKey(permissions: List<String>): String {
        val subKey = MessageDigest.getInstance("MD5").digest(permissions.toString().toByteArray())
        return toHex(subKey)
    }

    private fun splitRequestKey(requestKey: String): Pair<String, String> {
        val splits = requestKey.split(SPLITERATOR)
        if(splits.size == 2) {
            return Pair.create(splits[0], splits[1])
        }
        return Pair.create("", "")
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