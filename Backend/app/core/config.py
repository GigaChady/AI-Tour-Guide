from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")
    # JWT
    JWT_SECRET_KEY: str 
    JWT_ALGORITHM: str
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    MAX_REFRESH_TOKENS_PER_USER: int = 1

#     #Google
    GOOGLE_CLIENT_ID: str
#     GOOGLE_CLIENT_SECRET: str
#     GOOGLE_REDIRECT_URI: str
#     GOOGLE_MAP_KEY: str
#     # GOOGLE_MAPS_API_URL: str = "https://maps.googleapis.com/maps/api"

#    # PostgreSQL (asyncpg)
#     POSTGRES_HOST: str
#     POSTGRES_PORT: int = 5432
#     POSTGRES_DB: str
#     POSTGRES_USER: str
#     POSTGRES_PASSWORD: str
    DATABASE_URL: str
#     #Maybe add sync version for celery tasks
#     # DATABASE_URL_SYNC: str

#     # Redis
    # REDIS_HOST: str = "localhost"
    # REDIS_PORT: int = 6379
#     # REDIS_DB: int 
    # REDIS_PASSWORD: str 
    REDIS_URL: str

    # TTS
    TTS_PROVIDER: str = "edge"                    # "edge" or "google"
    TTS_SPEAKER_WAV: str = ""                      # reserved for future use
    AUDIO_FILE_TTL_SECONDS: int = 3600
    GOOGLE_APPLICATION_CREDENTIALS: str = ""       # only needed when TTS_PROVIDER=google

    # LLM

    # Route session
    SCRAPE_INTERVAL_SECONDS: int = 120   # co ile sekund scraper uderza w Google 
    # mozna tez pomyslec ze zamiast czasu bedziemy miec odleglosc od ostaniego poi 
    # i np jak user nie odejdzie za daleko to nie scrapujemy nowych danych tylko 
    # uzywamy tych co mamy ale to juz bedzie bardziej skomplikowane do zrobienia wiec na razie niech zostanie czas

    #Streaming 
    STREAM_TIMEOUT_SECONDS: int = 15 # jakis timeout dla streamingu zeby w razie cos sie nie zawiesil na amen, mozna tez pomyslec o jakims keep-alive zeby utrzymywac polaczenie ale to juz bedzie bardziej skomplikowane do zrobienia wiec na razie niech zostanie timeout

    # Demographics
    DEMOGRAPHICS_GENDER_OPTIONS: str = "male,female,non_binary,prefer_not_to_say"
    DEMOGRAPHICS_MIN_AGE: int = 10
    DEMOGRAPHICS_MAX_AGE: int = 120
    DEMOGRAPHICS_CUSTOM_GENDER_ANSWER_ID: str = "custom"

settings = Settings()


DEFAULT_PREFERENCE_CATALOG = [
    {
        "question_key": "pitch",
        "title": "Pitch",
        "type": "percentage",
        "sort_order": 1,
        "min_value": 0,
        "max_value": 100,
        "required": True,
        "answers": [],
    },
    {
        "question_key": "speed",
        "title": "Speed",
        "type": "percentage",
        "sort_order": 2,
        "min_value": 0,
        "max_value": 100,
        "required": True,
        "answers": [],
    },
    {
        "question_key": "loudness",
        "title": "Loudness",
        "type": "percentage",
        "sort_order": 3,
        "min_value": 0,
        "max_value": 100,
        "required": True,
        "answers": [],
    },
    {
        "question_key": "language",
        "title": "Language",
        "type": "single_choice",
        "sort_order": 4,
        "answers": [
            {"answer_key": "en", "title": "English", "body": "Narracja po angielsku", "trailing_content": None, "sort_order": 1},
            {"answer_key": "pl", "title": "Polish", "body": "Narracja po polsku", "trailing_content": None, "sort_order": 2},
            {"answer_key": "es", "title": "Spanish", "body": "Narracja po hiszpańsku", "trailing_content": None, "sort_order": 3},
            {"answer_key": "fr", "title": "French", "body": "Narracja po francusku", "trailing_content": None, "sort_order": 4},
            {"answer_key": "de", "title": "German", "body": "Narracja po niemiecku", "trailing_content": None, "sort_order": 5},
            {"answer_key": "it", "title": "Italian", "body": "Narracja po włosku", "trailing_content": None, "sort_order": 6},
            {"answer_key": "pt", "title": "Portuguese", "body": "Narracja po portugalsku", "trailing_content": None, "sort_order": 7},
            {"answer_key": "ru", "title": "Russian", "body": "Narracja po rosyjsku", "trailing_content": None, "sort_order": 8},
            {"answer_key": "uk", "title": "Ukrainian", "body": "Narracja po ukraińsku", "trailing_content": None, "sort_order": 9},
            {"answer_key": "ja", "title": "Japanese", "body": "Narracja po japońsku", "trailing_content": None, "sort_order": 10},
        ],
    },
    {
        "question_key": "interests",
        "title": "Interests",
        "type": "multi_choice",
        "sort_order": 5,
        "answers": [
            {"answer_key": "architecture", "title": "Architecture", "body": "Historia i styl zabudowy", "trailing_content": "🏛️", "sort_order": 1},
            {"answer_key": "history", "title": "History", "body": "Opowieści o wydarzeniach i miejscach", "trailing_content": "📜", "sort_order": 2},
            {"answer_key": "culture", "title": "Culture", "body": "Tradycje, sztuka i lokalny klimat", "trailing_content": "🎭", "sort_order": 3},
            {"answer_key": "food_and_dining", "title": "Food and dining", "body": "Smaki, kuchnia i lokalne rekomendacje", "trailing_content": "🍽️", "sort_order": 4},
            {"answer_key": "nature", "title": "Nature", "body": "Parki, krajobrazy i miejsca na świeżym powietrzu", "trailing_content": "🌿", "sort_order": 5},
        ],
    },
]
    

