import uuid
import json

class SessionService:
    def __init__(self, redis_client):
        self.redis = redis_client

    #create session
    async def create(self, user_id: str, route_id: str) -> str:
        session_id = str(uuid.uuid4())
        await self.redis.set(
            f"session:{session_id}:meta",
            json.dumps({ # mozna cos wiecje dodać do tego meta jak bedziemy potrzebowali, np. timestamp startu sesji czy cos
                "user_id": user_id,
                "route_id": route_id
            })
        )
        return session_id

    #end session 
    async def end_session(self, session_id: str):
        await self.redis.delete( # tak mysle ze taki sposob bedzie najlepszy ze mamy pois w kontekscie generowania nowych dialogow no i visited dla filtra bo dla scrapera nie oplaca sie robic filtra visited bo i tak bedzie scrapaowal wiec niech sobie scrapuje a visited bedzie tylko do tego zeby nie wyswietlac w dialogu tego co juz wyswietlilismy
            f"session:{session_id}:meta",
            f"session:{session_id}:pois",
            f"session:{session_id}:visited"
        )

    #get session meta/ verify session
    async def get_meta(self, session_id: str) -> dict:
        meta = await self.redis.get(f"session:{session_id}:meta")
        if meta:
            return json.loads(meta)
        return None
    
    async def verify(self, session_id: str, user_id: str) -> bool:
        meta = await self.get_meta(session_id)
        if not meta:
            return False
        return meta["user_id"] == user_id
    
    async def add_track_point(self, session_id: str, lat: float, lng: float, timestamp: str):
        key = f"session:{session_id}:track"
        point = json.dumps({"lat": lat, "lng": lng, "ts": timestamp})

        await self.redis.rpush(key, point)           
        await self.redis.expire(key, self.SESSION_TTL_SECONDS)

    async def get_track_points(self, session_id: str) -> list[dict]: # do zapisania potem do bazy i moze sie przydac do generowania narracji w trakcie trasy zeby wiedziec gdzie uzytkownik jest i jakie punkty trasy przeszedl
        key = f"session:{session_id}:track"
        raw = await self.redis.lrange(key, 0, -1)    
        return [json.loads(p) for p in raw]

    #scraped POI 

    #visited POI 
