from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
import httpx

from app.core.config import settings

router = APIRouter(prefix="/search", tags=["search"])


class PlaceResult(BaseModel):
    name: str
    lat: float
    lng: float
    description: str | None


@router.get("", response_model=list[PlaceResult])
async def search_places(q: str = Query(..., min_length=1)):
    url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
    params = {"query": q, "key": settings.GOOGLE_PLACES_API_KEY}
    async with httpx.AsyncClient() as client:
        resp = await client.get(url, params=params)
        resp.raise_for_status()
    data = resp.json()
    if data.get("status") not in ("OK", "ZERO_RESULTS"):
        raise HTTPException(status_code=502, detail=f"Places API error: {data.get('status')}")
    return [
        PlaceResult(
            name=p["name"],
            lat=p["geometry"]["location"]["lat"],
            lng=p["geometry"]["location"]["lng"],
            description=p.get("formatted_address"),
        )
        for p in data.get("results", [])
    ]
