package com.ytwrap.app.playback

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OkHttp-backed Downloader for NewPipe Extractor.
 * Handles all HTTP requests that the extractor needs to make to YouTube.
 */
class DownloaderImpl private constructor() : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private var instance: DownloaderImpl? = null

        fun getInstance(): DownloaderImpl {
            if (instance == null) {
                instance = DownloaderImpl()
            }
            return instance!!
        }
    }

    override fun execute(request: Request): Response {
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .method(
                request.httpMethod(),
                dataToSend?.toRequestBody()
            )

        // Add headers
        for ((headerName, headerValueList) in headers) {
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                for (headerValue in headerValueList) {
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        val responseBody = response.body?.string()
        val latestUrl = response.request.url.toString()

        val responseHeaders: MutableMap<String, List<String>> = mutableMapOf()
        for (headerName in response.headers.names()) {
            responseHeaders[headerName] = response.headers.values(headerName)
        }

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            latestUrl
        )
    }
}
