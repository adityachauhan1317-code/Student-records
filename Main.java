import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Scanner INPUT = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            StudentRepository repository = new StudentRepository();
            run(repository);
        } catch (IOException exception) {
            System.err.println("Could not open student data: " + exception.getMessage());
        }
    }

    private static void run(StudentRepository repository) {
        System.out.println("Student Records");
        System.out.println("Data file: " + repository.getDataFile().toAbsolutePath());

        while (true) {
            System.out.println("\n1. List students");
            System.out.println("2. Add student");
            System.out.println("3. Find student");
            System.out.println("4. Update student");
            System.out.println("5. Delete student");
            System.out.println("0. Exit");

            String choice = readText("Choose an option: ");
            try {
                switch (choice) {
                    case "1":
                        listStudents(repository);
                        break;
                    case "2":
                        addStudent(repository);
                        break;
                    case "3":
                        findStudent(repository);
                        break;
                    case "4":
                        updateStudent(repository);
                        break;
                    case "5":
                        deleteStudent(repository);
                        break;
                    case "0":
                        System.out.println("Goodbye.");
                        return;
                    default:
                        System.out.println("Please choose one of the listed options.");
                }
            } catch (IOException | IllegalArgumentException exception) {
                System.out.println("Could not complete that action: " + exception.getMessage());
            }
        }
    }

    private static void listStudents(StudentRepository repository) {
        List<Student> students = repository.getAllStudents();
        if (students.isEmpty()) {
            System.out.println("No student records yet.");
            return;
        }
        students.forEach(Main::printStudent);
    }

    private static void addStudent(StudentRepository repository) throws IOException {
        String rollNumber = readRequiredText("Roll number: ");
        String name = readRequiredText("Student name: ");
        String className = readRequiredText("Class / year: ");
        Map<String, Double> courses = readCourses();

        repository.addStudent(new Student(rollNumber, name, className, courses));
        System.out.println("Student added.");
    }

    private static void findStudent(StudentRepository repository) {
        String rollNumber = readRequiredText("Roll number: ");
        Student student = repository.findStudent(rollNumber);
        if (student == null) {
            System.out.println("No student found with that roll number.");
        } else {
            printStudent(student);
        }
    }

    private static void updateStudent(StudentRepository repository) throws IOException {
        String rollNumber = readRequiredText("Roll number of student to update: ");
        Student existing = repository.findStudent(rollNumber);
        if (existing == null) {
            System.out.println("No student found with that roll number.");
            return;
        }

        System.out.println("Leave a field blank to keep its current value.");
        String name = readText("Name [" + existing.getName() + "]: ");
        String className = readText("Class / year [" + existing.getClassName() + "]: ");
        String updateCourses = readText("Replace course marks? (y/n): ");

        Map<String, Double> courses = existing.getCourseMarks();
        if (updateCourses.equalsIgnoreCase("y")) {
            courses = readCourses();
        } else if (!updateCourses.isBlank() && !updateCourses.equalsIgnoreCase("n")) {
            System.out.println("Please enter y or n.");
            return;
        }

        Student updated = new Student(
                existing.getRollNumber(),
                name.isBlank() ? existing.getName() : name,
                className.isBlank() ? existing.getClassName() : className,
                courses);
        repository.updateStudent(rollNumber, updated);
        System.out.println("Student updated.");
    }

    private static void deleteStudent(StudentRepository repository) throws IOException {
        String rollNumber = readRequiredText("Roll number of student to delete: ");
        Student student = repository.findStudent(rollNumber);
        if (student == null) {
            System.out.println("No student found with that roll number.");
            return;
        }
        String confirmation = readText("Delete " + student.getName() + "? (y/n): ");
        if (confirmation.equalsIgnoreCase("y")) {
            repository.deleteStudent(rollNumber);
            System.out.println("Student deleted.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private static Map<String, Double> readCourses() {
        Map<String, Double> courses = new LinkedHashMap<>();
        System.out.println("Enter course names and marks (0-100). Leave the course name blank when done.");
        while (true) {
            String course = readText("Course name: ");
            if (course.isBlank()) {
                return courses;
            }
            if (courses.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(course))) {
                System.out.println("That course has already been entered.");
                continue;
            }
            courses.put(course, readMark());
        }
    }

    private static double readMark() {
        while (true) {
            String value = readRequiredText("Mark (0-100): ");
            try {
                double mark = Double.parseDouble(value);
                if (Double.isFinite(mark) && mark >= 0 && mark <= 100) {
                    return mark;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Enter a number from 0 to 100.");
        }
    }

    private static void printStudent(Student student) {
        System.out.println("\nRoll number: " + student.getRollNumber());
        System.out.println("Name: " + student.getName());
        System.out.println("Class / year: " + student.getClassName());
        System.out.println("Course marks:");
        if (student.getCourseMarks().isEmpty()) {
            System.out.println("  No courses recorded.");
        } else {
            student.getCourseMarks().forEach(
                    (course, mark) -> System.out.println("  " + course + ": " + mark));
        }
    }

    private static String readRequiredText(String prompt) {
        while (true) {
            String value = readText(prompt);
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("This field cannot be blank.");
        }
    }

    private static String readText(String prompt) {
        System.out.print(prompt);
        return INPUT.nextLine().trim();
    }
}
