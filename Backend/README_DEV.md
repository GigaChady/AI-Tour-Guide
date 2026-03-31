## Dokumentacja endpointów backendu

### /auth (autoryzacja)


**POST /auth/register**
- Oczekuje: `{ email, password, imie, nazwisko, plec, wiek }` (RegisterRequest)
	- imie, nazwisko, plec, wiek są opcjonalne
- Zwraca: `{ access_token, refresh_token }` (TokenResponse)

**POST /auth/login**
- Oczekuje: `{ email, password }` (LoginRequest)
- Zwraca: `{ access_token, refresh_token }` (TokenResponse)


**POST /auth/google**
- Oczekuje: `{ google_token, imie, nazwisko, plec, wiek }` (GoogleAuthRequest)
	- imie, nazwisko, plec, wiek są opcjonalne
- Zwraca: `{ access_token, refresh_token }` (TokenResponse)
### Model użytkownika

Model użytkownika (`User`) zawiera teraz dodatkowe pola:
- `imie` (str, opcjonalne)
- `nazwisko` (str, opcjonalne)
- `plec` (str, opcjonalne)
- `wiek` (float, opcjonalne)

Pola te są obsługiwane podczas rejestracji oraz logowania przez Google.

**POST /auth/refresh**
- Oczekuje: `{ refresh_token }` (RefreshRequest)
- Zwraca: `{ access_token, refresh_token }` (TokenResponse)

**POST /auth/logout**
- Oczekuje: `{ refresh_token }` (LogoutRequest)
- Zwraca: brak treści (204 No Content)

### /user/preferences (preferencje użytkownika)

**GET /user/preferences**
- Oczekuje: token autoryzacyjny (w nagłówku)
- Zwraca: obiekt preferencji użytkownika (UserPreferencesSchema)

**PUT /user/preferences**
- Oczekuje: JSON z preferencjami (np. `{ interests: [...] }`)
- Zwraca: brak treści (204 No Content)


