import hashlib
import hmac
import secrets
from urllib.parse import urlencode

import httpx
from jose import JWTError, jwt

from app.core.config import settings


class KeycloakService:
    @property
    def _base_url(self) -> str:
        return f"{settings.KEYCLOAK_SERVER_URL}/realms/{settings.KEYCLOAK_REALM}"

    @property
    def _internal_base_url(self) -> str:
        return f"{settings.KEYCLOAK_INTERNAL_URL}/realms/{settings.KEYCLOAK_REALM}"

    @property
    def _auth_url(self) -> str:
        return f"{self._base_url}/protocol/openid-connect/auth"

    @property
    def _register_url(self) -> str:
        return f"{self._base_url}/protocol/openid-connect/registrations"

    @property
    def _token_url(self) -> str:
        return f"{self._internal_base_url}/protocol/openid-connect/token"

    @property
    def _jwks_url(self) -> str:
        return f"{self._internal_base_url}/protocol/openid-connect/certs"

    def generate_state(self) -> str:
        nonce = secrets.token_urlsafe(32)
        sig = hmac.new(
            settings.JWT_SECRET_KEY.encode(),
            nonce.encode(),
            hashlib.sha256,
        ).hexdigest()
        return f"{nonce}.{sig}"

    def verify_state(self, state: str) -> bool:
        parts = state.split(".", 1)
        if len(parts) != 2:
            return False
        nonce, sig = parts
        expected = hmac.new(
            settings.JWT_SECRET_KEY.encode(),
            nonce.encode(),
            hashlib.sha256,
        ).hexdigest()
        return hmac.compare_digest(sig, expected)

    def build_register_url(self, state: str) -> str:
        params = {
            "client_id": settings.KEYCLOAK_CLIENT_ID,
            "response_type": "code",
            "scope": "openid email profile",
            "redirect_uri": settings.KEYCLOAK_REDIRECT_URI,
            "state": state,
        }
        return f"{self._register_url}?{urlencode(params)}"

    def build_login_url(self, state: str) -> str:
        params = {
            "client_id": settings.KEYCLOAK_CLIENT_ID,
            "response_type": "code",
            "scope": "openid email profile",
            "redirect_uri": settings.KEYCLOAK_REDIRECT_URI,
            "state": state,
        }
        return f"{self._auth_url}?{urlencode(params)}"

    async def exchange_code(self, code: str) -> dict:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                self._token_url,
                data={
                    "grant_type": "authorization_code",
                    "client_id": settings.KEYCLOAK_CLIENT_ID,
                    "client_secret": settings.KEYCLOAK_CLIENT_SECRET,
                    "code": code,
                    "redirect_uri": settings.KEYCLOAK_REDIRECT_URI,
                },
            )
            if response.status_code != 200:
                raise ValueError(f"Keycloak token exchange failed: {response.text}")
            return response.json()

    async def _fetch_jwks(self) -> dict:
        async with httpx.AsyncClient() as client:
            response = await client.get(self._jwks_url)
            response.raise_for_status()
            return response.json()

    async def verify_id_token(self, id_token: str, access_token: str) -> dict:
        jwks = await self._fetch_jwks()
        try:
            payload = jwt.decode(
                id_token,
                jwks,
                algorithms=["RS256"],
                audience=settings.KEYCLOAK_CLIENT_ID,
                access_token=access_token,
            )
        except JWTError as exc:
            raise ValueError(f"Invalid Keycloak token: {exc}")

        return payload


keycloak_service = KeycloakService()
