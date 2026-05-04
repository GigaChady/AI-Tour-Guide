# AI service (dev)

##  Status

Serwis AI jest **mockowany** i **gotowy do podłączenia**. Pracuje z `location:events` (Redis Stream z backendu) i publikuje wyniki na kanał `tour:{session_id}`. Opis dokładnych schematów danych w utils/schemas.py.

**Domyślnie nie wymaga Ollamy ani instalacji modeli LLM** — wystarczy skopiować `.env.example` i uruchomić `docker compose up`.

## Jak to działa (aktualne)

1. Worker czyta ze streamu `location:events` (wysyłane przez backend)
2. Przetwarza event w trybie mock: generuje mockowane `pois` i `narration`
3. Publikuje wynik na kanał Redis `tour:{session_id}` (frontend/backend je odbiera)

**Nie trzeba nic zmieniać w `.env`** — domyślnie `AI_MOCK=true` i serwis pracuje bez Ollamy.

## Uruchomienie (domyślny tryb mock, bez zmian)

```powershell
Copy-Item .env.example .env
docker compose up --build
```

To wszystko. Worker podłączy się do Redisa, będzie czekać na `location:events` i publikować mock payload.

## Dostępne tryby

- **`AI_MOCK=true`** (domyślnie) → mockowane `pois` + `narration`, bez Ollamy
- **`AI_MOCK=false`** → live pipeline narracji (wymaga profilu `llm` i Ollamy)

## Przełączenie na live (z Ollamą) — opcjonalnie

Jeśli chcesz później uruchomić pełny pipeline z LLM:

1. Zmień w `.env`:

```dotenv
AI_MOCK=false
COMPOSE_PROFILES=llm
```

2. Uruchom ze wsparciu profilu LLM:

```powershell
docker compose --profile llm up --build
```

Worker będzie czekać na `location:events`, uruchomi full pipeline (scraping → filtering → generation) i publikować rzeczywistą narrację z Ollamy.

## Jak działa część AI (szczegóły)

Poniżej krótki opis głównych kroków pipeline'u AI oraz ważne uwagi dotyczące zdjęć i generowania narracji:

- Odbiór zdarzenia: worker czyta eventy z Redis Stream `location:events`. Event zawiera m.in. `session_id`, `lat`, `lng` i opcjonalnie flagi (np. `include_photos`).
- Pobranie preferencji: przed generacją worker próbuje odczytać z Redisa klucz `preferences:{session_id}` (jeśli istnieje) i przekazać je dalej jako `UserPreferencesCache`.
- Lokalizacja i POI: `LocationProcessor.get_location_details()` używa Nominatim (reverse geocoding) oraz Overpass (lub alternatywy) żeby zebrać listę punktów zainteresowania (POI) w pobliżu zadanej lokacji. Każdy POI ma kształt: `{"name":..., "category":..., "lat":..., "lon":...}`.
- Wybór POI: `scraping_agent.select_best_poi()` ocenia listę POI (na podstawie kategorii i odległości) i wybiera najlepszy kandydat do wygenerowania narracji.
- Scraping i filtrowanie: `scraping_agent` zbiera surowe informacje o POI (np. przez wyszukiwarkę/serwis zewnętrzny), a `filtering_agent` czyści/skraca i formatuje treść, którą poda się do generatora narracji.
- Generacja narracji: `narrative_generation_agent` (w trybie live przy użyciu Ollama) generuje finalny tekst narracji kontekstowy dla danej lokalizacji/POI. W trybie mock zwracane są przykładowe teksty.
- Zdjęcia (photos): na razie system używa zdjęć domyślnych przypisanych do kategorii POI (tzw. default photos). Oznacza to, że jeśli POI nie ma własnych zdjęć w zasobach projektu, zwracany jest obraz pasujący do kategorii (np. muzeum -> zdjęcie_muzeum.jpg). W przyszłości możliwe dodanie pobierania zdjęć z zasobów zewnętrznych lub generowania miniatur.
- Publikacja: jeśli generacja powiodła się, worker publikuje dwa komunikaty na kanale Redis `tour:{session_id}`: jeden typu `pois` z listą POI oraz jeden typu `narration` z wygenerowanym tekstem (oraz ewentualnymi metadanymi i linkami do zdjęć).

Ważne uwagi operacyjne:
- Przy dużym obciążeniu publiczne endpointy Overpass mogą zwracać HTTP 429 lub timeouty — w takim przypadku warto rozważyć użycie alternatywnych usług (Google Places, własny Overpass dla regionu) lub cache'owanie wyników.
- Prefetch i cache: preferencje użytkownika są buforowane w Redisie pod kluczem `preferences:{session_id}` (format JSON z polem `interests: list[str]`). Dzięki temu dany session może mieć kontekst preferencji używany przy generacji.
- Tryby pracy: w trybie `AI_MOCK=true` pipeline zwraca mockowane POI i narracje (szybkie do testów). W trybie `AI_MOCK=false` uruchamiany jest pełny flow z LLM (wymaga Ollamy / profilu `llm`).

## Testowanie połączenia z Redisem

Aby sprawdzić, czy worker prawidłowo odbiera wiadomości:

```powershell
# W jednym oknie terminala (subskrypcja na wynik)
cd ..\..\Backend
docker compose exec redis redis-cli SUBSCRIBE tour:test-session-1

# W innym oknie (wysłanie testowego eventu)
docker compose exec redis redis-cli XADD location:events "*" session_id test-session-1 lat 52.2297 lng 21.0122
```

Powinno pojawić się:
- `{"type": "pois", "data": [...]}`
- `{"type": "narration", "data": {...}}`

