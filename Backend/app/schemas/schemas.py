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
    token_type: str = "Bearer"




# ----------------ROUTE SCHEMAS----------------
class RouteEditNameRequest(BaseModel):
    route_id: str
    name: str

class Location(BaseModel):
    lat: float
    lng: float


# ----------------USER SCHEMAS----------------
class UserPreferencesSchema(BaseModel):
    interests: list[dict[str, object]]


class PreferenceQuestionAnswerOption(BaseModel):
    answer_id: str
    title: str
    body: str | None = None
    trailingContent: str | None = None


class PreferenceQuestionInput(BaseModel):
    min: int | None = None
    max: int | None = None
    required: bool = False


class PreferenceQuestion(BaseModel):
    question_id: int
    question_key: str
    title: str
    type: Literal["percentage", "single_choice", "multi_choice"]
    answers: list[PreferenceQuestionAnswerOption] = []
    input: PreferenceQuestionInput | None = None


class PreferenceAnswerRequest(BaseModel):
    question_id: int
    answer_id: str | None = None
    answer_ids: list[str] | None = None
    value: int | None = None


class PreferenceAnswerResponse(BaseModel):
    question_id: int
    answer_id: str | None = None
    answer_ids: list[str] | None = None
    value: int | None = None


class DemographicsQuestionAnswerOption(BaseModel):
    answer_id: str
    title: str
    body: str | None = None


class DemographicsQuestionInput(BaseModel):
    value_key: str
    min: int | None = None
    max: int | None = None
    required: bool = False


class DemographicsQuestion(BaseModel):
    question_key: str
    title: str
    type: Literal["single_choice", "number_input"]
    answers: list[DemographicsQuestionAnswerOption] = []
    allow_custom_text: bool = False
    custom_text_key: str | None = None
    input: DemographicsQuestionInput | None = None


class DemographicsAnswerRequest(BaseModel):
    question_key: str
    answer_id: str | None = None
    custom_text: str | None = None
    value: int | None = None










