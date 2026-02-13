## BridgeU – Software Test Record

**Document Name**: BridgeU-TestRecord_v1.0  
**Prepared by**: Zhiyi Pan  
**Version**: v1.0  
**Date**: March 2026  

---

### 1. Introduction

#### 1.1 Purpose

The purpose of this Software Test Record is to document the execution results and pass/fail status of the test cases defined in the **BridgeU-TestPlan.md**.  
It provides objective evidence that the implemented system (BridgeU web platform) has been verified against the user and system requirements described in the **BridgeU-SRS.md** and designed in the **BridgeU-SDDv.2.md**.

#### 1.2 Scope

This Software Test Record is intended to cover **all test activities for the BridgeU web platform**, including:

- **Feature 1 – Daily Briefing System** (news aggregation, filtering, search, language switching, detail view, original link).  
- **Feature 2 – Community Interaction** (forums / posts / comments / reactions, if applicable).  
- **Feature 3 – Authentication & Profile / Private Messaging** (user login, roles/permissions, profile management, messaging).  
- Any additional features and cross‑cutting concerns (e.g., security, performance, accessibility) described in the SRS and SDD.

For each feature, this record will include:

- **Unit Testing (UTC)**: Component methods, backend controllers, services, repositories, and utility classes.  
- **System Testing (STC)**: Full browser‑level scenarios exercising the platform as a whole.

All test case definitions and detailed input/expected behaviour are taken from `BridgeU-TestPlan.md`. This record adds the **actual result** and **Pass/Fail (P/F)** status after execution.  


#### 1.3 Acronyms and Definitions

- **UTC**: Unit Test Case.  
- **ITC**: Integration Test Case.  
- **STC**: System Test Case.  
- **P/F**: Pass / Fail.  

---

### 2. Unit Test Record

This section tracks the execution of individual software units or components for **Feature 1 – Daily Briefing System**.

#### 2.1 Unit Test Execution Summary – Feature 1

| Test Case ID | Test Function                                         | Layer / Component                   | Result (P/F) | Remarks                                              |
|--------------|-------------------------------------------------------|-------------------------------------|--------------|------------------------------------------------------|
| UTC-01       | `fetchDailyBriefing()`                                | Frontend Vue component method       | Pass         | All 5 sub‑test cases passed as specified in Test Plan. |
| UTC-02       | `fetchNewsDetail()`                                   | Frontend Vue component method       | Pass         | All 5 sub‑test cases passed as specified in Test Plan. |
| UTC-03       | `handleSearch()`                                      | Frontend Vue component method       | Pass         | Search triggers, pagination reset verified.          |
| UTC-04       | `applyFilters()`                                      | Frontend Vue component method       | Pass         | Date range validation and filter application verified. |
| UTC-05       | `resetFilters()`                                      | Frontend Vue component method       | Pass         | Filters cleared, pagination reset, data refetch OK.  |
| UTC-06       | `handlePageChange(page)`                              | Frontend Vue component method       | Pass         | Page change and smooth scroll behavior verified.     |
| UTC-07       | `normalizeDateValue(value)`                           | Frontend utility function           | Pass         | Various input formats normalized correctly.          |
| UTC-08       | `formatDate(date)`                                    | Frontend utility function           | Pass         | Display format and padding verified.                 |
| UTC-09       | `convertToDTO(news, lang)`                            | Backend service helper              | Pass         | Translation selection & Thai hiding verified.        |
| UTC-10       | `getDailyBriefing()` – backend controller             | Backend controller `NewsController` | Pass         | Pagination, filters, and error handling verified.    |
| UTC-11       | `getNewsDetail()` – backend controller                | Backend controller `NewsController` | Pass         | Detail retrieval and `originalContent` rules OK.     |
| UTC-12       | `findByKeyword(keyword, pageable)`                    | Backend repository `NewsRepository` | Pass         | Search scope and pagination verified.                |
| UTC-13       | `findByPublishDateBetweenOrdered(start,end,pageable)`| Backend repository `NewsRepository` | Pass         | Date range and sort order verified.                  |

All unit test cases listed above were executed according to **Chapter 3.1** of the Test Plan; no deviations from the designed steps were required.

---

#### 2.2 Detailed Unit Test Records

##### 2.2.1 UTC-01: Test fetchDailyBriefing()

**Test ID**: UTC-01  
**Test Function**: `fetchDailyBriefing()`  
**Description**: Tests the frontend component method for fetching daily briefing list, verifying it correctly handles pagination, language parameters, search keywords, date filters, and properly processes API responses and error cases.  
**Prerequisite**: Frontend application is running and connected to the backend API; database contains at least 20 valid news records.  

**Postman example URLs** (for actual response verification):  
- Default list: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en`  
- With keyword: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&keyword=Thailand`  
- With date range: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&startDate=2026-01-01&endDate=2026-01-31`  

Test Results Table:

| ID | Description                                                                 | Input                                                                                                                                    | Expected Result                                                                                                                                                                                   | result |
|----|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify that `fetchDailyBriefing()` successfully retrieves news list with default parameters | `{ "currentPage": 1, "pageSize": 10, "currentLang": "en", "searchKeyword": "", "filterStartDate": null, "filterEndDate": null }`         | `newsList` contains an array of news items; `pagination.totalElements > 0`, `pagination.totalPages >= 1`; component state has `loading = false`, `error = null`.                                 | pass   |
| 2  | Verify that `fetchDailyBriefing()` handles API error response correctly     | `{ "apiResponse": { "success": false, "message": "Error message" } }`                                                                   | Component sets `error = "Error message"`, `loading = false`, and keeps previous `newsList` value (not reset to empty array).                                                                     | pass   |
| 3  | Verify that `fetchDailyBriefing()` includes keyword parameter when `searchKeyword` is provided | `{ "searchKeyword": "Thailand" }`                                                                                                       | Outgoing API request includes `params.keyword = "Thailand"` in `GET /api/news/daily-briefing`.                                                                                                   | pass   |
| 4  | Verify that `fetchDailyBriefing()` includes date parameters when filters are provided | `{ "filterStartDate": "2026-01-01", "filterEndDate": "2026-01-31" }`                                                                    | Outgoing request includes `params.startDate = "2026-01-01"` and `params.endDate = "2026-01-31"`.                                                                                                  | pass   |
| 5  | Verify that `fetchDailyBriefing()` includes language parameter              | `currentLang = "zh"`                                                                                                                    | API request includes `params.lang = "zh"` (e.g., `/api/news/daily-briefing?...&lang=zh`).                                                                                                         | pass   |

---

##### 2.2.2 UTC-02: Test fetchNewsDetail()

**Test ID**: UTC-02  
**Test Function**: `fetchNewsDetail()`  
**Description**: Tests the frontend component method for fetching news details, verifying it correctly retrieves news details based on news ID and language parameters, and handles 404 errors and network errors.  
**Prerequisite**: Same as UTC‑01, and at least one news record with a valid ID exists in the database.  

**Postman example URLs** (replace `{id}` with a real ID such as `609`):  
- English detail: `http://localhost:8080/api/news/daily-briefing/{id}?lang=en`  
- Chinese detail: `http://localhost:8080/api/news/daily-briefing/{id}?lang=zh`  
- 404 case: `http://localhost:8080/api/news/daily-briefing/999999?lang=en`  

Test Results Table:

| ID | Description                                                         | Input                                                                                 | Expected Result                                                                                                                                                 | result |
|----|---------------------------------------------------------------------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify that `fetchNewsDetail()` successfully retrieves news detail  | `{ "newsId": 123, "currentLang": "en" }`                                              | Component state `news.id = 123`, `news.title` and `news.summary` are not empty; `loading = false`, `error = null`.                                              | pass   |
| 2  | Verify that `fetchNewsDetail()` handles 404 error when news not found | `{ "apiResponse": { "status": 404 } }`                                               | Component sets `error` to localized “News not found” message, `loading = false`, `news = null`.                                                                | pass   |
| 3  | Verify that `fetchNewsDetail()` includes language parameter         | `{ "currentLang": "zh" }`                                                            | API request includes `params.lang = "zh"` (e.g., `/api/news/daily-briefing/{id}?lang=zh`).                                                                      | pass   |
| 4  | Verify that `fetchNewsDetail()` sets `originalContent` when available and not Thai | `{ "apiResponse": { "originalContent": "English content" } }`                        | Component sets `originalContent = "English content"` and renders it in the detail view.                                                                         | pass   |
| 5  | Verify that `fetchNewsDetail()` sets `originalContent` to null when content is Thai | `{ "apiResponse": { "content": "Thai content" } }` (detected as Thai by backend)     | Component sets `originalContent = null`; detail page does not show Thai original text, only translated title/summary or placeholders according to backend logic. | pass   |

---

##### 2.2.3 UTC-03: Test handleSearch()

**Test ID**: UTC-03  
**Test Function**: `handleSearch()`  
**Description**: Tests the frontend component method for handling search operations, verifying it resets pagination, triggers search requests, and correctly handles clearing search keywords.  
**Prerequisite**: Daily Briefing page is loaded successfully and `fetchDailyBriefing()` is available on the component instance.  

Test Results Table:

| ID | Description                                            | Input                                                           | Expected Result                                                               | result |
|----|--------------------------------------------------------|-----------------------------------------------------------------|-------------------------------------------------------------------------------|--------|
| 1  | Verify that `handleSearch()` resets `currentPage` to 1 | `{ "currentPage": 5, "searchKeyword": "Thailand" }`            | After call, `currentPage` becomes 1 and `fetchDailyBriefing()` is called.     | pass   |
| 2  | Verify that `handleSearch()` triggers `fetchDailyBriefing()` | `{ "searchKeyword": "test" }`                          | `fetchDailyBriefing()` is called with the updated `searchKeyword = "test"`.   | pass   |

---

##### 2.2.4 UTC-04: Test applyFilters()

**Test ID**: UTC-04  
**Test Function**: `applyFilters()`  
**Description**: Tests the frontend component method for applying date filters, verifying it correctly sets start and end dates, resets pagination, and triggers data refetch.  
**Prerequisite**: Daily Briefing page is loaded with date filter controls rendered and bound to component state.  

Test Results Table:

| ID | Description                                           | Input                                                                                       | Expected Result                                                                                                   | result |
|----|-------------------------------------------------------|---------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify that `applyFilters()` validates invalid date range | `{ "filterStartDate": "2026-01-31", "filterEndDate": "2026-01-01" }`                    | Frontend shows warning message; `fetchDailyBriefing()` is **not** called (`fetchDailyBriefingCalled = false`).     | pass   |
| 2  | Verify that `applyFilters()` resets `currentPage` to 1 on valid date range | `{ "filterStartDate": "2026-01-01", "filterEndDate": "2026-01-31" }`          | `currentPage` set to 1 and `fetchDailyBriefing()` called once.                                                    | pass   |
| 3  | Verify that `applyFilters()` works with only `startDate` | `{ "filterStartDate": "2026-01-01", "filterEndDate": null }`                             | `fetchDailyBriefing()` called; outgoing API has `startDate = "2026-01-01"` and no `endDate`.                      | pass   |
| 4  | Verify that `applyFilters()` works with only `endDate` | `{ "filterStartDate": null, "filterEndDate": "2026-01-31" }`                              | `fetchDailyBriefing()` called; outgoing API has `endDate = "2026-01-31"` and no `startDate`.                      | pass   |

---

##### 2.2.5 UTC-05: Test resetFilters()

**Test ID**: UTC-05  
**Test Function**: `resetFilters()`  
**Description**: Tests the frontend component method for resetting filters, verifying it clears date filters and search keywords, resets pagination, and refetches all news data.  
**Prerequisite**: Daily Briefing page is loaded and at least one filter (keyword or date) has been applied.  

Test Results Table:

| ID | Description                                     | Input                                                                                                                                      | Expected Result                                                                                                                                                          | result |
|----|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify that `resetFilters()` clears all filter values | `{ "searchKeyword": "test", "filterStartDate": "2026-01-01", "filterEndDate": "2026-01-31", "currentPage": 5 }`                       | After call: `searchKeyword = ""`, `filterStartDate = null`, `filterEndDate = null`, `currentPage = 1`, and `fetchDailyBriefing()` is called to reload default list.     | pass   |

---

##### 2.2.6 UTC-06: Test handlePageChange()

**Test ID**: UTC-06  
**Test Function**: `handlePageChange(page)`  
**Description**: Tests the frontend component method for handling page changes, verifying it correctly updates the current page number and refetches news data for the corresponding page.  
**Prerequisite**: Daily Briefing list is visible with pagination controls rendered.  

Test Results Table:

| ID | Description                                                | Input                                                | Expected Result                                                                                      | result |
|----|------------------------------------------------------------|------------------------------------------------------|------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify that `handlePageChange()` updates `currentPage`     | `{ "currentPage": 1, "page": 3 }`                    | `currentPage` becomes 3 and `fetchDailyBriefing()` is called once with page index 3.                 | pass   |
| 2  | Verify that `handlePageChange()` scrolls to top            | `{ "page": 2 }`                                      | `window.scrollTo({ top: 0, behavior: "smooth" })` is invoked in addition to calling data refetch.   | pass   |

---

##### 2.2.7 UTC-07: Test normalizeDateValue()

**Test ID**: UTC-07  
**Test Function**: `normalizeDateValue(value)`  
**Description**: Tests the utility method for normalizing date values, verifying it correctly handles various date input formats (strings, Date objects, null, etc.) and returns normalized date values.  
**Prerequisite**: JavaScript date utilities and localization libraries used by the component are available.  

Test Results Table:

| ID | Description                                                             | Input                                      | Expected Result                           | result |
|----|-------------------------------------------------------------------------|--------------------------------------------|-------------------------------------------|--------|
| 1  | Verify that `normalizeDateValue()` converts Date object to `yyyy-MM-dd` | `{ "value": new Date("2026-01-15") }`      | `returnValue = "2026-01-15"`              | pass   |
| 2  | Verify that `normalizeDateValue()` handles `yyyy-MM-dd` string          | `{ "value": "2026-01-15" }`                | `returnValue = "2026-01-15"`              | pass   |
| 3  | Verify that `normalizeDateValue()` handles ISO date string              | `{ "value": "2026-01-15T00:00:00Z" }`      | `returnValue = "2026-01-15"`              | pass   |
| 4  | Verify that `normalizeDateValue()` returns null for null input          | `{ "value": null }`                        | `returnValue = null`                      | pass   |
| 5  | Verify that `normalizeDateValue()` handles invalid string containing letters | `{ "value": "invalid-date" }`         | `returnValue = null`                      | pass   |

---

##### 2.2.8 UTC-08: Test formatDate()

**Test ID**: UTC-08  
**Test Function**: `formatDate(date)`  
**Description**: Tests the utility method for formatting dates, verifying it correctly formats date displays based on the current interface language and handles various date input formats.  
**Prerequisite**: Component or utility module has access to the current interface language setting (`currentLang`).  

Test Results Table:

| ID | Description                                              | Input                                        | Expected Result                          | result |
|----|----------------------------------------------------------|----------------------------------------------|------------------------------------------|--------|
| 1  | Verify that `formatDate()` formats date correctly        | `{ "date": "2026-01-15T14:30:00" }`         | `returnValue = "15-01-2026 14:30"`       | pass   |
| 2  | Verify that `formatDate()` handles null input            | `{ "date": null }`                          | `returnValue = ""`                       | pass   |
| 3  | Verify that `formatDate()` handles invalid date          | `{ "date": "invalid" }`                     | `returnValue = ""`                       | pass   |
| 4  | Verify that `formatDate()` pads single digit months/days | `{ "date": "2026-01-05T09:05:00" }`         | `returnValue = "05-01-2026 09:05"`       | pass   |

---

##### 2.2.9 UTC-09: Test convertToDTO()

**Test ID**: UTC-09  
**Test Function**: `convertToDTO(news, lang)`  
**Description**: Tests the backend service method for converting news entities to Data Transfer Objects (DTOs), verifying it correctly selects Chinese or English versions of titles and summaries and hides Thai content.  
**Prerequisite**: News entity objects are instantiated in memory; no database access is required.  

Test Results Table:

| ID | Description                                                           | Input                                                                                                                | Expected Result                                                                                                                   | result |
|----|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify that `convertToDTO()` uses Chinese translation when `lang="zh"` | `{ "news": { "titleZh": "中文标题", "summaryZh": "中文摘要" }, "lang": "zh" }`                                      | DTO has `title = "中文标题"`, `summary = "中文摘要"`.                                                                               | pass   |
| 2  | Verify that `convertToDTO()` uses English translation when `lang="en"` | `{ "news": { "titleEn": "English Title", "summaryEn": "English Summary" }, "lang": "en" }`                        | DTO has `title = "English Title"`, `summary = "English Summary"`.                                                                 | pass   |
| 3  | Verify that `convertToDTO()` never shows Thai content                 | `{ "news": { "title": "contains Thai characters", "titleZh": null }, "lang": "zh" }`                                 | DTO `title` does not contain Thai characters; uses English translation or placeholder instead.                                   | pass   |
| 4  | Verify that `convertToDTO()` falls back to English when Chinese unavailable | `{ "news": { "titleZh": null, "titleEn": "English Title", "title": "not Thai" }, "lang": "zh" }`             | DTO `title = "English Title"`.                                                                                                    | pass   |
| 5  | Verify that `convertToDTO()` sets placeholder when no translation for Thai content | `{ "news": { "title": "contains Thai", "titleZh": null, "titleEn": null }, "lang": "zh" }`                | DTO `title = "[新闻标题翻译中...]"`.                                                                                               | pass   |

---

##### 2.2.10 UTC-10: Test getDailyBriefing() – Backend

**Test ID**: UTC-10  
**Test Function**: `getDailyBriefing()`  
**Description**: Tests the backend controller API method for retrieving daily briefing list, verifying it correctly handles pagination, language, date filtering, keyword search parameters, and returns appropriate response data.  
**Prerequisite**: Spring Boot backend is running with access to the `news` table; `NewsRepository` is correctly wired.  

**Postman example URLs**:  
- Default: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en`  
- With keyword: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&keyword=Thailand`  
- With start date only: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&startDate=2026-01-01`  
- With end date only: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&endDate=2026-01-31`  
- With date range: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&startDate=2026-01-01&endDate=2026-01-31`  

Test Results Table:

| ID | Description                                                          | Input                                                                                                              | Expected Result                                                                                                                                               | result |
|----|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify default paginated list                                       | `{ "page": 0, "size": 10, "lang": "en", "startDate": null, "endDate": null, "keyword": null }`                     | JSON response `{ "success": true, "data": [NewsBriefDTO...], "pagination": { "totalElements": >= 0, "totalPages": >= 0 } }`.                                 | pass   |
| 2  | Verify keyword filter                                               | `{ "keyword": "Thailand" }`                                                                                        | All items in `data` contain “Thailand” in title, summary, or translated fields; no unrelated records returned.                                               | pass   |
| 3  | Verify date range filter                                            | `{ "startDate": "2026-01-01", "endDate": "2026-01-31" }`                                                           | All returned `publishDate` values are between the given dates (inclusive).                                                                                   | pass   |
| 4  | Verify combined filters                                             | `{ "keyword": "Thailand", "startDate": "2026-01-01", "endDate": "2026-01-31" }`                                     | Returned records match both keyword and date range filter simultaneously.                                                                                    | pass   |
| 5  | Verify invalid date format handling                                | `{ "startDate": "invalid-date" }`                                                                                  | Invalid date is ignored; method behaves as if no date filter applied and returns all news (subject to other filters).                                       | pass   |
| 6  | Verify error response on exception                                  | Simulation: database connection error during execution                                                              | Response `{ "success": false, "message": "<contains error details>" }` with appropriate HTTP status (e.g., 500).                                            | pass   |

---

##### 2.2.11 UTC-11: Test getNewsDetail() – Backend

**Test ID**: UTC-11  
**Test Function**: `getNewsDetail()`  
**Description**: Tests the backend controller API method for retrieving news details, verifying it correctly returns news details based on news ID and language parameters, and handles cases where news does not exist.  
**Prerequisite**: Backend is running; `news` table contains at least one valid record and ability to simulate a non‑existent ID.  

**Postman example URLs** (replace `{id}` with a real ID such as `609`):  
- English detail: `http://localhost:8080/api/news/daily-briefing/{id}?lang=en`  
- Chinese detail: `http://localhost:8080/api/news/daily-briefing/{id}?lang=zh`  
- Non‑existent ID: `http://localhost:8080/api/news/daily-briefing/999999?lang=en`  

Test Results Table:

| ID | Description                                                        | Input                                                                                         | Expected Result                                                                                                                                     | result |
|----|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify detail returned for valid ID                                | `{ "id": 123, "lang": "en" }`                                                                 | Response `{ "success": true, "data": { "id": 123, "title": "...", "summary": "..." } }`; fields are not empty.                                     | pass   |
| 2  | Verify 404 for non‑existent ID                                     | `{ "id": 99999, "lang": "en" }`                                                               | HTTP status 404, response `{ "success": false, "message": "News not found" }`.                                                                     | pass   |
| 3  | Verify Chinese translation when `lang="zh"`                        | `{ "id": 123, "lang": "zh", "news": { "titleZh": "exists", "summaryZh": "exists" } }`        | `data.title = news.titleZh`, `data.summary = news.summaryZh`.                                                                                      | pass   |
| 4  | Verify exclusion of Thai `originalContent`                         | `{ "news": { "originalContent": "contains Thai characters" } }`                               | In DTO/response, `originalContent = null`.                                                                                                          | pass   |
| 5  | Verify inclusion of non‑Thai `originalContent`                     | `{ "news": { "originalContent": "English content" } }`                                        | In DTO/response, `originalContent = "English content"`.                                                                                             | pass   |
| 6  | Verify error response on exception                                 | Simulation: database error during detail retrieval                                            | HTTP status 500, response `{ "success": false, "message": "<contains error details>" }`.                                                           | pass   |

---

##### 2.2.12 UTC-12: Test findByKeyword()

**Test ID**: UTC-12  
**Test Function**: `findByKeyword(keyword, pageable)`  
**Description**: Tests the backend repository method for searching news by keyword, verifying it performs fuzzy matching searches across multiple fields and supports pagination.  
**Prerequisite**: `NewsRepository` is available in a Spring test context with seeded test data.  

Test Results Table:

| ID | Description                                              | Input                                                                                                   | Expected Result                                                                                           | result |
|----|----------------------------------------------------------|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify search in `title` field                           | `{ "keyword": "Thailand", "news": { "title": "contains 'Thailand'" } }`                                 | Returned `Page` contains news where `title` includes “Thailand”.                                          | pass   |
| 2  | Verify search in `summary` field                         | `{ "keyword": "economy", "news": { "summary": "contains 'economy'" } }`                                 | Returned `Page` contains news where `summary` includes “economy”.                                         | pass   |
| 3  | Verify search in translation fields                      | `{ "keyword": "中文", "news": { "titleZh": "contains '中文'" } }`                                        | Returned `Page` contains news where `titleZh` includes “中文”.                                            | pass   |
| 4  | Verify case‑insensitive search                           | `{ "keyword": "THAILAND", "news": { "title": "Thailand" } }`                                           | Returned `Page` includes matching news even if case differs.                                              | pass   |
| 5  | Verify empty result when no matches                      | `{ "keyword": "nonexistentkeyword12345" }`                                                              | Returned `Page` has `totalElements = 0` and empty content list.                                           | pass   |
| 6  | Verify pagination is respected                           | `{ "keyword": "Thailand", "pageable": { "page": 0, "size": 10 } }`                                      | Returned `Page` size is `<= 10`, and `totalElements` reflects global count of all matches.                | pass   |

---

##### 2.2.13 UTC-13: Test findByPublishDateBetweenOrdered()

**Test ID**: UTC-13  
**Test Function**: `findByPublishDateBetweenOrdered(startDate, endDate, pageable)`  
**Description**: Tests the backend repository method for querying news by publication date range, verifying it correctly filters within the specified date range, orders by publication date in descending order, and supports pagination.  
**Prerequisite**: `NewsRepository` is available with multiple news records covering different publish dates.  

Test Results Table:

| ID | Description                                                    | Input                                                                                                           | Expected Result                                                                                                      | result |
|----|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Verify filter by date range                                    | `{ "startDate": "2026-01-01", "endDate": "2026-01-31", "news": { "publishDate": "in range" } }`                 | Returned `Page` contains only news items with `publishDate` between `startDate` and `endDate` (inclusive).          | pass   |
| 2  | Verify order by `publishDate` DESC                             | `{ "newsItems": "multiple in date range" }`                                                                     | Items in returned `Page` are ordered from newest to oldest by `publishDate`.                                         | pass   |
| 3  | Verify inclusion of boundary dates                             | `{ "startDate": "2026-01-15T00:00:00", "endDate": "2026-01-15T23:59:59", "news": { "publishDate": "2026-01-15" } }` | Returned `Page` includes news items whose `publishDate` equals `2026-01-15`.                                        | pass   |
| 4  | Verify empty result when no matches                            | `{ "startDate": "2026-12-01", "endDate": "2026-12-31", "news": "none in this range" }`                          | Returned `Page` has `totalElements = 0` and empty content list.                                                      | pass   |
| 5  | Verify pagination is respected                                 | `{ "startDate": "2026-01-01", "endDate": "2026-01-31", "pageable": { "page": 0, "size": 10 } }`                  | Returned `Page` size is `<= 10`; if more than 10 matches exist, `totalPages > 1`.                                   | pass   |

---

### 3. System Test Record

This section documents the execution results of **system‑level** test cases for **Feature 1 – Daily Briefing System**, as defined in **Chapter 4.1** of `BridgeU-TestPlan.md`.

#### 3.1 System Test Execution Summary – Feature 1

| Test Case ID | Description                             | Result (P/F) | Remarks                                                                 |
|--------------|-----------------------------------------|--------------|-------------------------------------------------------------------------|
| STC-01       | View Daily Briefing List (Default)      | Pass         | Default list, pagination, and no‑Thai rule verified.                   |
| STC-02       | Filter News by Date Range               | Pass         | Date filters (start only, end only, invalid range) behave as expected. |
| STC-03       | Search Daily Briefing by Keyword        | Pass         | Keyword search and combination with date filters verified.             |
| STC-04       | Switch Interface Language               | Pass         | Language toggle reloads list with correct `lang` parameter.            |
| STC-05       | View News Detail                        | Pass         | Detail page retrieval and `originalContent` rules verified.            |
| STC-06       | Open Original News Link                 | Pass         | “Open Original” opens correct URL from both list and detail views.     |

All six system test cases were executed in a browser environment with the Vue frontend and Spring Boot backend running against a populated `news` table.

---

#### 3.2 Detailed System Test Records

##### 3.2.1 STC-01: View Daily Briefing List (Default)

**Feature**: Feature 1 – Daily Briefing System  
**Test Case ID**: STC-01  
**Description**: Verifies that the integrated frontend and backend correctly display the Daily Briefing list using default parameters, including pagination and basic data fields, and that no Thai text appears in the UI.  
**Prepared Data**: Backend and frontend running; DB has at least 20 valid news records with non‑Thai translations.  

**Postman example URL for backend list request**:  
- `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en`  

Test Results Table:

| No | Description                                    | Input / Action                                                                                     | Expected Result                                                                                                                                                            | result |
|----|------------------------------------------------|----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Open Daily Briefing page with default state    | Open BridgeU and navigate to “Daily Briefing” page with no filters.                               | Page shows 10 news cards with title, summary, source, publish date and actions; no search keyword, date or source filter is selected.                                     | pass   |
| 2  | Verify pagination behaviour                    | Scroll to bottom of list.                                                                          | Pagination bar shows at least 2 pages when total records > 10.                                                                      | pass   |
| 3  | Check UI language rule                         | Inspect all items on first page.                                                                  | All visible titles and summaries are in Chinese or English only; no Thai characters appear anywhere in the list.                                                          | pass   |

---

##### 3.2.2 STC-02: Filter News by Date Range

**Test Case ID**: STC-02  
**Description**: Verifies that the system correctly filters Daily Briefing news items by publish date range across frontend and backend.  
**Prepared Data**: Same environment as STC‑01; DB contains news items over at least the last 30 days.  

**Postman example URLs for date filter**:  
- Fixed range: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&startDate=2026-01-01&endDate=2026-01-07`  
- Start only: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&startDate=2026-01-10`  
- End only: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&endDate=2026-01-20`  

Test Results Table:

| No | Description                              | Input / Action                                                                                               | Expected Result                                                                                                                                                           | result |
|----|------------------------------------------|--------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Filter list by fixed date range          | Set start date `01-01-2026`, end date `07-01-2026`, click “Apply Filters”.                                  | Network request `GET /api/news/daily-briefing?...&startDate=2026-01-01&endDate=2026-01-07`; results only contain news in this range and count matches DB records.        | pass   |
| 2  | Filter with only start date              | Clear filters, set start date `10-01-2026`, leave end date empty, click “Apply Filters”.                    | Request includes `startDate=2026-01-10` and no `endDate`; backend returns news from `2026-01-10` up to “today” without error.                                             | pass   |
| 3  | Filter with only end date and inferred start date | Clear filters, set end date `20-01-2026`, leave start date empty, click “Apply Filters”.             | Backend infers start date as 30 days before `20-01-2026`; results only include news in that computed range.                                                              | pass   |
| 4  | Validate invalid date range              | Set start date `10-02-2026`, end date `01-02-2026`, click “Apply Filters”.                                   | Frontend shows validation warning (“start date cannot be later than end date”); no API request is sent; existing list content remains unchanged.                         | pass   |

---

##### 3.2.3 STC-03: Search Daily Briefing by Keyword

**Test Case ID**: STC-03  
**Description**: Verifies that keyword search works end‑to‑end, including parameter passing and search scope.  
**Prepared Data**: Same environment as STC‑01; DB contains items with keyword “Thailand” in title/summary/translation fields.  

**Postman example URL for keyword search**:  
- `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en&keyword=Thailand`  

Test Results Table:

| No | Description                       | Input / Action                                                                                     | Expected Result                                                                                                                                                                 | result |
|----|-----------------------------------|----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Search by keyword                 | Enter `Thailand` in search box and press the Filter/Search button.                                | UI resets current page to 1; request `GET /api/news/daily-briefing?...&keyword=Thailand`; results only contain news where title, summary or any translation includes “Thailand” (case‑insensitive). | pass   |
| 2  | Clear keyword search              | Clear search box and click Search again.                                                          | New request has no `keyword` parameter; list shows unfiltered results (only active date/source filters, if any, are applied).                                                 | pass   |
| 3  | Combine keyword and date filters  | Enter `student`, set a valid date range, click Search.                                            | Request includes both `keyword` and `startDate`/`endDate`; results only include items matching both the keyword and the date range.                                           | pass   |

---

##### 3.2.4 STC-04: Switch Interface Language

**Test Case ID**: STC-04  
**Description**: Verifies that switching between English and Chinese interfaces correctly reloads the Daily Briefing list using appropriate translations while still hiding Thai content.  
**Prepared Data**: Same as STC‑01; DB contains records with both English and Chinese translations.  

**Postman example URLs for language switching**:  
- English list: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=en`  
- Chinese list: `http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=zh`  

Test Results Table:

| No | Description                | Input / Action                                                              | Expected Result                                                                                       | result |
|----|----------------------------|------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|--------|
| 1  | Load list in English       | Set interface language to `en`, open Daily Briefing list.                   | News list shows titles/summaries in English where `titleEn` / `summaryEn` are available.             | pass   |
| 2  | Switch to Chinese          | Use language toggle to switch to `zh`.                                      | List is reloaded; network request includes `lang=zh`; items with Chinese translations show Chinese.  | pass   |
| 3  | Switch back to English     | Switch language back to `en`.                                               | Request is resent with `lang=en`; UI shows English texts again; no Thai content appears.             | pass   |

---

##### 3.2.5 STC-05: View News Detail

**Test Case ID**: STC-05  
**Description**: Verifies that selecting a news item from the list opens the detail view and correctly retrieves full information from the backend, including rules for `originalContent`.  
**Prepared Data**: Same as STC‑01; DB contains at least one news record with valid translations.  

**Postman example URLs for detail** (replace `{id}` with a real ID such as `609`):  
- `http://localhost:8080/api/news/daily-briefing/{id}?lang=en`  
- `http://localhost:8080/api/news/daily-briefing/{id}?lang=zh`  

Test Results Table:

| No | Description          | Input / Action                                                        | Expected Result                                                                                                                                           | result |
|----|----------------------|------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Open detail content  | From list, click “View Detail” on a news item.                        | Network sends `GET /api/news/daily-briefing/{id}?lang=<currentLang>`; response `{ success: true, data: NewsBriefDTO, originalContent: "..." }`; detail page shows title, summary, source, publish date; Thai original text is not displayed. | pass   |

---

##### 3.2.6 STC-06: Open Original News Link

**Test Case ID**: STC-06  
**Description**: Verifies that the “Open Original” action correctly opens the original news article URL from backend data, both from the list and from the detail view.  
**Prepared Data**: Same as STC‑01; selected news items have a valid `originalUrl`.  

Test Results Table:

| No | Description                     | Input / Action                                                                                                 | Expected Result                                                                                                                                         | result |
|----|---------------------------------|-----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| 1  | Open original link from list    | On Daily Briefing list, click “Open Original” for a news item with valid `originalUrl`.                        | Browser opens a new tab with URL equal to `originalUrl` from backend; existing BridgeU tab remains open; Network tab shows no extra API calls.        | pass   |
| 2  | Open original link from detail  | From same item, click “View Detail” then “Open Original” on detail page.                                       | New tab opens with the same URL as in test 1; URL matches the `originalUrl` value returned by backend for that news item.                             | pass   |

---

### 4. Test Summary

- **Scope of this record**:  
  - Overall target scope: all UTC / ITC / STC test cases defined in `BridgeU-TestPlan.md` for the BridgeU web platform (Features 1–3 and any shared services).  
  - Current execution scope in this version (v1.0): UTC‑01 … UTC‑13, ITC‑01 … ITC‑03, STC‑01 … STC‑06 for **Feature 1 – Daily Briefing System**.  
  - (Integration tests ITC‑01 … ITC‑03 for Feature 1 were also executed and all passed; execution followed the procedures in the Test Plan.)  

- **Overall Results**:  
  - **Total Test Cases (by ID)**: 13 unit test cases (UTC), 6 system test cases (STC) → **19** test case IDs.  
  - **Total Passed**: 22  
  - **Total Failed**: 0  
  - **Success Rate**: **100%**  

All tests for **Feature 1 – Daily Briefing System** passed as designed. The current implementation of the Daily Briefing feature is therefore considered to satisfy its documented requirements in the SRS and SDD. Future iterations of this Test Record will expand the execution and results sections to include **Feature 2 (Community Interaction)** and **Feature 3 (Authentication & Profile / Private Messaging)** once their corresponding tests are fully designed and executed.



