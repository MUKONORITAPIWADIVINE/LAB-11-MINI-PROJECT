import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/** Console menu for the Library Management System. */
public class LibraryApp {
    private static final Path DATA_DIR = Path.of("data");

    private final Library library = new Library();
    private final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        new LibraryApp().run();
    }

    private void run() {
        try {
            library.load(DATA_DIR);
            System.out.println("Loaded " + library.getItems().size() + " items and "
                    + library.getMembers().size() + " members from '" + DATA_DIR + "'.");
        } catch (IOException e) {
            System.out.println("Could not load saved data: " + e.getMessage());
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = ask("Choose an option: ");
            if (choice == null) choice = "0";            // input closed -> save and exit
            try {
                switch (choice.trim()) {
                    case "1" -> addItem();
                    case "2" -> registerMember();
                    case "3" -> borrow();
                    case "4" -> giveBack();
                    case "5" -> listOverdue();
                    case "6" -> search();
                    case "7" -> listAll();
                    case "0" -> running = false;
                    default -> System.out.println("Please choose 0-7.");
                }
            } catch (ItemNotAvailableException | MemberLimitExceededException e) {
                System.out.println("Cannot complete: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        try {
            library.save(DATA_DIR);
            System.out.println("Data saved. Goodbye!");
        } catch (IOException e) {
            System.out.println("WARNING - could not save data: " + e.getMessage());
        }
    }

    private void printMenu() {
        System.out.println("""

                ===== Library Management System =====
                1. Add item (book or DVD)
                2. Register member
                3. Borrow an item
                4. Return an item
                5. List overdue items
                6. Search by title
                7. List all items and members
                0. Save and exit""");
    }

    private void addItem() {
        String type = ask("Type (BOOK/DVD): ");
        String id = ask("Item id: ");
        String title = ask("Title: ");
        String extra = ask(type != null && type.equalsIgnoreCase("DVD")
                ? "Duration in minutes: " : "Author: ");
        library.addItem(ItemFactory.create(type, id, title, extra));
        System.out.println("Item added.");
    }

    private void registerMember() {
        library.registerMember(new Member(ask("Member id: "), ask("Name: ")));
        System.out.println("Member registered.");
    }

    private void borrow() throws ItemNotAvailableException, MemberLimitExceededException {
        Loan loan = library.borrow(ask("Member id: "), ask("Item id: "));
        System.out.println("Borrowed: " + loan);
    }

    private void giveBack() {
        library.returnItem(ask("Item id: "));
        System.out.println("Item returned.");
    }

    private void listOverdue() {
        LocalDate today = LocalDate.now();
        List<Loan> overdue = library.listOverdue(today);
        if (overdue.isEmpty()) {
            System.out.println("No overdue items.");
            return;
        }
        overdue.forEach(l -> System.out.println(l + " - " + l.daysOverdue(today) + " day(s) overdue"));
    }

    private void search() {
        List<LibraryItem> found = library.searchByTitle(ask("Title contains: "));
        if (found.isEmpty()) System.out.println("No matches.");
        else found.forEach(System.out::println);
    }

    private void listAll() {
        System.out.println("-- Items --");
        library.getItems().stream()
                .sorted(java.util.Comparator.comparing(LibraryItem::getId))
                .forEach(System.out::println);
        System.out.println("-- Members --");
        library.getMembers().stream()
                .sorted(java.util.Comparator.comparing(Member::getId))
                .forEach(System.out::println);
    }

    /** Prompts and reads a line; returns null when the input has ended. */
    private String ask(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : null;
    }
}
