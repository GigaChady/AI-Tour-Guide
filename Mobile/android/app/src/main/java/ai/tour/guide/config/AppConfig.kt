package ai.tour.guide.config

import io.ktor.http.URLProtocol

object AppConfig {
    const val SIGN_IN_WITH_GOOGLE_CLIENT_ID =
        "231024205055-e5lu5ajo32c3hkfqb9rrnu442rkd2rcj.apps.googleusercontent.com"
    val HTTPS_CLIENT_PROTOCOL: URLProtocol = URLProtocol.HTTP
}
