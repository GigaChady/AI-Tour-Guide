# AI service (dev)

##  Status

Serwis AI jest **mockowany** i **gotowy do podłączenia**. Pracuje z `location:events` (Redis Stream z backendu) i publikuje wyniki na kanał `tour:{session_id}`. Opis dokładnych schematów danych znajduje się w folderze `schemas`.

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
- **`AI_MOCK=false`** → live pipeline narracji (Nominatim + Overpass + Wikimedia + cloud LLM)

## Przełączenie na live — opcjonalnie

Jeśli chcesz później uruchomić pełny pipeline z LLM:

1. Zmień w `.env`:

```dotenv
AI_MOCK=false
NVIDIA_API_KEY=...
```

Opcjonalnie można ustawić:

```dotenv
WIKIMEDIA_USER_AGENT=AI-Tour-Guide/0.1 (contact: your-email@example.com)
CLOUD_NARRATIVE_MODEL_NAME=meta/llama-3.3-70b-instruct
CLOUD_NARRATIVE_REQUEST_TIMEOUT_SECONDS=30
CLOUD_NARRATIVE_MAX_RETRIES=2
CLOUD_NARRATIVE_RETRY_BACKOFF_SECONDS=2.0
```

2. Uruchom:

```powershell
docker compose up --build
```

Worker będzie czekać na `location:events`, uruchomi full pipeline (POI discovery → Wikimedia enrichment → filtering/context → cloud generation) i publikować rzeczywistą narrację.

## Jak działa część AI (szczegóły)

Poniżej krótki opis głównych kroków pipeline'u AI oraz ważne uwagi dotyczące zdjęć i generowania narracji:

- Odbiór zdarzenia: worker czyta eventy z Redis Stream `location:events`. Event zawiera m.in. `session_id`, `lat`, `lng` i opcjonalnie flagi (np. `include_photos`).
- Pobranie preferencji: przed generacją worker próbuje odczytać z Redisa klucz `preferences:{session_id}` (jeśli istnieje) i przekazać je dalej jako `UserPreferencesCache`.
- Lokalizacja i POI: `LocationDiscoveryTask.get_location_details()` używa Nominatim (reverse geocoding) oraz Overpass (lub alternatywy), a następnie zwraca `LocationDiscoveryResult` z adresem oraz listą `PoiCandidate`.
- Wybór POI: `PoiSelectionTask` ocenia listę POI przez scoring, który łączy odległość, kategorię oraz sygnały popularności (`wikipedia`, `wikidata`, `website`, `description`). Dzięki temu nie zawsze wygrywa najbliższy punkt; preferowane są obiekty bardziej rozpoznawalne i lepsze do narracji.
- Enrichment i filtrowanie: `PoiEnrichmentTask` zbiera informacje o POI przez `WikimediaSearchClient`. Klient próbuje kolejno: tag `wikipedia` z OSM, tag `wikidata` z OSM, a potem wyszukiwanie w Wikipedia API po nazwie POI i lokalizacji. Jeśli Wikimedia nic nie zwróci, task buduje fallback context z metadanych OSM, żeby pipeline nadal mógł wygenerować ostrożną narrację.
- Kontekst z Wikipedii: `WikimediaSearchClient` pobiera dłuższy extract z MediaWiki API (`prop=extracts`, plain text), limitowany domyślnie do ok. 2500 znaków. Krótkie `page/summary` jest używane tylko jako fallback, gdy dłuższy extract jest pusty albo niedostępny.
- Generacja narracji: `CloudNarrativeAgent` generuje finalny tekst narracji przez NVIDIA/Cloud LLM. Wywołanie modelu ma timeout per próba oraz retry z backoffem, żeby chwilowe problemy serwera nie wieszały całego pipeline'u. W trybie mock zwracane są przykładowe teksty.
- Zdjęcia (photos): na razie system używa zdjęć domyślnych przypisanych do kategorii POI (tzw. default photos). Oznacza to, że jeśli POI nie ma własnych zdjęć w zasobach projektu, zwracany jest obraz pasujący do kategorii (np. muzeum -> zdjęcie_muzeum.jpg). W przyszłości możliwe dodanie pobierania zdjęć z zasobów zewnętrznych lub generowania miniatur.
- Publikacja: jeśli generacja powiodła się, worker publikuje dwa komunikaty na kanale Redis `tour:{session_id}`: jeden typu `pois` z listą POI oraz jeden typu `narration` z wygenerowanym tekstem (oraz ewentualnymi metadanymi i linkami do zdjęć).

Ważne uwagi operacyjne:
- Przy dużym obciążeniu publiczne endpointy Overpass mogą zwracać HTTP 429 lub timeouty — w takim przypadku warto rozważyć użycie alternatywnych usług (Google Places, własny Overpass dla regionu) lub cache'owanie wyników.
- Wikimedia jest darmowym źródłem enrichmentu, ale wymaga sensownego `User-Agent`. Jeśli `WIKIMEDIA_USER_AGENT` nie jest ustawiony, aplikacja używa domyślnego `AI-Tour-Guide/0.1 (contact: unavailable)`.
- DuckDuckGo searcher jest zachowany w repo jako niepodpięta klasa pomocnicza, ale domyślny live pipeline go nie używa.
- Prefetch i cache: preferencje użytkownika są buforowane w Redisie pod kluczem `preferences:{session_id}` (format JSON z polem `interests: list[str]`). Dzięki temu dany session może mieć kontekst preferencji używany przy generacji.
- Tryby pracy: w trybie `AI_MOCK=true` pipeline zwraca mockowane POI i narracje (szybkie do testów). W trybie `AI_MOCK=false` uruchamiany jest pełny flow z LLM i enrichmentiem Wikimedia.

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

