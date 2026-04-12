package ai.tour.guide.config

import io.ktor.http.URLProtocol

object AppConfig {
    const val SIGN_IN_WITH_GOOGLE_CLIENT_ID =
        "934881955828-0221kagjsgdefr5ttkp4vh2dv7unrlrg.apps.googleusercontent.com"
    val HTTPS_CLIENT_PROTOCOL: URLProtocol = URLProtocol.HTTP
    const val HTTPS_CLIENT_HOST: String = "localhost:8000"
}