from fastapi import APIRouter, Query
import requests
from typing import List

router = APIRouter()

@router.get("/api/route-geojson") # mam taka koncepepcje ze dane trasy beda zapisywane w redis i pzebyta trase bierzemy stamtad ale jak bedziemy miec na ekranie np wybor miejsc do ktorych chcemy isc to mozemy sobie wybrac i osrm bedzie nam trase wyznaczal na podstawie punktow obecnej lokaliacji i wybranych punktow docelowych
def get_route_geojson(
    points: List[str] = Query(..., description="Lista punktów w formacie lon,lat;lon,lat;... (np. 16.9,51.1;17.0,51.2)")
):
    points_str = ";".join(points)
    osrm_url = f"http://osrm-foot:5000/route/v1/foot/{points_str}?overview=full&geometries=geojson"
    resp = requests.get(osrm_url)
    return resp.json()


#Nowa koncepcja
# polega na tym ze zamiast w redis dane lokalizacji i trasy zapisywane beda we front i na koniec trasy wysylane do back
# React Native może zapisać punkty lokalnie na urządzeniu (AsyncStorage) jako backup, co jest lepszym rozwiązaniem niż ciągłe requesty do backendu.
# W momencie zakończenia trasy, aplikacja mobilna wysyła wszystkie zebrane punkty do backendu, gdzie są one przetwarzane i zapisywane w bazie danych.
