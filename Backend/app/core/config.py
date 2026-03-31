from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # JWT
    JWT_SECRET_KEY: str 
    JWT_ALGORITHM: str
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 30

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

    # LLM

    # Route session
    SCRAPE_INTERVAL_SECONDS: int = 120   # co ile sekund scraper uderza w Google 
    # mozna tez pomyslec ze zamiast czasu bedziemy miec odleglosc od ostaniego poi 
    # i np jak user nie odejdzie za daleko to nie scrapujemy nowych danych tylko 
    # uzywamy tych co mamy ale to juz bedzie bardziej skomplikowane do zrobienia wiec na razie niech zostanie czas

    #Streaming 
    STREAM_TIMEOUT_SECONDS: int = 15 # jakis timeout dla streamingu zeby w razie cos sie nie zawiesil na amen, mozna tez pomyslec o jakims keep-alive zeby utrzymywac polaczenie ale to juz bedzie bardziej skomplikowane do zrobienia wiec na razie niech zostanie timeout

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

settings = Settings()
    

