package com.example.permission.request

import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.Request
import java.lang.IndexOutOfBoundsException

/**
 * [IChain]的默认实现
 * Created by 陈健宇 at 2021/8/15
 */
internal class DefaultChain constructor(
    private val req: Request,
    private val nodes: List<INode>,
    private val index: Int = 0
) : IChain {

    override fun getRequest(): Request {
        return req
    }

    override fun process(request: Request, finish: Boolean, restart: Boolean, again: Boolean) {
        if(index < 0 || index >= nodes.size){
            throw IndexOutOfBoundsException("Node index out of bounds: index = $index, size = ${nodes.size}")
        }
        val finalIndex = when {
            finish -> {
                nodes.size - 1
            }
            restart -> {
                0
            }
            again && index > 0 -> {
                index - 1
            }else -> {
                index
            }
        }
        request.isRestart = restart
        val node = nodes[finalIndex]
        node.handle(DefaultChain(
            request,
            nodes,
            index = finalIndex + 1
        ))
    }

}