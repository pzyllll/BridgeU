BridgeU
Test Plan

Zhiyi Pan 652115558

Bachelor of Science
Software Engineering Program

Department College of Arts,Media,and
Technology
Chiang Mai University
January 2026
Project Advisor
Asst. Prof. Pattama Longani, Ph.D.


Chapter 1 Introduction
1.1 Purpose
The purpose of this Test Plan is to define a structured approach for testing the BridgeU system, including unit, integration, and system testing activities. It aims to detect potential defects as early as possible, ensure that all implemented features behave correctly under normal and exceptional conditions, and provide evidence that the system satisfies the requirements defined in the SRS and SDD.

1.2 Scope
The scope of this Test Plan covers all essential functions of the BridgeU platform using both white-box and black-box testing techniques:
- White-box testing focuses on **internal implementation**, including unit tests for Vue.js components, Spring Boot controllers, services, repositories, and utility classes (e.g., Daily Briefing pipeline, language detection, content moderation).  
- Black-box testing focuses on **externally visible behavior**, including integration tests for REST APIs and database interactions, and system tests that verify end-to-end user scenarios (such as viewing Daily Briefings, applying filters, searching, switching languages, and viewing news details).  


1.3 Acronyms and Definitions
Acronyms
URS	User Requirement Specification 
UTC	Unit Test Case
STC	System Test Case
TD	Test Data
SRS	Software Requirement Specification
SDD	Software Design Development 
API	Application Programming Interface
REST	Representational State Transfer
HTTP	 Hypertext Transfer Protocol
JSON	JavaScript Object Notation
JWT	JSON Web Token
RSS	Really Simple Syndication

Definition
Name	Definition
Test Plan	A document that describes the scope, approach, resources, and schedule of intended test activities. It identifies test items, the features to be tested, the testing tasks, who will do each task, and any risks requiring contingency planning.
Unit Test	 A test of individual programs or modules in order to remove design or programming errors. In the context of BridgeU, unit tests verify the functionality of individual methods in Vue.js components, Spring Boot controllers, services, and repositories.
System Testing	Testing conducted on a complete and integrated system to evaluate the system's compliance with its specified requirements. For BridgeU, system testing verifies the integration between frontend Vue.js components and backend Spring Boot API endpoints.
Test Case	 A set of test inputs, execution conditions, and expected results developed for a particular objective, such as to exercise a particular program path or to verify compliance with a specific requirement. Each test case in BridgeU includes Test ID, Test Function, Description, Input, and Expected Result.
Test Coverage	The degree to which a test suite exercises the code or requirements. In BridgeU test plan, coverage includes all implemented methods in the Daily Briefing System feature, including frontend component methods and backend API endpoints.
Daily Briefing	A news aggregation feature in BridgeU that automatically captures and summarizes Thai news from approved government and mainstream media sources via Google News RSS, providing bilingual (Chinese/English) access to daily news briefings for international students.
NewsBriefDTO
Pagination	Data Transfer Object used in BridgeU backend to transfer news summary data from the server to the frontend. It contains translated title and summary in the requested language (Chinese or English), excluding Thai content.
Bilingual Interface	A user interface that supports multiple languages. BridgeU Daily Briefing System supports both Chinese (zh) and English (en) interfaces, allowing users to switch between languages dynamically.
Date Filter	A filtering mechanism that allows users to filter news items by publication date range. In BridgeU, users can specify start date and end date to retrieve news published within a specific time period.
Keyword Search	 A search functionality that allows users to find news items containing specific keywords. In BridgeU, keyword search queries across multiple fields including title, summary, and translation fields (titleZh, summaryZh, titleEn, summaryEn).
Traceability	The ability to trace the history, application or location of an item or activity, or work products or activities, by means of recorded identification. In BridgeU test plan, traceability ensures that test cases can be traced back to specific requirements and use cases.
Pagination	A mechanism for dividing a large dataset into smaller, manageable pages. In BridgeU Daily Briefing System, pagination allows users to browse news items page by page, with configurable page size (default 10 items per page).


Chapter 2 Test Plan and Test Procedure
2.1 Test Objective
The objectives of testing the BridgeU system are:
1. To detect and fix all bugs and defects across all system features
2. To cover all functional, non-functional, and user interface-related requirements
3. To ensure that all API endpoints return correct responses according to the specifications
4. To validate that all user interactions (search, filter, pagination, etc.) work as expected
5. To ensure proper error handling and user feedback mechanisms throughout the system
6. To verify integration between frontend Vue.js components and backend Spring Boot API endpoints
7. To validate data consistency and integrity across all features
2.2 Scope of Testing
The scope of testing covers all BridgeU system essential functions using unit testing, integration testing, and system testing. 

2.3 Test Duration
The planned testing schedule for the BridgeU project is organised by feature and test level.  
Each feature follows the same 4‑week cycle (unit → integration → system), for a **total of 12 weeks** across all three features:
- **Weeks 1–4 – Feature 1: Daily Briefing System**  
  - Weeks 1–2: Design and execute unit tests (UTC) for frontend Daily Briefing components and backend news pipeline (controllers, services, repositories).  
  - Week 3: Design and execute integration tests (ITC) for `/api/news/daily-briefing` and `/api/news/daily-briefing/{id}`, verifying interaction with the `news` table.  
  - Week 4: Execute system tests (STC‑01 ~ STC‑06) for Feature 1, validating end‑to‑end user scenarios in the browser.  
- **Weeks 5–8 – Feature 2: Community Interaction Platform**  
  - Weeks 5–6: Unit tests for community posts, comments, likes, follows, and report‑related controllers/services.  
  - Week 7: Integration tests for community REST APIs and database tables (`posts`, `comments`, `post_likes`, `user_follows`, `reports`).  
  - Week 8: System tests for community UI flows (feed, posting, commenting, following, reporting, AI moderation).  
- **Weeks 9–12 – Feature 3: Auth & Profile / Private Messaging**  
  - Weeks 9–10: Unit tests for authentication, verification codes, profile management, conversations and messages.  
  - Week 11: Integration tests for auth/messaging APIs and their interaction with `users`, `verification_codes`, `conversations`, `messages`.  
  - Week 12: System tests for login/registration, profile update, mutual follow and private messaging flows.  
The schedule may be refined during the project, but all three features must complete their 4‑week test cycles before final acceptance.

2.4 Test Responsibility
Name	Responsibility
Unit Test	ZhiYiPan
Integration Test	ZhiYiPan
System Test	ZhiYiPan
Test Record	ZhiYiPan

2.5 Test Strategy
The BridgeU system testing follows a structured approach:
Test Planning:
Test case design based on use case descriptions and system requirements
Test data preparation and environment setup
Test schedule and resource allocation
Test Case Design:
Each test case includes Test ID, Test Function, Description, Input, and Expected Result
Test cases are designed based on use case descriptions and system requirements
Test cases cover both normal flows and exception flows
Test cases are organized by feature and test level (unit, system, integration)
Test Execution:
Unit tests are executed for individual methods and functions
System tests are executed for complete feature workflows
Integration tests verify component interactions
Test results are recorded with actual outputs and pass/fail status
Test Documentation:
All test cases are documented with clear descriptions and expected results
Test execution results are recorded with timestamps
Failed tests are logged with detailed error messages and steps to reproduce

In addition, the following testing methodologies are applied:
- **White-box testing** for unit tests of Vue.js components, Spring Boot controllers, services, repositories, and utility classes.  
- **Black-box testing** for integration and system tests, focusing on observable behaviour of REST APIs and UI workflows against requirements.  
- **Regression testing** is performed after major changes to ensure that previously implemented features continue to work as expected.  

2.6 Test Environment
The tests for BridgeU are executed in a controlled environment to ensure reproducible results.

- **Hardware**:  
  - Laptop-class machine with at least Intel i5 (or equivalent) CPU, 16 GB RAM, 512 GB SSD.  
  - Screen resolution 1920×1080 or higher for verifying responsive UI layouts.  
- **Operating System**:  
  - Windows 10 / Windows 11 64-bit.  
- **Backend Software Stack**:  
  - JDK 17 or later.  
  - Spring Boot backend (`global-buddy-backend`) running on port 8080.  
  - MySQL 8.x database instance with schema `bridgeu`.  
- **Frontend & Tools**:  
  - Node.js / npm for running the Vue.js frontend.  
  - Modern web browser (e.g., Google Chrome, Microsoft Edge) with Developer Tools (Network tab, Console).  
  - Postman or similar API client for integration testing of REST endpoints.  


2.7 Result of Testing

Test Execution:
Each test case is executed and produces an actual output
Test results are compared against expected results
Pass/Fail Criteria:
Pass: The actual result matches the expected result exactly
Fail: The actual result differs from the expected result, or an unexpected error occurs
Test Record:
All test results are documented with timestamps and execution details
Failed tests are logged with detailed error messages, stack traces, and steps to reproduce
Test coverage reports are generated to ensure all methods and use cases are tested
Test summary reports provide an overview of test execution status and coverage metrics









Chapter 3 Unit test
3.1 Feature 1
3.1.1 Unit Test Cases
3.1.1.1 UTC-01: Test fetchDailyBriefing()
Description: Tests the frontend component method for fetching daily briefing list, verifying it correctly handles pagination, language parameters, search keywords, date filters, and properly processes API responses and error cases.
Prerequisite: Frontend application is running and connected to the backend API; database contains at least 20 valid news records.
Test ID: UTC-01
Test Function: fetchDailyBriefing()
Test Cases: 
ID	Description	Input	Expected Result
1	Verify that fetchDailyBriefing() successfully retrieves news list with default parameters	{
  "currentPage": 1,
  "pageSize": 10,
  "currentLang": "en",
  "searchKeyword": "",
  "filterStartDate": null,
  "filterEndDate": null
}
	{
  "newsList": "array of news items",
  "pagination": {
    "totalElements": "> 0",
    "totalPages": ">= 1"
  },
  "loading": false,
  "error": null
}

2	Verify that fetchDailyBriefing() handles API error response correctly	{
  "apiResponse": {
    "success": false,
    "message": "Error message"
  }
}	{
  "error": "Error message",
  "loading": false,
  "newsList": "keeps previous value (not reset to empty array)"
}
3	Verify that fetchDailyBriefing() includes keyword parameter when searchKeyword is provided	{
  "searchKeyword": "Thailand"
}	{
  "apiRequest": {
    "params": {
      "keyword": "Thailand"
    }
  }
}
4	Verify that fetchDailyBriefing() includes date parameters when filters are provided	{
  "filterStartDate": "2026-01-01",
  "filterEndDate": "2026-01-31"
}	{
  "apiRequest": {
    "params": {
      "startDate": "2026-01-01",
      "endDate": "2026-01-31"
    }
  }
}
5	Verify that fetchDailyBriefing() includes language parameter	currentLang = 'zh'
	API request includes params.lang = "zh"

3.1.2 UTC-02: Test fetchNewsDetail()
Description:Tests the frontend component method for fetching news details, verifying it correctly retrieves news details based on news ID and language parameters, and handles 404 errors and network errors.
Prerequisite: Same as UTC-01, and at least one news record with a valid ID exists in the database.
Test ID: UTC-02
Test Function: fetchNewsDetail()
Test Cases:
ID	Description	Input	Expected Result
1	Verify that fetchNewsDetail() successfully retrieves news detail	{
  "newsId": 123,
  "currentLang": "en"
}	{
  "news": {
    "id": 123,
    "title": "not empty",
    "summary": "not empty"
  },
  "loading": false,
  "error": null
}
2	Verify that fetchNewsDetail() handles 404 error when news not found	{
  "apiResponse": {
    "status": 404
  }
}	{
  "error": "News not found (localized message)",
  "loading": false,
  "news": null
}
3	Verify that fetchNewsDetail() includes language parameter	{
  "currentLang": "zh"
}	{
  "apiRequest": {
    "params": {
      "lang": "zh"
    }
  }
}
4	Verify that fetchNewsDetail() sets originalContent when available and not Thai	{
  "apiResponse": {
    "originalContent": "English content"
  }
}	{
  "originalContent": "English content"
}
5	Verify that fetchNewsDetail() sets originalContent to null when content is Thai	{
  "apiResponse": {
    "content": "Thai content"
  }
}	{
  "originalContent": null
}


3.1.1.3 UTC-03: Test handleSearch()
Description:Tests the frontend component method for handling search operations, verifying it resets pagination, triggers search requests, and correctly handles clearing search keywords.
Prerequisite: Daily Briefing page is loaded successfully and `fetchDailyBriefing()` is available on the component instance.
Test ID: UTC-03
Test Function: handleSearch()
Test Cases:
ID	Description	Input	Expected Result
1	Verify that handleSearch() resets currentPage to 1	{
  "currentPage": 5,
  "searchKeyword": "Thailand"
}	{
  "currentPage": 1,
  "fetchDailyBriefingCalled": true
}
2	Verify that handleSearch() triggers fetchDailyBriefing()	{
  "searchKeyword": "test"
}	{
  "fetchDailyBriefingCalled": true,
  "searchKeyword": "test"
}

3.1.1.4 UTC-04: Test applyFilters()
Description:Tests the frontend component method for applying date filters, verifying it correctly sets start and end dates, resets pagination, and triggers data refetch.
Prerequisite: Daily Briefing page is loaded with date filter controls rendered and bound to component state.
Test ID: UTC-04
Test Function: applyFilters()
Test Cases:
ID	Description	Input	Expected Result
1	Verify that applyFilters() validates invalid date range	{
  "filterStartDate": "2026-01-31",
  "filterEndDate": "2026-01-01"
}	{
  "warningMessage": "displayed",
  "fetchDailyBriefingCalled": false
}
2	Verify that applyFilters() resets currentPage to 1 on valid date range	{
  "filterStartDate": "2026-01-01",
  "filterEndDate": "2026-01-31"
}	{
  "currentPage": 1,
  "fetchDailyBriefingCalled": true
}
3	Verify that applyFilters() works with only startDate	{
  "filterStartDate": "2026-01-01",
  "filterEndDate": null
}	{
  "fetchDailyBriefingCalled": true,
  "startDateParameter": "2026-01-01"
}
4	Verify that applyFilters() works with only endDate	{
  "filterStartDate": null,
  "filterEndDate": "2026-01-31"
}	{
  "fetchDailyBriefingCalled": true,
  "endDateParameter": "2026-01-31"
}

3.1.1.5 UTC-05: Test resetFilters()
Description:Tests the frontend component method for resetting filters, verifying it clears date filters and search keywords,resets pagination, and refetches all news data.
Prerequisite: Daily Briefing page is loaded and at least one filter (keyword or date) has been applied.
Test ID: UTC-05
Test Function: resetFilters()
Test Cases:
ID	Description	Input	Expected Result
1	Verify that resetFilters() clears all filter values	{
  "searchKeyword": "test",
  "filterStartDate": "2026-01-01",
  "filterEndDate": "2026-01-31",
  "currentPage": 5
}	{
  "searchKeyword": "",
  "filterStartDate": null,
  "filterEndDate": null,
  "currentPage": 1,
  "fetchDailyBriefingCalled": true
}


3.1.1.6 UTC-06: Test handlePageChange()
Description:Tests the frontend component method for handling page changes, verifying it correctly updates the current page number and refetches news data for the corresponding page.
Prerequisite: Daily Briefing list is visible with pagination controls rendered.
Test ID: UTC-06
Test Function: handlePageChange(page)
Test Cases:
ID	Description	Input	Expected Result
1	Verify that handlePageChange() updates currentPage	{
  "currentPage": 1,
  "page": 3
}	{
  "currentPage": 3,
  "fetchDailyBriefingCalled": true
}
2	Verify that handlePageChange() scrolls to top	{
  "page": 2
}	{
  "windowScrollToCalled": true,
  "scrollOptions": {
    "top": 0,
    "behavior": "smooth"
  }
}



3.1.1.7 UTC-07: Test normalizeDateValue() 
Description:Tests the utility method for normalizing date values, verifying it correctly handles various date input formats (strings, Date objects, null, etc.) and returns normalized date values.
Prerequisite: JavaScript date utilities and localization libraries used by the component are loaded (no external API dependency).
Test ID: UTC-07
Test Function: normalizeDateValue(value)
Test Cases:
ID	Description	Input	Expected Result
1	Verify that normalizeDateValue() converts Date object to yyyy-MM-dd	{
  "value": "2026-01-15"
}	{
  "returnValue": "2026-01-15"
}
2	Verify that normalizeDateValue() handles yyyy-MM-dd string	{
  "value": "2026-01-15"
}	{
  "returnValue": "2026-01-15"
}
3	Verify that normalizeDateValue() handles ISO date string	{
  "value": "2026-01-15T00:00:00Z"
}	{
  "returnValue": "2026-01-15"
}
4	Verify that normalizeDateValue() returns null for null input	{
  "value": null
}	{
  "returnValue": null
}
5	Verify that normalizeDateValue() handles invalid string containing letters	{
  "value": "invalid-date"
}	{
  "returnValue": null    }

3.1.1.8 UTC-08: Test formatDate()
Description: Tests the utility method for formatting dates, verifying it correctly formats date displays based on the current interface language (Chinese or English) and handles various date input formats.
Prerequisite: Component or utility module has access to the current interface language setting (`currentLang`).
Test ID: UTC-08
Test Function: formatDate(date)
Test Cases:
ID	Description	Input	Expected Result
1	Verify that formatDate() formats date correctly	{
  "date": "2026-01-15T14:30:00"
}	{
  "returnValue": "15-01-2026 14:30"
}
2	Verify that formatDate() handles null input	{
  "date": null
}	{
  "returnValue": ""
}
3	Verify that formatDate() handles invalid date	{
  "date": "invalid"
}	{
  "returnValue": ""
}
4	Verify that formatDate() pads single digit months and days	{
  "date": "2026-01-05T09:05:00"
}	{
  "returnValue": "05-01-2026 09:05"
}

3.1.1.9 UTC-09: Test convertToDTO()
Description:Tests the backend service method for converting news entities to Data Transfer Objects (DTOs), verifying it correctly selects Chinese or English versions of titles and summaries based on the language parameter.
Prerequisite: News entity objects are instantiated in memory with various combinations of original and translated fields; no database access is required.
Test ID: UTC-09
Test Function: convertToDTO(news, lang)
Test Cases:

ID	Description	Input	Expected Result
1	Verify that convertToDTO() uses Chinese translation when lang='zh'	{
  "news": {
    "titleZh": "中文标题",
    "summaryZh": "中文摘要"
  },
  "lang": "zh"
}	{
  "dto": {
    "title": "中文标题",
    "summary": "中文摘要"
  }
}
2	 Verify that convertToDTO() uses English translation when lang='en'	{
  "news": {
    "titleEn": "English Title",
    "summaryEn": "English Summary"
  },
  "lang": "en"
}	{
  "dto": {
    "title": "English Title",
    "summary": "English Summary"
  }
}
3	Verify that convertToDTO() never shows Thai content	{
  "news": {
    "title": "contains Thai characters",
    "titleZh": null
  },
  "lang": "zh"
}	{
  "dto": {
    "title": "does not contain Thai characters",
    "titleType": "either English translation or placeholder"
  }
}
4	Verify that convertToDTO() falls back to English when Chinese translation unavailable	{
  "news": {
    "titleZh": null,
    "titleEn": "English Title",
    "title": "not Thai"
  },
  "lang": "zh"
}	{
  "dto": {
    "title": "English Title"
  }
}
5	 Verify that convertToDTO() sets placeholder when no translation available for Thai content	{
  "news": {
    "title": "contains Thai",
    "titleZh": null,
    "titleEn": null
  },
  "lang": "zh"
}	{
  "dto": {
    "title": "[新闻标题翻译中...]"
  }
}

3.1.1.10 UTC-10: Test getDailyBriefing() – Backend
Description:Tests the backend controller API method for retrieving daily briefing list, verifying it correctly handles pagination, language, date filtering, keyword search parameters, and returns appropriate response data.
Prerequisite: Spring Boot backend is running with access to the `news` table in the MySQL `bridgeu` database; `NewsRepository` is correctly wired.
Test ID: UTC-10
Test Function: getDailyBriefing()
Test Cases:
ID	Description	Input	Expected Result
1	Verify that getDailyBriefing() returns paginated news list with default parameters	{
  "page": 0,
  "size": 10,
  "lang": "en",
  "startDate": null,
  "endDate": null,
  "keyword": null
}	{
  "success": true,
  "data": "array of NewsBriefDTO",
  "pagination": {
    "totalElements": ">= 0",
    "totalPages": ">= 0"
  },
  "note": "When no filters are provided (no date, no keyword), the method returns ALL news items with pagination (no default date window applied)"
}
2	Verify that getDailyBriefing() filters by keyword	{
  "keyword": "Thailand"
}	{
  "response": "contains only news items matching keyword in title, summary, or translations"
}
3	Verify that getDailyBriefing() filters by date range	{
  "startDate": "2026-01-01",
  "endDate": "2026-01-31"
}	{
  "response": "contains only news items with publishDate between startDate and endDate"
}
4	Verify that getDailyBriefing() combines multiple filters	{
  "keyword": "Thailand",
  "startDate": "2026-01-01",
  "endDate": "2026-01-31"
}	{
  "response": "contains news items matching all filter criteria"
}
5	Verify that getDailyBriefing() handles invalid date format	{
  "startDate": "invalid-date"
}	{
  "dateFilter": "ignored",
  "result": "all news returned"
}
6	Verify that getDailyBriefing() returns error response on exception	{
  "error": "Database connection error"
}	{
  "success": false,
  "message": "contains error details"
}

3.1.1.11 UTC-11: Test getNewsDetail() – Backend
Description:Tests the backend controller API method for retrieving news details, verifying it correctly returns news details based on news ID and language parameters, and handles cases where news does not exist.
Prerequisite: Backend is running and the `news` table contains at least one valid record and one non-existent ID to test 404 behaviour.
Test ID: UTC-11
Test Function: getNewsDetail()
Test Cases:

ID	Description	Input	Expected Result
1	Verify that getNewsDetail() returns news detail for valid ID	{
  "id": 123,
  "lang": "en"
}	{
  "success": true,
  "data": {
    "id": 123,
    "title": "not empty",
    "summary": "not empty"
  }
}
2	Verify that getNewsDetail() returns 404 for non-existent ID	{
  "id": 99999,
  "lang": "en"
}	{
  "status": 404,
  "success": false,
  "message": "News not found"
}
3	Verify that getNewsDetail() returns Chinese translation when lang='zh'	{
  "id": 123,
  "lang": "zh",
  "news": {
    "titleZh": "exists"
  }
}	{
  "data": {
    "title": "news.titleZh",
    "summary": "news.summaryZh"
  }
}
4	Verify that getNewsDetail() excludes Thai originalContent	{
  "news": {
    "originalContent": "contains Thai characters"
  }
}	{
  "originalContent": null
}
5	Verify that getNewsDetail() includes non-Thai originalContent	{
  "news": {
    "originalContent": "English content"
  }
}	{
  "originalContent": "English content"
}
6	Verify that getNewsDetail() returns error response on exception	{
  "error": "Database error"
}	{
  "status": 500,
  "success": false,
  "message": "contains error details"
}
3.1.1.12 UTC-12: Test findByKeyword()
Description: Tests the backend repository method for searching news by keyword, verifying it performs fuzzy matching searches across multiple fields (titles, summaries, Chinese and English versions) and supports pagination.
Prerequisite: `NewsRepository` is available in a Spring test context with test data inserted into the `news` table.
Test ID: UTC-12
Test Function: findByKeyword(keyword, pageable)
Test Cases:

ID	Description	Input	Expected Result
1	Verify that findByKeyword() searches in title field	{
  "keyword": "Thailand",
  "news": {
    "title": "contains 'Thailand'"
  }
}	{
  "returnValue": "Page containing news items with 'Thailand' in title"
}
2	Verify that findByKeyword() searches in summary field	{
  "keyword": "economy",
  "news": {
    "summary": "contains 'economy'"
  }
}	{
  "returnValue": "Page containing news items with 'economy' in summary"
}
3	Verify that findByKeyword() searches in translation fields	{
  "keyword": "中文",
  "news": {
    "titleZh": "contains '中文'"
  }
}	{
  "returnValue": "Page containing news items with '中文' in titleZh"
}
4	Verify that findByKeyword() is case-insensitive	{
  "keyword": "THAILAND",
  "news": {
    "title": "Thailand"
  }
}	{
  "returnValue": "Page containing matching news items"
}
5	Verify that findByKeyword() returns empty page when no matches	{
  "keyword": "nonexistentkeyword12345"
}	{
  "returnValue": "empty Page with totalElements = 0"
}
6	Verify that findByKeyword() respects pagination	{
  "keyword": "Thailand",
  "pageable": {
    "page": 0,
    "size": 10
  }}	{
  "returnValue": "Page with size <= 10"
}

3.1.1.13 UTC-13: Test findByPublishDateBetweenOrdered()
Description:Tests the backend repository method for querying news by publication date range, verifying it correctly filters news within the specified date range, orders by publication date in descending order, and supports pagination.
Prerequisite: `NewsRepository` is available in a Spring test context with multiple news records covering different publish dates for the target range.
Test ID: UTC-13
Test Function: findByPublishDateBetweenOrdered(startDate, endDate, pageable)`
Test Cases:
ID	Description	Input	Expected Result
1	Verify that findByPublishDateBetweenOrdered() filters by date range	{
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "news": {
    "publishDate": "in range"
  }
}	{
  "returnValue": "Page containing only news items with publishDate between startDate and endDate"
}
2	Verify that findByPublishDateBetweenOrdered() orders by publishDate DESC	{
  "newsItems": "multiple in date range"
}	{
  "order": "from newest to oldest by publishDate"
}
3	Verify that findByPublishDateBetweenOrdered() includes boundary dates	{
  "startDate": "2026-01-15T00:00:00",
  "endDate": "2026-01-15T23:59:59",
  "news": {
    "publishDate": "2026-01-15"
  }
}	{
  "returnValue": "Page containing news item with publishDate = 2026-01-15"
}
4	Verify that findByPublishDateBetweenOrdered() returns empty page when no matches	{
  "startDate": "2026-12-01",
  "endDate": "2026-12-31",
  "news": "none in this range"
}	{
  "returnValue": "empty Page with totalElements = 0"
}
5	Verify that findByPublishDateBetweenOrdered() respects pagination	{
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "pageable": {
    "page": 0,
    "size": 10
  }
}	{
  "returnValue": "Page with size <= 10"
}







                        
                        
                        
                        
                        
                        
                        
                        
                        
                        
                        
3.2 Feature 1 – Integration Test (Daily Briefing System)

3.2.1 ITC-01: GET /api/news/daily-briefing with Database Filters

**Description**: This integration test verifies that `NewsController.getDailyBriefing()` correctly integrates with `NewsRepository` and the database, handling pagination, date range, keyword, and source filters according to the implemented logic.

**Prepared Data**:  
- Spring Boot backend running with a real MySQL `bridgeu` database.  
- `news` table contains at least 30 rows with different `publish_date`, `source`, `title`, `summary`, and translation fields (`titleEn`, `titleZh`, `summaryEn`, `summaryZh`).  

**Test Script**:  
1. Send `GET /api/news/daily-briefing?page=0&size=10&lang=en` using Postman or an automated integration test.  
2. Verify that the response status is **200 OK** and the JSON body has `success = true`, `data` (array), and `pagination` object.  
3. Run `SELECT COUNT(*) FROM news;` in the database and verify that `pagination.totalElements` equals this count and `data.length <= 10`.  
4. Send `GET /api/news/daily-briefing?page=0&size=10&lang=en&startDate=2026-01-01&endDate=2026-01-31`.  
5. Verify that all returned items have `publishDate` between `2026-01-01 00:00:00` and `2026-01-31 23:59:59`, and that the number of items matches the SQL query for this range.  
6. Send `GET /api/news/daily-briefing?...&keyword=Thailand`.  
7. Confirm that all returned news rows contain “Thailand” (case-insensitive) in at least one of: `title`, `summary`, `titleEn`, `summaryEn`, `titleZh`, or `summaryZh`.  
8. Send `GET /api/news/daily-briefing?...&keyword=Thailand&source=Bangkok%20Post`.  
9. Verify that every result matches **both** the keyword and the source, and that no unexpected errors occur.  

---

3.2.2 ITC-02: GET /api/news/daily-briefing/{id} and DTO Conversion

**Description**: This integration test verifies that `NewsController.getNewsDetail()` and `convertToDTO()` integrate correctly with `NewsRepository`, `LanguageDetectionService`, and the `news` table, including language selection and Thai-content hiding rules.

**Prepared Data**:  
- At least three `news` records:  
  - Record A: has both `titleEn`/`summaryEn` and `titleZh`/`summaryZh`.  
  - Record B: has only English translations (`titleEn`/`summaryEn`), with non-Thai original text.  
  - Record C: `originalContent` is primarily Thai, with missing or partial translations.  

**Test Script**:  
1. For record A, call `GET /api/news/daily-briefing/{idA}?lang=zh`.  
2. Verify response `success = true` and that `data.title` / `data.summary` equal `titleZh` / `summaryZh`.  
3. Call `GET /api/news/daily-briefing/{idA}?lang=en` and verify that `data.title` / `data.summary` equal `titleEn` / `summaryEn`.  
4. For record B, call `GET /api/news/daily-briefing/{idB}?lang=zh` and verify that the DTO falls back to English translations (no Thai characters in `data.title`).  
5. For record C, confirm in DB that `original_content` is Thai. Call `GET /api/news/daily-briefing/{idC}?lang=en`.  
6. Verify that the response has `success = true`, `data` filled, and `originalContent = null`, confirming integration with `LanguageDetectionService.hasAnyThai()` and Thai-content hiding.  
7. Call `GET /api/news/daily-briefing/999999?lang=en` (non-existent ID) and verify that the API returns HTTP 404 with `success = false` and an appropriate error message.  

---

3.2.3 ITC-03: Scheduled Crawl & Store Pipeline

**Description**: This integration test verifies that the scheduled crawl pipeline integrates `NewsScheduler`, `NewsCrawlerService`, `NewsRelevanceService`, `AiSummaryService`, `TranslationService`, `LanguageDetectionService`, and `NewsRepository` to insert relevant, non-duplicate news into the database.

**Prepared Data**:  
- Backend running in a test profile with stubbed external dependencies:  
  - `NewsCrawlerService` returns a fixed list of raw news items.  
  - AI services (`AiSummaryService`, `TranslationService`) return deterministic summaries and translations.  
- `news` table initially empty or with a known record count **N**.  

**Test Script**:  
1. Trigger `NewsScheduler.scheduledCrawlAndSummarize()` once (either by waiting for the cron time in a test environment or by calling the method directly in an integration test).  
2. Verify that the crawler stub is invoked and returns the configured raw news list.  
3. After the job finishes, query the database and confirm that the number of rows in `news` has increased by the expected number of **relevant** (non-filtered) items, and no duplicates were added for identical `originalUrl` values.  
4. Inspect one newly inserted row and verify that `titleEn`/`titleZh`, `summaryEn`/`summaryZh`, `originalUrl`, `source`, `publishDate`, and `createTime` are populated from the crawler and AI stubs as designed.  
5. Run the scheduler again with the same stub data and confirm that the record count in `news` does **not** increase (deduplication via `findByOriginalUrl` works correctly).  

---

Chapter 4 System test

4.1 Progress I – Feature 1: Daily Briefing System

4.1.1 STC-01: View Daily Briefing List (Default)

**Description**: This system test verifies that the integrated frontend and backend correctly display the Daily Briefing list using default parameters, including pagination and basic data fields, and that no Thai text appears in the UI.

**Prepared Data**:  
- Backend Spring Boot application running on port 8080.  
- Frontend Vue application running and configured to proxy `/api` to backend.  
- Database contains at least 20 valid news records with non-Thai `titleEn` / `summaryEn` or `titleZh` / `summaryZh` (some records may still have Thai in original data, which must not be shown on UI).  

**Test script**:  
1. Open the BridgeU website in a web browser and navigate to the **Daily Briefing** page.  
2. Observe that no search keyword, date filter, or source filter is selected (default state).  
3. Verify that the page displays a list of **10 news items** with titles, summaries, sources, publish dates, and actions (such as “View Detail” and “Open Original”).  
4. Verify that the pagination bar shows at least 2 pages when the total number of news records is greater than 10.  
5. Open the browser **Network** tab and confirm that the frontend sends a `GET /api/news/daily-briefing?page=0&size=10&lang=en` request without `keyword`, `startDate`, `endDate`, or `source` parameters.  
6. Check the API response body and confirm it contains `{ "success": true, "data": [...], "pagination": {...} }`.  
7. Scroll through all news items on the first page and verify that all visible titles and summaries are in **Chinese or English only**, with **no Thai characters** displayed.  

**Test Case (table format)**:  

| No | Description | Input | Expected Result |
|----|-------------|-------|-----------------|
| 1 | Open Daily Briefing page with default state | Open BridgeU in a browser and navigate to “Daily Briefing” page with no filters applied. | Page shows 10 news cards with title, summary, source, publish date and actions; no search keyword, date or source filter is selected. |
| 2 | Verify backend request parameters | Observe Network tab for list request. | Request is `GET /api/news/daily-briefing?page=0&size=10&lang=en` with **no** `keyword`, `startDate`, `endDate` or `source` parameters. |
| 3 | Verify pagination behaviour | Scroll to bottom of list. | Pagination bar shows at least 2 pages when total news records > 10. |
| 4 | Check UI language rule | Inspect all items on first page. | All visible titles and summaries are in Chinese or English only; **no Thai characters** appear anywhere in the list. |

---

4.1.2 STC-02: Filter News by Date Range

**Description**: This system test verifies that the system correctly filters Daily Briefing news items by publish date range across frontend and backend.

**Prepared Data**:  
- Same environment as STC-01.  
- Database contains news items whose `publishDate` covers at least the last 30 days.  

**Test script**:  
1. Open the Daily Briefing page in the browser.  
2. In the date filter controls, set **start date** to `2026-01-01` and **end date** to `2026-01-07`.  
3. Click the **Apply Filters** button.  
4. In the Network tab, confirm that the frontend sends `GET /api/news/daily-briefing?...&startDate=2026-01-01&endDate=2026-01-07`.  
5. Verify that the returned list only contains news whose `publishDate` is between `2026-01-01 00:00:00` and `2026-01-07 23:59:59`, and that `pagination.totalElements` matches the number of records in the database for that period.  
6. Clear all filters, then set **start date** to `2026-01-10`, leave **end date** empty, and click **Apply Filters**.  
7. Confirm via Network tab that the request contains `startDate=2026-01-10` and no `endDate`, and that the backend returns news from `2026-01-10` up to **today** only, without server error.  
8. Clear filters again, set **end date** to `2026-01-31`, leave **start date** empty, and click **Apply Filters**.  
9. Confirm that the backend interprets the start date as **30 days before** `2026-01-31` and that the results only contain news in this computed range.  
10. Finally, set **start date** to `2026-02-10` and **end date** to `2026-02-01` (startDate > endDate) and click **Apply Filters**.  
11. Verify that the frontend shows a validation warning that the start date cannot be later than the end date, **no API request** is sent, and the list content remains unchanged.  

**Test Case (table format)**:  

| No | Description | Input | Expected Result |
|----|-------------|-------|-----------------|
| 1 | Filter list by fixed date range | Set start date `01‑01‑2026`, end date `07‑01‑2026` and click “Apply Filters”. | Network request is `GET /api/news/daily-briefing?...&startDate=2026-01-01&endDate=2026-01-07`; returned list only contains news with `publishDate` between these timestamps and `pagination.totalElements` equals DB count for that period. |
| 2 | Filter with only start date | Clear filters, set start date `10‑01‑2026`, leave end date empty, click “Apply Filters”. | Request contains `startDate=2026-01-10` and **no** `endDate`; backend returns news from `2026‑01‑10` up to today only, without error. |
| 3 | Filter with only end date and inferred start date | Clear filters, set end date `20‑01‑2026`, leave start date empty, click “Apply Filters”. | Backend infers start date as 30 days before `20-01-2026`; results only include news in this computed range. |
| 4 | Validate invalid date range | Set start date `10‑02‑2026`, end date `01‑02‑2026`, click “Apply Filters”. | Frontend shows validation warning (start date cannot be later than end date), **no API request** is sent and list content remains unchanged. |

---

4.1.3 STC-03: Search Daily Briefing by Keyword

**Description**: This system test verifies that keyword search works end-to-end, including parameter passing and search scope.

**Prepared Data**:  
- Same environment as STC-01.  
- Database contains news items whose `title`, `summary`, or translation fields (`titleZh`, `titleEn`, `summaryZh`, `summaryEn`) contain the keyword **“Thailand”**.  

**Test script**:  
1. Open the Daily Briefing page in the browser.  
2. In the search box, enter the keyword **“Thailand”** and click the **Search** button.  
3. Verify that the UI resets the current page to **1** (first page).  
4. In the Network tab, confirm that the frontend sends `GET /api/news/daily-briefing?...&keyword=Thailand`.  
5. Check that the returned list only contains items whose `title`, `originalContent`, `summary`, or any translation field includes “thailand” (case-insensitive), and that `pagination.totalElements ≥ 1`.  
6. Clear the search box so that it is empty and click **Search** again.  
7. Confirm that the new request does **not** contain the `keyword` parameter and that the list shows unfiltered results (respecting only any active date/source filters).  
8. Enter a different keyword such as **“student”**, set a valid date range, and click **Search**.  
9. Verify that the API request includes both `keyword` and `startDate`/`endDate`, and that results only include items matching **both** the keyword and the date range.  

**Test Case (table format)**:  

| No | Description | Input | Expected Result |
|----|-------------|-------|-----------------|
| 1 | Search by keyword | Enter `Thailand` in search box and click “Search”. | UI resets current page to 1; request is `GET /api/news/daily-briefing?...&keyword=Thailand`; returned list only contains news whose title, originalContent, summary or any translation field includes “thailand” (case‑insensitive). |
| 2 | Clear keyword search | Clear search box and click “Search” again. | New request has no `keyword` parameter; list shows unfiltered results (respecting only any active date/source filters). |
| 3 | Combine keyword and date filters | Enter `student`, set a valid date range, click “Search”. | Request includes both `keyword` and `startDate`/`endDate`; results only include items matching both the keyword and the date range. |

---

4.1.4 STC-04: Switch Interface Language

**Description**: This system test verifies that switching between English and Chinese interfaces correctly reloads the Daily Briefing list using appropriate translations while still hiding Thai content.

**Prepared Data**:  
- Same environment as STC-01.  
- Database contains a dedicated test set of news records where **every record has both English and Chinese translations** (`titleEn`/`summaryEn` and `titleZh`/`summaryZh`).
**Test script**:  
1. Ensure the interface language is set to **English (en)**, then open the Daily Briefing list.  
2. Observe a few news items and note their titles and summaries in English.  
3. Use the language toggle in the UI to switch the interface language to **Chinese (zh)**.  
4. In the Network tab, verify that the frontend re-sends `GET /api/news/daily-briefing` with `lang=zh`.  
5. Confirm that items with `titleZh`/`summaryZh` now display Chinese text; items without Chinese translations show English or placeholder texts `[新闻标题翻译中...]` / `[新闻内容翻译中...]` according to `convertToDTO` rules; no Thai characters appear.  
6. Switch the interface language back to **English (en)**.  
7. Verify that the frontend re-sends `GET /api/news/daily-briefing` with `lang=en` and that titles/summaries are displayed in English when available, otherwise in Chinese or placeholder texts; still no Thai characters appear.  
8. For a news item whose original title/summary is Thai and has no translations, switch between **zh** and **en** and confirm that only placeholder texts are shown, and the original Thai text is never displayed.  

**Test Case (table format)**:  

| No | Description | Input | Expected Result |
|----|-------------|-------|-----------------|
| 1 | Load list in English | Set interface language to `en`, open Daily Briefing list. | News list shows titles/summaries in English when available; others fall back to Chinese or placeholder texts; no Thai characters displayed. |
| 2 | Switch to Chinese | Use language toggle to switch to `zh`. | Network request is resent with `lang=zh`; items with `titleZh`/`summaryZh` display Chinese text; items without Chinese translations show English or placeholder `[新闻标题翻译中...]` / `[新闻内容翻译中...]`; still no Thai characters. |
| 3 | Switch back to English | Switch language back to `en`. | Request is resent with `lang=en`; list displays English titles/summaries when available, otherwise Chinese or placeholders; no Thai characters. |
| 4 | Thai‑only records | Select a news item whose original title/summary is Thai and has no translations, toggle between `zh` and `en`. | Only placeholder texts are shown in all languages; original Thai text is never displayed. |

---

4.1.5 STC-05: View News Detail

**Description**: This system test verifies that selecting a news item from the list opens the detail view and correctly retrieves full information from the backend, including rules for `originalContent`.

**Prepared Data**:  
- Same environment as STC-01.  
- Database contains at least one news record with **non-Thai** `originalContent` and at least one record whose `originalContent` is primarily **Thai**.  

**Test script**:  
1. Open the Daily Briefing list and locate a news item with ID = **N** (non-Thai `originalContent`).  
2. Click the **“View Detail”** action for this item.  
3. In the Network tab, confirm that the frontend sends `GET /api/news/daily-briefing/N?lang=<currentLang>`.  
4. Verify that the backend response is `{ success: true, data: NewsBriefDTO, originalContent: "..." }` and that the detail page shows title, summary, source, publish date, and the **original content** section.  
5. Return to the list and choose another item whose `originalContent` in the database is primarily Thai.  
6. Click **“View Detail”** for this Thai-content item and verify that the response still has `success = true` but `originalContent = null`.  
7. Confirm that the detail page displays only translated title/summary (or placeholders) and **does not** show any Thai original text.  
8. Manually modify the browser URL to `/daily-briefing/999999` (an ID that does not exist) and press Enter.  
9. Confirm that the backend returns HTTP 404 with `success = false` and message `"News not found with id: 999999"`, and that the frontend shows a localized “News not found” message with a way to navigate back to the list, without any uncaught error.  

**Test Case (table format)**:  

| No | Description | Input | Expected Result |
|----|-------------|-------|-----------------|
| 1 | Open detail for non‑Thai content | From list, click “View Detail” on news item N whose `originalContent` is non‑Thai. | Network sends `GET /api/news/daily-briefing/N?lang=<currentLang>`; response `{ success: true, data: NewsBriefDTO, originalContent: "..." }`; detail page shows title, summary, source, publish date and original content section. |
| 2 | Open detail for Thai content | From list, click “View Detail” on news item T whose `originalContent` is primarily Thai. | Backend returns `success = true` with `originalContent = null`; detail page only shows translated title/summary or placeholders and **no Thai original text**. |
| 3 | Handle non‑existent ID | Manually open `/daily-briefing/999999` in browser. | Backend returns HTTP 404 with `success = false` and message `"News not found with id: 999999"`; frontend shows localized “News not found” message with navigation back to list and no uncaught error. |

---

4.1.6 STC-06: Open Original News Link

**Description**: This system test verifies that the **“Open Original”** action correctly opens the original news article URL from backend data, both from the list and from the detail view.

**Prepared Data**:  
- Same environment as STC-01.  
- Selected news items have a valid `originalUrl` field in the database.  

**Test script**:  
1. Open the Daily Briefing list and find a news item that has a visible **“Open Original”** action.  
2. Click **“Open Original”** on this item.  
3. Verify that the browser opens a **new tab** pointing to the `originalUrl` value from the backend and that the BridgeU tab remains open.  
4. Confirm in the Network tab that no additional API call other than the initial list request is required to open the original link.  
5. From the Daily Briefing list, click **“View Detail”** on the same news item to open the detail page.  
6. On the detail page, click **“Open Original”** again.  
7. Verify that a new browser tab opens with the **same URL** as in step 3 and that this URL matches the `originalUrl` field returned by the backend for that news item.  

**Test Case (table format)**:  

| No | Description | Input | Expected Result |
|----|-------------|-------|-----------------|
| 1 | Open original link from list | On Daily Briefing list, click “Open Original” for a news item with valid `originalUrl`. | Browser opens a **new tab** with the URL equal to the `originalUrl` from backend; existing BridgeU tab stays open; Network tab shows no extra API calls beyond the list request. |
| 2 | Open original link from detail | From the same item, click “View Detail” then “Open Original” on detail page. | New tab opens with the **same URL** as in test 1; this URL matches the `originalUrl` value returned by the backend for that news item. |
