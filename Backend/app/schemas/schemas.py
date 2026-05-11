from datetime import datetime
from typing import Any, Literal, Optional
from pydantic import BaseModel, EmailStr, Field



# ----------------WORKER MESSAGE SCHEMA----------------
class WorkerMessage(BaseModel):
    type: str
    data: Optional[Any] = None
    text: Optional[str] = None
    narration_id: Optional[str] = None

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
    # volume: int = Field(..., ge=0, le=100)
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


class RouteMapResponse(BaseModel):
    geojson: dict | None

class RouteStatsResponse(BaseModel):
    distance_m: float
    duration_s: int
    started_at: datetime
    ended_at: datetime | None
    pois: list[RoutePoiResponse]


# ----------------WEB HISTORY SCHEMAS----------------
class WebDashboardStats(BaseModel):
    total_countries: int
    total_cities: int
    total_distance_km: float
    total_duration_minutes: int

class WebDashboardExpedition(BaseModel):
    id: str
    city: str | None
    date: datetime
    route_url: str | None

class WebDashboardResponse(BaseModel):
    stats: WebDashboardStats
    recent_expeditions: list[WebDashboardExpedition]


class RouteHistoryUser(BaseModel):
    name: str
    avatar_url: str | None
    total_explorations: int
    total_distance_km: float
    total_duration_minutes: int

class RouteHistoryItem(BaseModel):
    id: str
    name: str | None
    date: datetime
    distance_km: float | None
    duration_minutes: int | None
    route_url: str | None

class RouteHistoryResponse(BaseModel):
    user: RouteHistoryUser
    routes: list[RouteHistoryItem]


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
class WsPreviewReadyMessage(BaseModel):
    type: Literal["session_start"] = "session_start"
    session_id: str

class WsTourMessage(BaseModel):
    type: Literal["tour_start", "tour_reconnect"]
    route_id: str
    session_id: str

class NarrationMessage(BaseModel):
    type: Literal["narration"]
    text: str

class PoisMessage(BaseModel):
    type: Literal["pois"]
    narration_id: Optional[str] = None
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
    narration_id: str
    transcript: list[NarrationTranscriptChunk]


class NarrationWords(BaseModel):
    type: Literal["narration_words"]
    narration_id: str
    chunk_id: int
    words: list

class NarrationDone(BaseModel):
    type: Literal["narration_done"]
    narration_id: str


# ----------------REDIS LOCATION EVENT SCHEMA----------------
class LocationEvent(BaseModel):
    session_id: str
    lat: float
    lng: float
    include_photos: int | None = None
    is_narration: bool | None = None


class RoutePoints(BaseModel):
    route_id: str
    points: list[Location]

