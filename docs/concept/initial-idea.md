# AI Tour Guide / Tour Companion

## Opis

**AI Tour Guide** – kontekstowy, narracyjny przewodnik turystyczny oparty na sztucznej inteligencji

### Opis projektu

Celem projektu jest opracowanie mobilnego systemu inteligentnego przewodnika turystycznego, który w sposób ciągły i kontekstowy generuje narrację audio dotyczącą aktualnego otoczenia użytkownika. Aplikacja wykorzystuje lokalizację GPS, zewnętrzne źródła wiedzy oraz model językowy (LLM), aby dynamicznie tworzyć spersonalizowane opowieści o miejscach odwiedzanych podczas spaceru.

W przeciwieństwie do klasycznych audioprzewodników lub aplikacji opartych na statycznych trasach i wcześniej przygotowanych nagraniach, proponowany system nie odtwarza gotowych treści, lecz generuje narrację w czasie rzeczywistym, dopasowując ją do:

- bieżącej lokalizacji użytkownika,
- jego preferencji tematycznych,
- kontekstu spaceru (tempo, czas przebywania w miejscu, wcześniejsze wysłuchane treści).

Rozwiązanie ma charakter **„hands-free"** – użytkownik może zwiedzać miasto w naturalny sposób, słuchając opowieści podobnie jak podcastu lub rozmowy z ludzkim przewodnikiem, bez konieczności patrzenia w ekran telefonu.

---

## Kluczowa idea systemu

System działa jako **ciągły narrator kontekstowy** (*continuous context-aware narrator*), a nie jako odtwarzacz opisów przypisanych do punktów na mapie.

Zamiast modelu:
> „wejdź w punkt → odtwórz gotową historię"

zastosowano model:
> „co kilka sekund: analizuj lokalizację → znajdź interesujące obiekty → wybierz najbardziej adekwatne → wygeneruj narrację → odtwórz audio"

Dzięki temu aplikacja tworzy płynne, naturalne doświadczenie zwiedzania, w którym opowieści są budowane dynamicznie i łączone w spójną historię przestrzeni.

---

## Architektura funkcjonalna (wysoki poziom)

System składa się z następujących modułów:

1. **Moduł lokalizacji** – ciągłe śledzenie pozycji GPS użytkownika
2. **Moduł pozyskiwania wiedzy** – pobieranie informacji o pobliskich punktach POI (API map, otwarte źródła, Wikipedia itp.)
3. **Moduł filtracji i rankingu** – dopasowanie obiektów do preferencji użytkownika
4. **Moduł generowania narracji (LLM)** – tworzenie spójnych, naturalnych opowieści
5. **Moduł syntezy mowy (TTS)** – konwersja tekstu do audio
6. **Silnik kontekstowy** – kontrola kiedy, o czym i jak długo mówić, aby zapewnić naturalny przepływ narracji

To właśnie **silnik kontekstowy**, a nie sam model językowy, stanowi główny element innowacyjny projektu.

---

## UVP (Unique Value Proposition)

**For** travelers exploring a city  
**Who** want to discover interesting stories without constantly looking at a phone,  
**AI Tour Companion** is a mobile AI guide  
**That** narrates the surroundings in real time.

**Unlike** traditional audio guides or route-based apps,  
**Our system** dynamically generates flowing and contextual stories while walking filtered by preferences.


## MVP - Minimalna wersja produktu
*(Tymczasowe - do rewizji po User Stories)*

### Cel MVP
Celem MVP jest walidacja podstawowej hipotezy:

> *„Czy dynamiczna, generowana w czasie rzeczywistym narracja audio zwiększa komfort i atrakcyjność zwiedzania miasta?"*

### Zakres MVP

**W zakresie:**
- pobieranie lokalizacji GPS
- wybór preferencji tematycznych (np. historia / architektura / ciekawostki)
- wyszukiwanie pobliskich punktów POI
- generowanie krótkiej narracji przez LLM
- odtwarzanie treści jako audio
- automatyczne uruchamianie narracji podczas spaceru

**Bez:**
- planowania tras
- rozbudowanej nawigacji
- funkcji społecznościowych
- zaawansowanej personalizacji długoterminowej

MVP skupia się wyłącznie na dostarczeniu kluczowego doświadczenia: **„AI opowiada miasto podczas spaceru"**.

---

## Fazy tworzenia oprogramowania

1. **Planowanie** – W tym miejscu ustalany jest cel przedsięwzięcia. Planowanie zawsze należy poprzedzić analizą biznesową oraz studium wykonalności.

2. **Analiza wymagań** – Zebranie i zrozumienie potrzeb i oczekiwań użytkowników końcowych (np. od klienta).

3. **Projektowanie** – Określenie struktury technicznej i architektonicznej systemu, który będzie spełniał zebrane wymagania oraz tworzenie szczegółowych planów i specyfikacji technicznych.

4. **Implementacja (Kodowanie)** – Przekładanie projektów i specyfikacji na kod źródłowy.

5. **Testowanie i zatwierdzanie oprogramowania** – Weryfikacja i walidacja, czy oprogramowanie działa zgodnie z wymaganiami i nie posiada błędów.

6. **Wdrażanie** – Przeniesienie oprogramowania ze środowiska testowego do środowiska produkcyjnego.

7. **Utrzymanie** – Zapewnienie ciągłej pracy i poprawności oprogramowania po jego wdrożeniu.
