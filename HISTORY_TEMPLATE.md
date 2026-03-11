# Project History & Milestones 📜

> [!IMPORTANT]
> **🤖 AI HANDOFF NOTES & CURRENT STATE**
> *AI INSTRUCTION: Upon first residency in this project, immediately replace the placeholders below to establish the "Source of Truth" for all subsequent models.*

> [!IMPORTANT]
> **INITIALIZATION PROTOCOL**: 
> 1. If `history/` directory does not exist: Create it immediately.
> 2. If `history/history.md` does not exist: Create it inside the `history/` folder using this template.
> 3. If `history/history.md` exists: Ensure it follows this "Elite" structure. Refactor if necessary while preserving history.

> [!CAUTION]
> **ABSOLUTE REQUIREMENT**: You MUST update the `history/history.md` file (based on this template) at the end of every significant task or at the conclusion of your session. Failure to maintain this log breaks the project's historical chain and is unacceptable.

**Project Persona**: [Short description, e.g., "Professional Desktop Data Recovery Suite"]
**Tech Stack**: [e.g., Tauri v2, React 19, Rust, Tailwind CSS]
**Design Language**: [e.g., "Deep Midnight" #08050a base, crisp 1px borders, no diffused shadows]
**Repository**: [AI: Populate with result of `git remote get-url origin`]

### 🛡️ Safety & Push Policy
- **Primary Remote**: [AI: Ensure this matches the Repository URL above before any `git push`]
- **Ignore Files**: Ensure the `history/` directory is in `.gitignore` to maintain local privacy for all logs and archives.
- **Secret Safety**: Never commit `.env` files, API keys, or hardcoded credentials.

### ✅ Verification Protocol
Before marking a task as complete, the AI must verify stability:
- **Linting**: [e.g., `npm run lint` or `cargo clippy`]
- **Build**: [e.g., `npm run build` or `cargo build`]
- **Visuals**: [e.g., Provide a screenshot/recording for UI changes]

### 📚 External Intelligence
- **Official Docs**: [AI: Populate with relevant docs, e.g., https://tauri.app/v2/]
- **Design Spec**: [e.g., Figma or Brand Guide URL]

- [ ] **Next Up**: [Immediate next priority]

### 🏗️ Phase 0: Implementation Strategy
> [!NOTE]
> If this is an **Existing Project**, you may skip these structural mandates to maintain backwards compatibility with the established architectural patterns. 

**1. Modular Initialization (For New Starts)**:
- **Strict Separation**: Maintain a clear boundary between Backend (e.g., Rust/Node commands) and Frontend (e.g., React/Vue components).
- **Interface-First Design**: Define backend command signatures and payload types *before* building UI listeners.
- **Clean Structure**: Standardize on a `/src-backend` and `/src-frontend` type split if possible, or use clear directory names like `/commands` and `/components`.

**2. Surgical Implementation (Chunking)**:
- **Build in Atoms**: Never implement an entire complex feature in one massive block. Break work into logical "atoms" (e.g., 1. Backend Logic -> 2. State Binding -> 3. UI Display).
- **Limit Management**: To avoid hitting AI context/token limits, use `multi_replace_file_content` for non-contiguous edits and keep replacement chunks precise.
- **Large Files**: Always use `view_file_outline` before reading any file exceeding 500 lines to preserve token economy.
- **Continuous Validation**: Build and verify after every 2-3 significant "atoms" to prevent bug nesting.

### 📍 Current Architecture & Flow
1. **[Entry Point]**: [e.g., App.tsx handles state and navigation]
2. **[Core Logic]**: [e.g., Rust backend executes CLI commands via std::process]
3. **[Data Flow]**: [e.g., Tauri Events stream raw logs to React terminal]

### 🚧 What's In Progress
- [ ] **Active Task**: [Current focus from task.md]
- [ ] **Next Up**: [Immediate next priority]

### 📉 Technical Debt & Gotchas
- **[Constraint]**: [e.g., "Windows-only due to NTFS dependency"]
- **[Known Issue]**: [e.g., "Flickering in WebView2 — use translate-z-0"]

---

### � Change Log (Git-Style)
- **feat(component)**: [timestamp] Implementation details...
- **fix(component)**: [timestamp] Bug description and fix...
- **refactor(component)**: [timestamp] Code structural changes...
- **docs**: [timestamp] Update documentation or logs...
- **chore**: [timestamp] Maintenance or setup changes...

### 📅 Future Roadmap
- [ ] **v[Version]**: [Major Feature Group]
- [ ] **Optimization**: [Refactoring or Performance goal]

---

> 📂 **AI Instruction**: Maintain this log chronologically (newest at the top). 
> 
> **Pruning & Maintenance**:
> 1. **Token Economy**: If this file exceeds ~500 lines, prune entries older than 30 days by moving them to a `history_archive.md` to maintain a permanent record.
> 2. **Context Preservation**: Always keep the "AI Handoff Notes" and the last 5 major milestones at the top.
> 3. **Read Limit**: Standardize on reading the top 100-200 lines. This can be increased if the task requires deep research into historical implementation patterns.
