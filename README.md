# Mini-Project: Library Management System (Lab 11)

A console application that lends books and DVDs to members, enforces a
3-loan limit, reports overdue items, searches by title and saves everything
to CSV files between runs.

## Run it (Windows cmd, from this folder)

```bat
run.bat      :: compile + start the menu
test.bat     :: compile + run the 33 automated tests
```

Or by hand:

```bat
mkdir out
javac -d out src\*.java
java -cp out LibraryApp
java -cp out LibraryTest
```

Data is stored in the `data` folder (`items.csv`, `members.csv`, `loans.csv`).
The sample data includes one overdue loan so option 5 has something to show.
Delete the three files for a fresh, empty library.

## Structure

| File | Role |
|---|---|
| `Borrowable` | Interface: `borrow(Member)`, `giveBack()` |
| `LibraryItem` | Abstract parent: id, title, availability, abstract `loanPeriodDays()` |
| `Book`, `Dvd` | Subclasses (14-day and 7-day loans) |
| `Member`, `Loan` | Members (max 3 loans) and loan records (due date, overdue check) |
| `ItemNotAvailableException`, `MemberLimitExceededException` | Custom checked exceptions |
| `Library` | Core service: `Map` collections, streams search, overdue list, CSV save/load |
| `ItemFactory` | Factory pattern: builds a Book or Dvd from a type name |
| `LibraryApp` | Menu |
| `LibraryTest` | Plain-Java tests (no JUnit needed) |

See `docs/class-diagram.png` for the UML class diagram.
