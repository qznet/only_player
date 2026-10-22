package one.only.player.core.data.remote.subtitle

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody

// 取消覆盖响应体读取，超时或离开搜索时立即关闭网络请求。
internal suspend fun <T> Call.awaitBody(transform: (ResponseBody) -> T): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            val result = try {
                response.use {
                    if (!it.isSuccessful) throw IOException("Subtitle request failed with code ${it.code}")
                    transform(it.body)
                }
            } catch (exception: Exception) {
                continuation.resumeWithException(exception)
                return
            }
            continuation.resume(result)
        }
    })
}
