from pydantic import BaseModel, EmailStr

# ----------------AUTH SCHEMAS----------------
class RegisterRequest(BaseModel): # do endpointu register, zeby tam wstrzykiwac email i password
    email: EmailStr
    password: str
  
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
class RouteStartResponse(BaseModel): # do endpointu start route, zeby tam zwracać session_id i route_id
    session_id: str
    route_id: str
 



# Schema for editing route name
class RouteEditNameRequest(BaseModel):
    route_id: str
    name: str

class RouteEndRequest(BaseModel): # do endpointu end route, zeby tam wstrzykiwac session_id
    session_id: str

class Location(BaseModel): # do endpointu z generowaniem narracji, zeby tam wstrzykiwac aktualną lokalizację użytkownika
    lat: float
    lng: float

class TrackPointRequest(BaseModel): # do endpointu track point, zeby tam wstrzykiwac session_id i aktualną lokalizację użytkownika, timestamp i na tej podstawie zapisywać te punkty trasy w redisie zeby potem móc je wykorzystać do obliczania trasy, generowania narracji czy innych rzeczy
    session_id: str
    location: Location
    timestamp: str


# ----------------USER SCHEMAS----------------
class UserPreferencesSchema(BaseModel): # do endpointu z generowaniem narracji, zeby tam wstrzykiwac preferencje użytkownika, na tej podstawie bedziemy generowac narracje
    interests: list[str]     # mozna jakis enum albo cos zeby ograniczyc do konkretnych kategorii, ale na razie niech bedzie string    


# ----------------LLM SCHEMAS----------------
class NarrationRequest(BaseModel): # to zakladam ze bedzie potrzebne do endpointu z generowaniem narracji, zeby tam wstrzykiwac session_id i user preferences zeby na tej podstawie generowac narracje
    session_id: str
    location: Location
    preferences: UserPreferencesSchema







