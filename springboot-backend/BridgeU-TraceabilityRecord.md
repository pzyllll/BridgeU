## BridgeU – Traceability Record Table

> This document links user requirements (URS), system requirements (SRS), design artefacts (use cases, diagrams, UI)
> and test cases (UTC/ITC/STC) for each feature of the BridgeU system.

---

### Feature 1 – Daily Briefing System

Traceability for Feature 1 is broken down **per user requirement**, following the style of your Word template
(each row corresponds to one URS and links to SRS, design artefacts and tests).

| Feature | User Requirement (URS) | System Requirement (SRS) | Use Case (UC) | Activity Diagram (AD) | Sequence Diagram (SD) | User Interface (UI) | Unit Test (UTC) | Integration Test (ITC) | System Test (STC) |
|---------|------------------------|--------------------------|---------------|-----------------------|-----------------------|---------------------|-----------------|-------------------------|--------------------|
| Feature 1 – Daily Briefing System | URS‑01 | SRS‑01 – SRS‑07 (basic Daily Briefing list, pagination, hide Thai content, open original link) | UC‑01: Find Daily Briefing | AD‑01 | SD‑01 (View Daily Briefings) | UI‑01 (Daily Briefing List) | UTC‑01, UTC‑03, UTC‑04, UTC‑05, UTC‑06, UTC‑07, UTC‑08, UTC‑10 | ITC‑01 (`GET /api/news/daily-briefing`) | STC‑01, STC‑02, STC‑03, STC‑06 |
| Feature 1 – Daily Briefing System | URS‑02 | SRS‑08 – SRS‑14 (view news detail, show/hide originalContent, error handling for missing ID) | UC‑02: View News Detail | AD‑02 | SD‑02 (View Daily Briefing News Details) | UI‑02 (News Detail) | UTC‑02, UTC‑11 | ITC‑02 (`GET /api/news/daily-briefing/{id}` + DTO conversion) | STC‑05 |
| Feature 1 – Daily Briefing System | URS‑03 | SRS‑15 – SRS‑21 (keyword search, date range filter, source filter, language switch) | UC‑01 / UC‑02 (with filters & language) | AD‑01, AD‑02 | SD‑05 (Filter by Date), SD‑06 (Search), SD‑03 (Switch Language) | UI‑01, UI‑02 | UTC‑01, UTC‑03, UTC‑04, UTC‑05, UTC‑07, UTC‑10, UTC‑12, UTC‑13 | ITC‑01 (`/daily-briefing` with filters) | STC‑02, STC‑03, STC‑04 |
| Feature 1 – Daily Briefing System | URS‑04 | SRS‑22 – SRS‑26 (scheduled crawl, relevance filter, AI summary & translation, deduplication) | (internal pipeline supporting UC‑01/UC‑02) | AD‑01 (extended with scheduler) | SD‑07 (Scheduled Crawl & Summarize) | (no direct UI; visible via updated list/detail) | UTC‑09, UTC‑12, UTC‑13 | ITC‑03 (`NewsScheduler.scheduledCrawlAndSummarize`) | STC‑01 (list reflects up‑to‑date crawled news) |
| Feature 1 – Daily Briefing System | URS‑05 | SRS‑22 – SRS‑26 (open original news in a new browser tab via “Read Original” button and `openOriginalUrl(url)` logic) | UC‑04: Jump to Original Link | AD‑04 | SD‑04 (Jump to Original Link) | UI‑01 (Daily Briefing List), UI‑02 (News Detail) | (covered implicitly by frontend component unit tests) | ITC‑01, ITC‑02 (reuse same data returned for list/detail) | STC‑01, STC‑06 |
| Feature 1 – Daily Briefing System | URS‑06 | SRS‑27 – SRS‑32 (filter Daily Briefing news by start/end date, validate ranges, query DB, and show paginated results) | UC‑05: Filter Daily Briefing News by Date | AD‑05 | SD‑05 (Filter by Date) | UI‑01 (Daily Briefing List) | UTC‑04, UTC‑05, UTC‑07, UTC‑08, UTC‑10, UTC‑13 | ITC‑01 (`/api/news/daily-briefing` with date filters) | STC‑02 |

---

### Feature 2 – Community Interaction Platform

> Note: Exact URS/SRS/UC IDs should match the numbering in `BridgeU-SRS.md`. Below is a consolidated mapping
> based on the current SRS/SDD/TestPlan structure.

| Aspect | IDs / Artefacts |
|--------|------------------|
| **User Requirements (URS)** | URS‑08 … URS‑21 – requirements covering viewing community feed, creating/editing posts, commenting, liking, following/unfollowing, reporting content, and viewing AI moderation results. |
| **System Requirements (SRS)** | SRS‑08 … SRS‑21 – community and interaction requirements (bilingual posts/comments, tag/category filters, AI translation, AI comment summary, reporting, moderation workflow, mutual follow list). |
| **Use Cases (UC)** | UC‑03: View Community Feed; UC‑04: Create / Publish Post; UC‑05: Like Post; UC‑06: Filter by Tag; UC‑07: View Post Details; UC‑08: Report Post/Comment; UC‑09: Post Comment; UC‑10: Search Posts; UC‑11: Communicate with Others; UC‑12: Manage Message Status; UC‑13: View and Manage Mutual Follow List; UC‑14: AI Summary of Comments. |
| **Activity / Sequence Diagrams (AD / SD)** | AD‑03 … AD‑06 – community activities (browse feed, create post, comment/like, report & moderation, private messaging). SD‑07~SD‑18 in SRS/SDD (view feed, create post, like, filter by tag, view details, report, comment, search posts, communicate, manage message status, mutual follow list, AI comment summary). |
| **User Interface (UI)** | UI‑03: Community Feed page; UI‑04: Post Detail page; UI‑05: New Post / Edit Post form; UI‑06: Mutual Follow list; UI‑07: Report dialog / AI summary panel. |
| **Unit Tests (UTC)** | UTC‑2x … UTC‑3x – Vue components for community feed and post details; backend `CommunityController`, `PostController`, `ReportController`, `ContentModerationService`, `TranslationService`, `LanguageDetectionService`, repositories for `CommunityPost`, `Comment`, `PostLike`, `UserFollow`, `Report`. *(IDs should be filled to match your actual UTC numbering.)* |
| **Integration Tests (ITC)** | ITC‑04: `/api/posts` list (feed) with language and scoring; ITC‑05: `/api/communities/{id}/posts` + create post with auto‑translation; ITC‑06: comment & like APIs (`/api/posts/{id}/comments`, `/api/posts/{id}/likes`); ITC‑07: follow/unfollow APIs (`/api/users/{id}/follow` etc.); ITC‑08: report & moderation pipeline (`/api/reports`, `ContentModerationService`). |
| **System Tests (STC)** | STC‑1x … – end‑to‑end scenarios: view community feed, create bilingual post, comment and like, follow/unfollow user, view mutual follow list, report abusive content, review AI moderation result, see post acceptance/rejection and reasons. |

---

### Feature 3 – Authentication, Profile & Private Messaging

| Aspect | IDs / Artefacts |
|--------|------------------|
| **User Requirements (URS)** | URS‑22 … URS‑30 – account registration via email/phone, login/logout, password reset via verification code, profile management (display name, avatar, preferred language), follow relationships, one‑to‑one messaging between users. |
| **System Requirements (SRS)** | SRS‑22 … SRS‑39 – authentication and profile requirements (JWT‑based login, email/SMS verification, profile editing, viewing one’s own posts and review status, mutual follow rules for messaging, unread counts, soft‑delete of conversations). |
| **Use Cases (UC)** | UC‑15: Register Account with Email/Phone; UC‑16: Verify Code; UC‑17: Login / Logout; UC‑18: View / Edit Profile; UC‑19: View My Community Posts; UC‑20: Start Conversation; UC‑21: Send Message; UC‑22: View Conversation History; UC‑23: Mark Messages as Read. |
| **Activity / Sequence Diagrams (AD / SD)** | AD‑07 … AD‑10 – registration & verification flow, login flow, profile editing, messaging flow. Corresponding sequence diagrams in SDD: auth flow (send verification code, verify, register, login with JWT), profile update, create/get conversation, send message, list conversations/messages with unread counts. |
| **User Interface (UI)** | UI‑08: Login / Register page; UI‑09: Profile page; UI‑10: “My Community Posts” page; UI‑11: Conversations list; UI‑12: Chat window. |
| **Unit Tests (UTC)** | UTC‑4x … UTC‑5x – backend `AuthController`, `UserController`, `JwtService`, `VerificationCodeService`, `MessageController`, `ConversationRepository`, `MessageRepository`, plus related Vue components for login, profile and messaging. *(Fill in concrete IDs according to your actual UTC list.)* |
| **Integration Tests (ITC)** | ITC‑09: Auth flow (send verification code, verify, register, login); ITC‑10: Profile APIs and “My Community Posts”; ITC‑11: Messaging APIs (create/get conversation, send message, mark as read) including enforcement of mutual-follow rule. |
| **System Tests (STC)** | STC‑2x … – complete workflows: email/phone registration and login, logout, profile editing and language preference, viewing personal posts with review status, starting conversations with mutual followers, sending/receiving messages, enforcing “one message before mutual follow” limit, and marking messages as read. |

---

> **Note:**  
> - Replace the placeholder ranges (e.g., URS‑08 … URS‑21, UTC‑2x …) with the exact IDs from your final SRS, SDD, and Test Plan documents.  
> - When porting this Markdown into a Word document, you can use one large table per feature, with columns: Feature, URS, SRS, UC, AD, SD, UI, Unit Test, Integration Test, System Test – matching the layout shown in your template screenshots.


