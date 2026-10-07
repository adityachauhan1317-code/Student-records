import java.util.LinkedHashMap;
import java.util.Map;

public class Student {
    private final String rollNumber;
    private final String name;
    private final String className;
    private final Map<String, Double> courseMarks;

    public Student(String rollNumber, String name, String className, Map<String, Double> courseMarks) {
        this.rollNumber = requireText(rollNumber, "Roll number");
        this.name = requireText(name, "Name");
        this.className = requireText(className, "Class / year");
        if (courseMarks == null) {
            throw new IllegalArgumentException("Course marks cannot be null.");
        }
        this.courseMarks = new LinkedHashMap<>();
        courseMarks.forEach((course, mark) -> {
            String courseName = requireText(course, "Course name");
            if (mark == null || !Double.isFinite(mark) || mark < 0 || mark > 100) {
                throw new IllegalArgumentException("Marks must be between 0 and 100.");
            }
            if (this.courseMarks.keySet().stream()
                    .anyMatch(existing -> existing.equalsIgnoreCase(courseName))) {
                throw new IllegalArgumentException("Course names must be unique.");
            }
            this.courseMarks.put(courseName, mark);
        });
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Double> getCourseMarks() {
        return new LinkedHashMap<>(courseMarks);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }
}