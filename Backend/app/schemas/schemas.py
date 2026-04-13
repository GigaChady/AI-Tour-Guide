from typing import Literal

from pydantic import BaseModel, EmailStr


# ----------------AUTH SCHEMAS----------------
class RegisterRequest(BaseModel): # do endpointu register, zeby tam wstrzykiwac email i password
    email: EmailStr
    password: str
    name: str
    imie: str | None = None
    nazwisko: str | None = None

class LoginRequest(BaseModel): # do endpointu login, zeby tam wstrzykiwac email i password
    email: EmailStr
    password: str

class LogoutRequest(BaseModel): # do endpointu logout, zeby tam wstrzykiwac refresh token do unieważnienia
    refresh_token: str

class GoogleAuthRequest(BaseModel): # do endpointu Google Auth, zeby tam wstrzykiwac token Google
    google_token: str

class RefreshRequest(BaseModel): # do endpointu refresh, zeby tam wstrzykiwac refresh token do odświeżenia access tokena
    refresh_token: str

class TokenResponse(BaseModel): # do endpointów auth, zeby tam zwracać access token i refresh token
    access_token: str
    refresh_token: str


# ----------------ROUTE SCHEMAS----------------
class RouteEditNameRequest(BaseModel):
    route_id: str
    name: str

class Location(BaseModel):
    lat: float
    lng: float


# ----------------ONBOARDING SCHEMAS----------------
class OnboardingOption(BaseModel):
    key: str
    title: str
    body: str | None = None
    trailing_content: str | None = None


class OnboardingQuestion(BaseModel):
    key: str
    title: str
    type: Literal["single_choice", "multi_choice"]
    options: list[OnboardingOption] = []


class OnboardingAnswerRequest(BaseModel):
    question_key: str
    answer_key: str | None = None
    answer_keys: list[str] | None = None


class OnboardingAnswerResponse(BaseModel):
    question_key: str
    answer_key: str | None = None
    answer_keys: list[str] | None = None


# ----------------NARRATION SETTINGS SCHEMAS----------------
class NarrationSettingOption(BaseModel):
    key: str
    title: str
    body: str | None = None


class NarrationSetting(BaseModel):
    key: str
    title: str
    type: Literal["percentage", "single_choice"]
    min: int | None = None
    max: int | None = None
    required: bool = False
    options: list[NarrationSettingOption] = []
