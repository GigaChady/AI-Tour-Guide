## Dokumentacja backendu

### Szybki start

#### Docker
Uruchomienie podstawowych usług backendu:

```powershell
docker compose up -d db redis
```

Jeśli chcesz uruchomić bazę testową ręcznie:

```powershell
docker compose up -d test_db
```

#### Inicjalizacja bazy
Po starcie kontenera bazy możesz zainicjalizować schemat:

```powershell
python init_db.py
```

#### Testy
Testy korzystają z `TEST_DATABASE_URL` z pliku `.env.test`:

```powershell
pytest tests
```

#### Seed danych
Seeder tworzy schemat automatycznie i uzupełnia bazę przykładowymi danymi:

```powershell
python seed_db.py --reset --users 8
```

Opcja `--reset` usuwa wcześniejsze dane przed seedem.

### Endpoints

#### /auth

**POST /auth/register**
- Oczekuje: `{ email, password, imie, nazwisko }` (`RegisterRequest`)
- `imie` i `nazwisko` są opcjonalne
- Zwraca: `{ access_token, refresh_token, token_type }` (`TokenResponse`)

**POST /auth/login**
- Oczekuje: `{ email, password }` (`LoginRequest`)
- Zwraca: `{ access_token, refresh_token, token_type }` (`TokenResponse`)

**POST /auth/google**
- Oczekuje: `{ google_token }` (`GoogleAuthRequest`)
- Dane profilu (`email`, `given_name`, `family_name`) są pobierane z tokena Google
- Zwraca: `{ access_token, refresh_token, token_type }` (`TokenResponse`)

**POST /auth/refresh**
- Oczekuje: `{ refresh_token }` (`RefreshRequest`)
- Zwraca: `{ access_token, refresh_token, token_type }` (`TokenResponse`)

**POST /auth/logout**
- Oczekuje: `{ refresh_token }` (`LogoutRequest`)
- Zwraca: brak treści (`204 No Content`)

#### Model użytkownika

Model `User` zawiera teraz pola:
- `imie` (`str`, opcjonalne)
- `nazwisko` (`str`, opcjonalne)
- `google_id` (`str`, opcjonalne)
- `gender_option_id` (`int`, opcjonalne)
- `gender_custom` (`str`, opcjonalne)
- `wiek` (`float`, opcjonalne)

#### /user/preferences

**GET /user/preferences/questions**
- Oczekuje: brak payloadu
- Zwraca: listę pytań preferencji (`PreferenceQuestion`)
- Jeśli w bazie istnieją definicje pytań, backend używa danych z tabel:
  - `preference_questions`
  - `preference_question_options`
- W przeciwnym razie używa fallbacku z `DEFAULT_PREFERENCE_CATALOG`

**GET /user/preferences**
- Oczekuje: token autoryzacyjny w nagłówku
- Zwraca: obiekt preferencji użytkownika (`UserPreferencesSchema`)

**POST /user/preferences/answers**
- Oczekuje: listę odpowiedzi, np. `[{ question_id, answer_id }, { question_id, answer_ids }, { question_id, value }]`
- Zapisuje odpowiedzi do `user_preferences.interests`
- Zwraca: znormalizowaną listę odpowiedzi (`PreferenceAnswerResponse`)

#### /user/demographics

**GET /user/demographics/questions**
- Oczekuje: brak payloadu
- Zwraca: listę pytań demograficznych (`gender`, `age`) wraz z odpowiedziami
- Lista odpowiedzi dla `gender` jest generowana z tabeli `demographics_gender_options` tylko dla rekordów `is_active=true`, sortowanych po `sort_order`, `id`
- Jeśli tabela jest pusta, backend używa fallbacku z `DEMOGRAPHICS_GENDER_OPTIONS` z `.env`
- Dla płci niestandardowej używany jest `answer_id = custom`

**POST /user/demographics/answers**
- Oczekuje: token autoryzacyjny w nagłówku
- Oczekuje: listę odpowiedzi, np. `[{ question_key, answer_id, custom_text }, { question_key, value }]`
- Backend waliduje `answer_id` względem tej samej listy (DB albo fallback z `.env`)
- Zapisuje wynik do pól `users.gender_option_id`, `users.gender_custom` i `users.wiek`
- Zwraca: brak treści (`204 No Content`)