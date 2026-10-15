import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Plain-Java test runner (no JUnit needed): run with  java -cp out LibraryTest */
public class LibraryTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        testValidation();
        testPolymorphism();
        testBorrowAndReturn();
        testItemNotAvailable();
        testMemberLimit();
        testUnknownIds();
        testOverdue();
        testSearch();
        testDuplicateIds();
        testPersistenceRoundTrip();
        testCorruptFile();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------ helpers
    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) passed++; else failed++;
    }

    private static <T extends Throwable> void expect(String name, Class<T> type, Runnable r) {
        try { r.run(); check(name, false); }
        catch (Throwable t) { check(name, type.isInstance(t)); }
    }

    private static Library sample() {
        Library lib = new Library();
        lib.addItem(new Book("B1", "Clean Code", "Robert Martin"));
        lib.addItem(new Book("B2", "Effective Java", "Joshua Bloch"));
        lib.addItem(new Book("B3", "Java Concurrency", "Brian Goetz"));
        lib.addItem(new Book("B4", "Head First Java", "Kathy Sierra"));
        lib.addItem(new Dvd("D1", "Java Documentary", 90));
        lib.registerMember(new Member("M1", "Tapiwa"));
        lib.registerMember(new Member("M2", "Rudo"));
        return lib;
    }

    // -------------------------------------------------------------- tests
    static void testValidation() {
        expect("Blank title rejected", IllegalArgumentException.class, () -> new Book("B9", " ", "A"));
        expect("Comma in title rejected", IllegalArgumentException.class, () -> new Book("B9", "A,B", "A"));
        expect("Zero DVD duration rejected", IllegalArgumentException.class, () -> new Dvd("D9", "X", 0));
        expect("Null member name rejected", IllegalArgumentException.class, () -> new Member("M9", null));
    }

    static void testPolymorphism() {
        LibraryItem book = new Book("B1", "T", "A");
        LibraryItem dvd = new Dvd("D1", "T", 100);
        check("Book loan period is 14 days", book.loanPeriodDays() == 14);
        check("DVD loan period is 7 days", dvd.loanPeriodDays() == 7);
        Member m = new Member("M1", "A");
        LocalDate d = LocalDate.of(2026, 1, 1);
        Loan lb = new Loan(m, book, d), ld = new Loan(m, dvd, d);
        check("Due date uses the item's own period",
                lb.getDueDate().equals(d.plusDays(14)) && ld.getDueDate().equals(d.plusDays(7)));
    }

    static void testBorrowAndReturn() throws Exception {
        Library lib = sample();
        lib.borrow("M1", "B1");
        LibraryItem b1 = lib.searchByTitle("Clean").get(0);
        Member m1 = lib.getMembers().stream().filter(m -> m.getId().equals("M1")).findFirst().get();
        check("Item unavailable after borrow", !b1.isAvailable());
        check("Member has 1 loan", m1.getLoanCount() == 1);
        lib.returnItem("B1");
        check("Item available after return", b1.isAvailable());
        check("Member has 0 loans after return", m1.getLoanCount() == 0);
        expect("Returning an item not on loan fails", IllegalStateException.class, () -> lib.returnItem("B1"));
    }

    static void testItemNotAvailable() throws Exception {
        Library lib = sample();
        lib.borrow("M1", "B1");
        try { lib.borrow("M2", "B1"); check("ItemNotAvailableException thrown", false); }
        catch (ItemNotAvailableException e) { check("ItemNotAvailableException thrown", true); }
    }

    static void testMemberLimit() throws Exception {
        Library lib = sample();
        lib.borrow("M1", "B1"); lib.borrow("M1", "B2"); lib.borrow("M1", "B3");
        try { lib.borrow("M1", "B4"); check("4th loan blocked (limit 3)", false); }
        catch (MemberLimitExceededException e) { check("4th loan blocked (limit 3)", true); }
        check("Blocked item stays available", lib.searchByTitle("Head First").get(0).isAvailable());
    }

    static void testUnknownIds() {
        Library lib = sample();
        expect("Unknown member rejected", IllegalArgumentException.class, () -> {
            try { lib.borrow("NOPE", "B1"); } catch (ItemNotAvailableException | MemberLimitExceededException e) { }
        });
        expect("Unknown item rejected", IllegalArgumentException.class, () -> {
            try { lib.borrow("M1", "NOPE"); } catch (ItemNotAvailableException | MemberLimitExceededException e) { }
        });
    }

    static void testOverdue() throws Exception {
        Library lib = sample();
        LocalDate today = LocalDate.now();
        lib.borrow("M1", "B1", today.minusDays(20));   // due 6 days ago  -> overdue
        lib.borrow("M1", "D1", today.minusDays(8));    // due 1 day ago   -> overdue
        lib.borrow("M2", "B2", today.minusDays(5));    // due in 9 days   -> fine
        List<Loan> overdue = lib.listOverdue(today);
        check("Two overdue loans found", overdue.size() == 2);
        check("Most overdue first", overdue.get(0).getItem().getId().equals("B1"));
        check("Days overdue computed", overdue.get(0).daysOverdue(today) == 6);
    }

    static void testSearch() {
        Library lib = sample();
        List<LibraryItem> found = lib.searchByTitle("JAVA");
        check("Search is case-insensitive (4 matches)", found.size() == 4);
        check("Results sorted by title", found.get(0).getTitle().equals("Effective Java")
                && found.get(3).getTitle().equals("Java Documentary"));
        check("No match gives empty list", lib.searchByTitle("zzz").isEmpty());
    }

    static void testDuplicateIds() {
        Library lib = sample();
        expect("Duplicate item id rejected", IllegalArgumentException.class,
                () -> lib.addItem(new Book("B1", "Other", "X")));
        expect("Duplicate member id rejected", IllegalArgumentException.class,
                () -> lib.registerMember(new Member("M1", "Other")));
    }

    static void testPersistenceRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("library-test");
        Library lib = sample();
        LocalDate borrowed = LocalDate.now().minusDays(10);
        lib.borrow("M1", "B1", borrowed);
        lib.save(dir);

        Library reloaded = new Library();
        reloaded.load(dir);
        check("Items reloaded", reloaded.getItems().size() == 5);
        check("Members reloaded", reloaded.getMembers().size() == 2);
        LibraryItem b1 = reloaded.searchByTitle("Clean Code").get(0);
        check("Loan state reloaded", !b1.isAvailable());
        check("Loan date reloaded", b1.getCurrentLoan().getBorrowDate().equals(borrowed));
        check("Item subtype reloaded (Dvd)", reloaded.searchByTitle("Documentary").get(0) instanceof Dvd);
        check("Member's loan count reloaded", reloaded.getMembers().stream()
                .filter(m -> m.getId().equals("M1")).findFirst().get().getLoanCount() == 1);
    }

    static void testCorruptFile() throws Exception {
        Path dir = Files.createTempDirectory("library-bad");
        Files.writeString(dir.resolve("items.csv"), "BOOK,B1,OnlyThreeFields\n");
        try { new Library().load(dir); check("Corrupt CSV reported as IOException", false); }
        catch (java.io.IOException e) { check("Corrupt CSV reported as IOException", true); }
        Library empty = new Library();
        try { empty.load(Files.createTempDirectory("library-empty")); check("Missing files load as empty library", empty.getItems().isEmpty()); }
        catch (Exception e) { check("Missing files load as empty library", false); }
    }
}
