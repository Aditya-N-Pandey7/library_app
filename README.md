# Oracle JDBC Library Book Manager

A console-based Java JDBC application (`com.exam.App`) for adding, viewing, searching, updating, issuing, returning, and deleting library books.

## Requirements

- **Java 21**
- **Oracle Database** (optional — automatically falls back to an embedded H2 database in Oracle compatibility mode if no local Oracle instance is running)

## Database Schema

On first run, `createTableIfNotExists` automatically creates the `library_books` table:

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `book_id` | `NUMBER` | `PRIMARY KEY` | Unique identifier for each book |
| `title` | `VARCHAR2(100)` | `NOT NULL` | Title of the book |
| `author` | `VARCHAR2(100)` | `NOT NULL` | Author of the book |
| `price` | `NUMBER(10, 2)` | — | Price of the book |
| `quantity` | `NUMBER` | `DEFAULT 1` | Number of available copies in the library |

## Configuration

`com.exam.App` reads connection settings from environment variables first, falling back to default constants (`jdbc:oracle:thin:@localhost:1521:xe`, `system`, `tiger`) if unset, and finally to the local embedded H2 database (`jdbc:h2:./library_db;MODE=Oracle`) if Oracle Server is not reachable:

- `ORACLE_JDBC_URL` (e.g., `jdbc:oracle:thin:@localhost:1521:xe`)
- `ORACLE_DB_USER` (e.g., `system`)
- `ORACLE_DB_PASSWORD` (e.g., `tiger`)

## How to Run

### Option 1: Using the Run Scripts (Recommended)

- **Git Bash / Linux:**
  ```bash
  ./run.sh
  ```

- **Windows CMD / PowerShell:**
  ```powershell
  .\run.bat
  ```

### Option 2: Manual Compilation & Execution

**PowerShell:**
```powershell
javac --release 21 -d target/classes src/main/java/com/exam/App.java
java -cp "target/classes;$HOME\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.2.0.0\ojdbc11-23.2.0.0.jar;$HOME\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" com.exam.App
```
