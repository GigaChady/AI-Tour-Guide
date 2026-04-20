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

