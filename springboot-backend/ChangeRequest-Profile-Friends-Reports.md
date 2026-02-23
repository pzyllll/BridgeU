### Change Request

#### Document History

| Version Number | Record Date     | Prepared/Modified By | Reviewed By              | Change Details |
|----------------|-----------------|----------------------|--------------------------|----------------|
| 1.0            | 19/February/2026 | pzy                  | (TBD)                    | - Add feature: View mutual follows (Friends) and following list in Profile.<br>- Add feature: View My Reports in Profile, reusing existing reporting and moderation backend. |

---

### Objective

Provide a formal record of the changes made to the **Profile** module of the BridgeU platform, adding:

- **Mutual Follows (Friends) view** – allow users to see users who mutually follow each other.
- **Following-only list** – allow users to see users that they follow (one-way follow).
- **View My Reports** – allow users to view all reports they have submitted, including status and moderation details.

These changes extend **Feature #3: Authentication and Profile System** in `BridgeU-SRS.md` (UC-24 View Followers/Friends List, UC-17 View and Manage the mutual follow list, UC-25 View My Reports) and are fully implemented in the backend and frontend.

---

### Project Information

| Name                                                          | Phase | Description |
|---------------------------------------------------------------|-------|-------------|
| BridgeU – Authentication and Profile System Enhancements      | 2     | Extend the Profile page to support a **Friends (mutual follows)** view, a **Following** list, and a **My Reports** view. Update the Software Requirements Specification (SRS), Use Case Diagram, Activity Diagram, UI Design, and System Testing accordingly. |

---

### Change Request Summary

| No | Requested Date    | Requested By | Description of Change | Status | Remark |
|----|-------------------|--------------|------------------------|--------|--------|
| 1  | 19/February/2026  | pzy          | - Add Profile entry to view **Mutual Follows (Friends)** and **Following** lists, backed by user follow relationships.<br>- Add Profile entry to **View My Reports**, listing all reports submitted by the current user with status and AI moderation results.<br>- Update SRS (Feature #3), URS, use cases UC-24, UC-17, UC-25, and related diagrams and test cases. | Done   |        |

---

### Change Request Form (Modification/Maintenance Record Report)

**Submitting Organization:** BridgeU Project  
**Contact Person:** pzy  
**Product/Project Name:** BridgeU – AI-assisted Community & Daily Briefing Platform  
**Subsystem:** Authentication and Profile System (Feature #3)  
**Type of Change:** ✓ Add  
**Tracking No.:** 01-2026-PROFILE-FRIENDS-REPORTS  
**Date:** 19/February/2026

#### 1. Specify Change

**Proposed Change**

1. **Add Friends (Mutual Follows) view in Profile**
   - On the user Profile page, add an entry/tab **Friends** that shows users who have a **mutual follow** relationship with the current user.  
   - The system identifies mutual follow relationships between users and presents a consolidated “friends list” with basic user information (such as username, display name, avatar or email).  
   - Support searching and filtering within this friends list (for example, by username or display name) to help users quickly locate specific friends.

2. **Add Following list (one-way follow) in Profile**
   - On the user Profile page, add an entry/tab **Following** (or **My Following**) to show users that the current user follows, regardless of whether they follow back.  
   - The system provides a clear list of all such followed users together with their basic profile information, enabling users to review and understand their following relationships.  
   - From this list, users can manage their follow relationships (for example, unfollow or adjust interaction preferences) in a centralized manner.

3. **Add “View My Reports” in Profile**
   - On the user Profile page, add an entry **My Reports** which navigates to a list of all reports submitted by the current user.  
   - The list shows key information for each report, including the reported target, selected reasons, description, current processing status, submission time, review time, and (if applicable) moderation result and notes.  
   - This view gives users a transparent overview of how their reports are being handled and the final decisions made by the moderation process.

4. **Update SRS and Related Artifacts**
   - Update `BridgeU-SRS.md` to reflect that Feature #3 includes viewing followers/friends lists and viewing submitted reports.  
   - If needed, add or refine activity diagrams and UI mockups for the Profile page to include Friends, Following and My Reports entries.  
   - Update unit and system test cases to cover: loading mutual follows list, following-only list, and viewing own reports, including error cases (unauthorized, server error).

**Reason for Change**

1. Allow users to clearly distinguish between **Friends (mutual follows)** and **one-way following relationships**, improving social interaction clarity and supporting private messaging rules based on mutual follow .  
2. Provide transparency for community moderation by enabling users to **view all reports they have submitted** and the processing status and moderation decision for each report, increasing trust in the platform’s reporting and AI moderation system.  
3. Align the implemented backend endpoints with the formal documentation in the SRS and ensure traceability between requirements, design, implementation, and testing.

---

### 2. Approve Change (for Maintenance Persons)

- **Approve Change:** ( ✓ ) Yes   (  ) No  
- **Authorized by:** (TBD)  
- **Authorized Date:** 19/February/2026

---

### 3. Execute Change

- **Status:** Done  
- **Solution:**  
  - Implemented mutual follows computation and listing in `UserController` using `UserFollowRepository` to build mutual follow lists and follower/following counts.  
  - Implemented user reports listing in `ReportController.getMyReports` using `ReportRepository.findByReporterIdOrderByCreatedAtDesc` and DTO mapping.  
  - Integrated new Profile entries/tabs (Friends, Following, My Reports) in the frontend, consuming the above endpoints.  
  - Updated `BridgeU-SRS.md` Feature #3.
- **Tested (Test script/status):** Pass

**Executed by:** pzy  
**Executed Date:** 19/February/2026

---

### 4. Accept Change (Clients or Users)

- **Accepted by:** (TBD)  
- **Accepted Date:** (TBD)


