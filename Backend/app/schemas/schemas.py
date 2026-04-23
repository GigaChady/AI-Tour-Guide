from datetime import datetime
from typing import Any, Literal, Optional
from pydantic import BaseModel, EmailStr, Field



# ----------------WORKER MESSAGE SCHEMA----------------
class WorkerMessage(BaseModel):
    type: str
    data: Optional[Any] = None
    text: Optional[str] = None

# ----------------ERROR RESPONSE SCHEMA----------------
class ErrorResponse(BaseModel):
    detail: str


# ----------------AUTH SCHEMAS----------------
class RegisterRequest(BaseModel): 
    email: EmailStr
    password: str
    name: str

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

class KeycloakLoginResponse(BaseModel):
    login_url: str


# ----------------ROUTE SCHEMAS----------------
class Location(BaseModel):
    lat: float
    lng: float
    ai: bool = False


# ----------------ONBOARDING SCHEMAS----------------
class OnboardingOption(BaseModel):
    key: str
    title: str
    body: str | None = None
    trailing_content: str | None = None


class OnboardingAnswerRequest(BaseModel):
    question_key: str
    answer_key: str | None = None
    answer_keys: list[str] | None = None

class OnboardingQuestion(BaseModel):
    key: str
    title: str
    type: Literal["single_choice", "multi_choice"]
    options: list[OnboardingOption] = []

# ----------------ONBOARDING WRAPPED REQUEST----------------
class OnboardingAnswersRequest(BaseModel):
    items: list[OnboardingAnswerRequest]
    detail: str | None = None

class OnboardingSelectedAnswers(BaseModel):
    gender: str | None = None
    interests: list[str] = []

class OnboardingQuestionsResponse(BaseModel):
    items: list[OnboardingQuestion]
    selected_answers: OnboardingSelectedAnswers



# ----------------NARRATION SETTINGS SCHEMAS----------------
class NarrationSettingsRequest(BaseModel):
    language: str
    pitch: int = Field(..., ge=0, le=100)
    speed: int = Field(..., ge=0, le=10)
    volume: int = Field(..., ge=0, le=100)
    detail_level: str
    auto_play: bool



# ----------------USER SETTINGS SCHEMAS----------------
class UpdateUserParamsRequest(BaseModel):
    name: str | None = None
    new_email: EmailStr | None = None
    new_password: str | None = None

# ----------------SESSION SCHEMAS----------------
class SessionMeta(BaseModel):
    user_id: str
    route_id: str


# ----------------USER PARAMS RESPONSE SCHEMA----------------
class UserParamsResponse(BaseModel):
    email: str
    name: str

# ----------------PREFERENCES CACHE SCHEMA----------------
class UserPreferencesCache(BaseModel):
    interests: list[str]


# ----------------ROUTE STATS SCHEMAS----------------
class RoutePoiResponse(BaseModel):
    id: str
    poi_id: str | None
    name: str
    lat: float
    lng: float
    description: str | None

class RouteStatsResponse(BaseModel):
    distance_m: float
    duration_s: int
    started_at: datetime
    ended_at: datetime | None
    pois: list[RoutePoiResponse]


# ----------------DASHBOARD SCHEMAS----------------
class DashboardJobResponse(BaseModel):
    session_id: str


# ----------------POI DATA SCHEMA----------------
class PoiData(BaseModel):
    name: str
    photos: list[str]
    desc: str | None = None
    lat: float
    lng: float


# ----------------WEBSOCKET MESSAGE SCHEMAS----------------
class WsReadyMessage(BaseModel):
    type: Literal["ready"]
    route_id: str
    session_id: str

class WsReconnectedMessage(BaseModel):
    type: Literal["reconnected"]
    route_id: str
    session_id: str

class NarrationMessage(BaseModel):
    type: Literal["narration"]
    text: str

class PoisMessage(BaseModel):
    type: Literal["pois"]
    data: list[PoiData]

# ----------------WS CONNECT SCHEMA----------------
class WsConnectRequest(BaseModel):
    token: str
    session_id: str | None = None

# ----------------NARRATION SCHEMAS----------------
class NarrationTranscriptChunk(BaseModel):
    chunk_id: int
    text: str

class NarrationTranscript(BaseModel):
    type: Literal["narration_transcript"]
    transcript: list[NarrationTranscriptChunk]

class NarrationChunk(BaseModel):
    type: Literal["narration_chunk"]
    chunk_id: int
    audio: str
    words: list

class NarrationDone(BaseModel):
    type: Literal["narration_done"]


# ----------------REDIS LOCATION EVENT SCHEMA----------------
class LocationEvent(BaseModel):
    session_id: str
    lat: float
    lng: float
    include_photos: int | None = None


