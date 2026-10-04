package io.github.wtfjb.aximo.ai

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** A fake server: records requests and answers with [handler]. */
class MockServer(
    timeouts: AiTimeouts = AiTimeouts(),
    private val handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    val requests = mutableListOf<HttpRequestData>()

    val client = AiHttp.client(
        MockEngine { request ->
            requests += request
            handler(request)
        },
        timeouts,
    )

    val lastRequest: HttpRequestData get() = requests.last()
}

fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

/** The JSON body that was sent. */
fun HttpRequestData.jsonBody(): JsonObject {
    val content = body as OutgoingContent
    val text = (content as TextContent).text
    return Json.parseToJsonElement(text).jsonObject
}
