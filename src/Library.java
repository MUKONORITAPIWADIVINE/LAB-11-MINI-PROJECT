import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Core service: owns the items and members and enforces the library rules. */
public class Library {
    private static final String ITEMS_FILE = "items.csv";
    private static final String MEMBERS_FILE = "members.csv";
    private static final String LOANS_FILE = "loans.csv";

    private final Map<String, LibraryItem> items = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();

    // ---------------------------------------------------------------- adding
    public void addItem(LibraryItem item) {
        if (items.containsKey(item.getId())) {
            throw new IllegalArgumentException("An item with id " + item.getId() + " already exists.");
        }
        items.put(item.getId(), item);
    }

    public void registerMember(Member member) {
        if (members.containsKey(member.getId())) {
            throw new IllegalArgumentException("A member with id " + member.getId() + " already exists.");
        }
        members.put(member.getId(), member);
    }

    // ------------------------------------------------------------ borrowing
    public Loan borrow(String memberId, String itemId)
            throws ItemNotAvailableException, MemberLimitExceededException {
        return borrow(memberId, itemId, LocalDate.now());
    }

    /** Date can be supplied so tests (and CSV loading) can create older loans. */
    Loan borrow(String memberId, String itemId, LocalDate date)
            throws ItemNotAvailableException, MemberLimitExceededException {
        Member member = findMember(memberId);
        LibraryItem item = findItem(itemId);
        return item.borrow(member, date);
    }

    public void returnItem(String itemId) {
        findItem(itemId).giveBack();
    }

    // -------------------------------------------------------------- queries
    /** Streams: filter by title text, sort alphabetically. */
    public List<LibraryItem> searchByTitle(String text) {
        String needle = text.toLowerCase();
        return items.values().stream()
                .filter(i -> i.getTitle().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(LibraryItem::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** All loans past their due date, most overdue first. */
    public List<Loan> listOverdue(LocalDate today) {
        return items.values().stream()
                .map(LibraryItem::getCurrentLoan)
                .filter(loan -> loan != null && loan.isOverdue(today))
                .sorted(Comparator.comparing(Loan::getDueDate))
                .toList();
    }

    public Collection<LibraryItem> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public Collection<Member> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    private Member findMember(String id) {
        Member m = members.get(id);
        if (m == null) throw new IllegalArgumentException("No member with id " + id);
        return m;
    }

    private LibraryItem findItem(String id) {
        LibraryItem i = items.get(id);
        if (i == null) throw new IllegalArgumentException("No item with id " + id);
        return i;
    }

    // ---------------------------------------------------------- persistence
    /** Writes three CSV files into the folder (try/catch is left to the caller). */
    public void save(Path dir) throws IOException {
        Files.createDirectories(dir);

        List<String> itemLines = new ArrayList<>();
        List<String> loanLines = new ArrayList<>();
        items.values().stream()
                .sorted(Comparator.comparing(LibraryItem::getId))
                .forEach(i -> {
                    itemLines.add(String.join(",", i.getType(), i.getId(), i.getTitle(), i.getExtra()));
                    if (i.getCurrentLoan() != null) {
                        Loan l = i.getCurrentLoan();
                        loanLines.add(String.join(",", l.getMember().getId(), i.getId(),
                                l.getBorrowDate().toString()));
                    }
                });
        List<String> memberLines = members.values().stream()
                .sorted(Comparator.comparing(Member::getId))
                .map(m -> m.getId() + "," + m.getName())
                .toList();

        Files.write(dir.resolve(ITEMS_FILE), itemLines);
        Files.write(dir.resolve(MEMBERS_FILE), memberLines);
        Files.write(dir.resolve(LOANS_FILE), loanLines);
    }

    /** Reloads everything saved by save(). Missing files simply mean "nothing saved yet". */
    public void load(Path dir) throws IOException {
        items.clear();
        members.clear();
        try {
            for (String[] f : readCsv(dir.resolve(ITEMS_FILE), 4)) {
                addItem(ItemFactory.create(f[0], f[1], f[2], f[3]));
            }
            for (String[] f : readCsv(dir.resolve(MEMBERS_FILE), 2)) {
                registerMember(new Member(f[0], f[1]));
            }
            for (String[] f : readCsv(dir.resolve(LOANS_FILE), 3)) {
                borrow(f[0], f[1], LocalDate.parse(f[2]));
            }
        } catch (IllegalArgumentException | DateTimeParseException
                 | ItemNotAvailableException | MemberLimitExceededException e) {
            throw new IOException("Saved data is corrupt: " + e.getMessage(), e);
        }
    }

    private static List<String[]> readCsv(Path file, int fields) throws IOException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(file)) return rows;
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank()) continue;
            String[] parts = line.split(",", -1);
            if (parts.length != fields) {
                throw new IOException(file.getFileName() + ": expected " + fields
                        + " fields but found " + parts.length + " in '" + line + "'");
            }
            rows.add(parts);
        }
        return rows;
    }
}
