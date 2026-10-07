# College Student Records

A simple Java console application for managing college student records. Each
student has a unique roll number, name, class or year, and a mark for each
course.

## Requirements

- Java 11 or later

## Run

Open a terminal in this folder and run:

```sh
javac Main.java Student.java StudentRepository.java
java Main
```

## Features

- List all student records
- Add a student and enter marks for multiple courses
- Find a student by roll number
- Update a student's name, class/year, or course marks
- Delete a student after confirmation
- Keep data between runs in `students.csv`

Marks must be between 0 and 100. Roll numbers are unique, ignoring letter case.
The CSV file is created automatically in the folder where you run the program.
Keep it private if it contains real student information, and do not commit it
to GitHub.
