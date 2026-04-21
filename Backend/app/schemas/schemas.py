from typing import Any, Literal
from pydantic import BaseModel, EmailStr, Field


# ----------------AUTH SCHEMAS----------------
class RegisterRequest(BaseModel): 
    email: EmailStr
    password: str
    name: str
    imie: str | None = None
    nazwisko: str | None = None

class LoginRequest(BaseModel): 
    email: EmailStr
    password: str

class LogoutRequest(BaseModel): 
    refresh_token: str

class GoogleAuthRequest(BaseModel): 
    google_token: str

class RefreshRequest(BaseModel): 
    refresh_token: str

class TokenResponse(BaseModel): 
    access_token: str
    refresh_token: str


# ----------------ROUTE SCHEMAS----------------
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
class NarrationSettingsRequest(BaseModel):
    language: str = Field(...)
    pitch: int = Field(..., ge=0, le=100)
    speed: int = Field(..., ge=0, le=10)
    volume: int = Field(..., ge=0, le=100)
    detail_level: str = Field(...)
    auto_play: bool = Field(...)



# ----------------USER SETTINGS SCHEMAS----------------
class ChangeNameRequest(BaseModel):
    name: str

class ChangePasswordRequest(BaseModel):
    new_password: str

class ChangeEmailRequest(BaseModel):
    new_email: EmailStr

# ----------------USER PARAMS RESPONSE SCHEMA----------------
class UserParamsResponse(BaseModel):
    email: str
    name: str

# ----------------PREFERENCES CACHE SCHEMA----------------
class UserPreferencesCache(BaseModel):
    answer_keys: list[str]


# ----------------DASHBOARD SCHEMAS----------------
class DashboardJobResponse(BaseModel):
    session_id: str

class DashboardPOIResponse(BaseModel):
    poi: Any
    # photos_base64: list[str] = []  # for base64


# ----------------BASE REQUEST WRAPPER----------------
class ItemsRequest(BaseModel):
    items: list[Any]
    detail: str | None = None

# ----------------ONBOARDING WRAPPED REQUEST----------------
class OnboardingAnswersRequest(BaseModel):
    items: list[OnboardingAnswerRequest]
    detail: str | None = None