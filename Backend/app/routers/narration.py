# jeszcze nie wiem jak to zrobic ale bedzie to cos w stylu tego co mamy w route.py tylko zamiast zarzadzac trasami to bedziemy zarzadzac narracja, czyli bedziemy mieli endpointy do rozpoczynania narracji, pobierania kolejnych dialogow, konczenia narracji itp. i bedzie to korzystalo z tego co mamy w session_service.py do zarzadzania sesja i z tego co mamy w celery.py do odpalenia zadania narracji w tle i potem streamowania wynikow do klienta



from fastapi import APIRouter, Depends


router = APIRouter(prefix="/narration", tags=["narration"])

@router.post("/generate")
async def generate_narration():
    # tutaj bedzie logika do odpalenia zadania narracji w tle i streamowania wynikow do klienta
    pass