import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentRepository {
    private static final String HEADER = "roll_number,name,class_name,course_marks";

    private final Path dataFile;
    private final Map<String, Student> students = new LinkedHashMap<>();

    public StudentRepository() throws IOException {
        this(Path.of("students.csv"));
    }

    StudentRepository(Path dataFile) throws IOException {
        this.dataFile = dataFile;
        load();
    }

    public Path getDataFile() {
        return dataFile;
    }

    public List<Student> getAllStudents() {
        return new ArrayList<>(students.values());
    }

    public Student findStudent(String rollNumber) {
        return students.get(normalizeRollNumber(rollNumber));
    }

    public void addStudent(Student student) throws IOException {
        String key = normalizeRollNumber(student.getRollNumber());
        if (students.containsKey(key)) {
            throw new IllegalArgumentException("A student with that roll number already exists.");
        }
        students.put(key, student);
        saveOrRestore(key, null);
    }

    public void updateStudent(String rollNumber, Student updatedStudent) throws IOException {
        String key = normalizeRollNumber(rollNumber);
        Student previous = students.get(key);
        if (previous == null) {
            throw new IllegalArgumentException("Student not found.");
        }
        if (!key.equals(normalizeRollNumber(updatedStudent.getRollNumber()))) {
            throw new IllegalArgumentException("A student's roll number cannot be changed.");
        }
        students.put(key, updatedStudent);
        saveOrRestore(key, previous);
    }

    public void deleteStudent(String rollNumber) throws IOException {
        String key = normalizeRollNumber(rollNumber);
        Student previous = students.remove(key);
        if (previous == null) {
            throw new IllegalArgumentException("Student not found.");
        }
        saveOrRestore(key, previous);
    }

    private void saveOrRestore(String key, Student previous) throws IOException {
        try {
            save();
        } catch (IOException exception) {
            if (previous == null) {
                students.remove(key);
            } else {
                students.put(key, previous);
            }
            throw exception;
        }
    }

    private void load() throws IOException {
        if (!Files.exists(dataFile)) {
            return;
        }
        List<String> lines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return;
        }
        if (!HEADER.equals(lines.get(0))) {
            throw new IOException("Unexpected CSV header in " + dataFile + ".");
        }

        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).isBlank()) {
                continue;
            }
            try {
                List<String> fields = parseCsvLine(lines.get(index));
                if (fields.size() != 4) {
                    throw new IllegalArgumentException("Expected four CSV fields.");
                }
                Student student = new Student(
                        fields.get(0),
                        fields.get(1),
                        fields.get(2),
                        decodeCourseMarks(fields.get(3)));
                String key = normalizeRollNumber(student.getRollNumber());
                if (students.putIfAbsent(key, student) != null) {
                    throw new IllegalArgumentException("Duplicate roll number.");
                }
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid student data on line " + (index + 1) + ": "
                        + exception.getMessage(), exception);
            }
        }
    }

    private void save() throws IOException {
        StringBuilder csv = new StringBuilder(HEADER).append(System.lineSeparator());
        for (Student student : students.values()) {
            csv.append(csvField(student.getRollNumber())).append(',')
                    .append(csvField(student.getName())).append(',')
                    .append(csvField(student.getClassName())).append(',')
                    .append(csvField(encodeCourseMarks(student.getCourseMarks())))
                    .append(System.lineSeparator());
        }

        Path absoluteFile = dataFile.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "students-", ".tmp");
        try {
            Files.writeString(temporaryFile, csv, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String normalizeRollNumber(String rollNumber) {
        return rollNumber.trim().toLowerCase(Locale.ROOT);
    }

    private static String encodeCourseMarks(Map<String, Double> courseMarks) {
        List<String> entries = new ArrayList<>();
        courseMarks.forEach((course, mark) -> entries.add(
                URLEncoder.encode(course, StandardCharsets.UTF_8) + "=" + mark));
        return String.join(";", entries);
    }

    private static Map<String, Double> decodeCourseMarks(String encoded) {
        Map<String, Double> courseMarks = new LinkedHashMap<>();
        if (encoded.isEmpty()) {
            return courseMarks;
        }
        for (String entry : encoded.split(";", -1)) {
            int separator = entry.lastIndexOf('=');
            if (separator < 1 || separator == entry.length() - 1) {
                throw new IllegalArgumentException("Invalid course mark entry.");
            }
            String course = URLDecoder.decode(entry.substring(0, separator), StandardCharsets.UTF_8);
            double mark;
            try {
                mark = Double.parseDouble(entry.substring(separator + 1));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid mark for course " + course + ".", exception);
            }
            if (course.isBlank() || !Double.isFinite(mark) || mark < 0 || mark > 100) {
                throw new IllegalArgumentException("Invalid course name or mark.");
            }
            if (courseMarks.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(course))) {
                throw new IllegalArgumentException("Duplicate course: " + course + ".");
            }
            courseMarks.put(course, mark);
        }
        return courseMarks;
    }

    private static String csvField(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (quoted) {
                if (character == '"' && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else if (character == '"') {
                    quoted = false;
                } else {
                    field.append(character);
                }
            } else if (character == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (character == '"' && field.length() == 0) {
                quoted = true;
            } else {
                field.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quote in CSV row.");
        }
        fields.add(field.toString());
        return fields;
    }
}
