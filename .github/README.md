# 🤖 AI & Copilot Guidance

This directory contains guidance documents for AI agents and code assistants (GitHub Copilot, Claude, Cursor, etc.) contributing to the **Fuel Queue Backend** project.

## 📚 Files

### **AGENTS.md** — Comprehensive Architecture Guide
The primary document for AI agents. Contains:
- Core purpose and system overview
- Architecture breakdown (CrowdService, CrowdStore, repositories, security)
- Build & test workflows with exact commands
- Project conventions (dependency injection, configuration patterns)
- Critical business rules (Haversine distance, TTL logic, crowd thresholds)
- Common editing patterns for new features
- Production upgrade considerations

**Use this file first** when contributing to this project.

---

## 🎯 Quick Reference for AI Agents

### **Start Here**
1. Read `AGENTS.md` for architecture overview
2. Check the "Common Editing Patterns" section for your task type
3. Refer to project file organization for file locations

### **Key Goals for AI Contributions**
- ✅ Always sync Haversine implementations (Java + SQL)
- ✅ Use constructor injection only (no `@Autowired` fields)
- ✅ Keep test `@Order` sequence intact
- ✅ Update both `application.properties` and `application-test.properties` for config changes
- ✅ Respect soft-delete pattern (filter `active = true`)

### **Critical Files to Know**
| File | Purpose |
|------|---------|
| `src/main/java/com/fuelqueue/service/CrowdService.java` | Core GPS/crowd logic |
| `src/main/java/com/fuelqueue/service/CrowdStore.java` | In-memory presence tracking |
| `src/main/resources/application.properties` | Configuration thresholds |
| `src/test/java/com/fuelqueue/FuelQueueIntegrationTest.java` | All 15 integration tests |

### **Common Tasks**
- **Add a new endpoint?** → See "Add a new API endpoint" in AGENTS.md
- **Change crowd thresholds?** → Modify `application.properties` (see config pattern)
- **Modify distance calculation?** → Update both `CrowdService` and `FuelStationRepository`
- **Run tests?** → `mvn test` or right-click `FuelQueueIntegrationTest.java` in IntelliJ
- **Start the app?** → `mvn spring-boot:run` or click ▶ on `FuelQueueApp.java`

---

## 📖 Project Context

**Tech Stack**: Spring Boot 3.2, Java 17, H2 in-memory DB, JWT authentication

**Purpose**: Real-time GPS geofencing system to track fuel station crowd levels and wait times

**API Pattern**: Stateless REST with JWT tokens (no sessions)

**Architecture Pattern**: Service-based with DTOs, custom JPA queries for geo-calculations

For full details, see `AGENTS.md`.


