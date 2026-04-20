import httpx
from fastapi import APIRouter, Query
from typing import List

router = APIRouter()


@router.get("/api/route-geojson")
async def get_route_geojson(
    points: List[str] = Query(...)
):
    points_str = ";".join(points)
    osrm_url = f"http://osrm-foot:5000/route/v1/foot/{points_str}?overview=full&geometries=geojson"
    async with httpx.AsyncClient() as client:
        resp = await client.get(osrm_url)
    return resp.json()
