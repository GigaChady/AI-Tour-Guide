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
    AUDIO_DIR: str = "audio_files"
    AUDIO_FILE_TTL_SECONDS: int = 3600
    GOOGLE_APPLICATION_CREDENTIALS: str = ""       # only needed when TTS_PROVIDER=google

    # LLM

    # Route session
    SCRAPE_INTERVAL_SECONDS: int = 120
    # mozna tez pomyslec ze zamiast czasu bedziemy miec odleglosc od ostaniego poi
    # i np jak user nie odejdzie za daleko to nie scrapujemy nowych danych tylko
    # uzywamy tych co mamy ale to juz bedzie bardziej skomplikowane do zrobienia wiec na razie niech zostanie czas

    #Streaming
    STREAM_TIMEOUT_SECONDS: int = 120  #TODO: do ustalenia na pozniej
    AUDIO_TTL_SECONDS: int = 600  # 10 minutes — HLS temp dirs live this long

    # Seed
    SEED_USER_PASSWORD: str = "Testpass1"

    # Testing
    TEST_DATABASE_URL: str

settings = Settings()


DEFAULT_ONBOARDING_CATALOG = [
    {
        "question_key": "gender",
        "title": "Jaka jest Twoja płeć?",
        "type": "single_choice",
        "answers": [
            {"answer_key": "male", "title": "Mężczyzna"},
            {"answer_key": "female", "title": "Kobieta"},
            {"answer_key": "non_binary", "title": "Niebinarny/a"},
        ],
    },
    {
        "question_key": "interests",
        "title": "Jakie masz zainteresowania?",
        "type": "multi_choice",
        "answers": [
            {"answer_key": "architecture", "title": "Architektura", "body": "Historia i styl zabudowy", "trailing_content": "🏛️"},
            {"answer_key": "history", "title": "Historia", "body": "Opowieści o wydarzeniach i miejscach", "trailing_content": "📜"},
            {"answer_key": "culture", "title": "Kultura", "body": "Tradycje, sztuka i lokalny klimat", "trailing_content": "🎭"},
            {"answer_key": "food_and_dining", "title": "Jedzenie", "body": "Smaki, kuchnia i lokalne rekomendacje", "trailing_content": "🍽️"},
            {"answer_key": "nature", "title": "Natura", "body": "Parki, krajobrazy i miejsca na świeżym powietrzu", "trailing_content": "🌿"},
        ],
    },
]
