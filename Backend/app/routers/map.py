import httpx
from fastapi import APIRouter, Query
from typing import List

router = APIRouter()


@router.get("/api/route-geojson")
async def get_route_geojson(
    points: List[str] = Query(..., description="Lista punktów w formacie lon,lat;lon,lat;... (np. 16.9,51.1;17.0,51.2)")
):
    points_str = ";".join(points)
    osrm_url = f"http://osrm-foot:5000/route/v1/foot/{points_str}?overview=full&geometries=geojson"
    async with httpx.AsyncClient() as client:
        resp = await client.get(osrm_url)
    return resp.json()
